package com.jomariabejo.connectly_api.supplier_api.service;

import com.jomariabejo.connectly_api.supplier_api.dto.*;
import com.jomariabejo.connectly_api.supplier_api.entity.*;
import com.jomariabejo.connectly_api.supplier_api.repository.*;
import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import com.jomariabejo.connectly_api.tenant_api.repository.TenantRepository;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import com.jomariabejo.connectly_api.tenant_api.service.AuditLoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GRNService {

    private final GoodsReceiptNoteRepository grnRepository;
    private final GRNItemRepository grnItemRepository;
    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderItemRepository poItemRepository;
    private final SupplierRepository supplierRepository;
    private final TenantContextService tenantContextService;
    private final AuditLoggingService auditService;
    private final TenantRepository tenantRepository;

    /**
     * Create goods receipt note
     */
    @Transactional
    public GoodsReceiptNoteDto createGRN(CreateGRNRequest request, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();

        // Get purchase order
        PurchaseOrder po = poRepository.findById(request.getPurchaseOrderId())
                .orElseThrow(() -> new IllegalStateException("Purchase order not found"));

        if (!po.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Purchase order does not belong to this tenant");
        }

        if (!"APPROVED".equals(po.getStatus())) {
            throw new IllegalStateException("Can only receive approved purchase orders");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));

        // Generate GRN number
        String grnNumber = generateGrnNumber(tenantId);

        GoodsReceiptNote grn = GoodsReceiptNote.builder()
                .tenant(tenant)
                .grnNumber(grnNumber)
                .purchaseOrder(po)
                .supplier(po.getSupplier())
                .receiptDate(LocalDateTime.now())
                .status("RECEIVED")
                .totalQuantityReceived(BigDecimal.ZERO)
                .receivedBy(userId)
                .notes(request.getNotes())
                .build();

        grn = grnRepository.save(grn);

        // Audit log
        auditService.logEvent(tenantId, userId, "CREATE", "GoodsReceiptNote", grn.getId(), null,
                Map.of("grnNumber", grnNumber, "poId", request.getPurchaseOrderId()),
                null, null);

        log.info("Created GRN: {} for PO: {}", grn.getId(), request.getPurchaseOrderId());

        return mapToDto(grn);
    }

    /**
     * Add received item to GRN
     */
    @Transactional
    public GRNItemDto addReceivedItem(Long grnId, CreateGRNItemRequest request, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();

        GoodsReceiptNote grn = grnRepository.findById(grnId)
                .orElseThrow(() -> new IllegalStateException("GRN not found"));

        if (!grn.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("GRN does not belong to this tenant");
        }

        if (!"RECEIVED".equals(grn.getStatus())) {
            throw new IllegalStateException("Can only add items to received GRNs");
        }

        PurchaseOrderItem poItem = poItemRepository.findById(request.getPoItemId())
                .orElseThrow(() -> new IllegalStateException("PO item not found"));

        // Validate item belongs to the same PO
        if (!poItem.getPurchaseOrder().getId().equals(grn.getPurchaseOrder().getId())) {
            throw new IllegalStateException("PO item does not belong to this purchase order");
        }

        // Check if quantity doesn't exceed ordered
        if (request.getQuantityReceived().compareTo(poItem.getRemainingQuantity()) > 0) {
            throw new IllegalStateException("Received quantity exceeds ordered quantity");
        }

        GRNItem item = GRNItem.builder()
                .tenant(grn.getTenant())
                .grn(grn)
                .poItem(poItem)
                .quantityReceived(request.getQuantityReceived())
                .quantityAccepted(request.getQuantityReceived())
                .rejectionReason(null)
                .build();

        item = grnItemRepository.save(item);

        // Update PO item quantity received
        poItem.setQuantityReceived(
                poItem.getQuantityReceived().add(request.getQuantityReceived())
        );
        poItemRepository.save(poItem);

        // Update GRN total
        grn.setTotalQuantityReceived(
                grn.getTotalQuantityReceived().add(request.getQuantityReceived())
        );
        grnRepository.save(grn);

        // Audit log
        auditService.logEvent(tenantId, userId, "CREATE", "GRNItem", item.getId(), null,
                Map.of("grnId", grnId, "quantity", request.getQuantityReceived()),
                null, null);

        return mapItemToDto(item);
    }

    /**
     * Inspect and accept GRN items
     */
    @Transactional
    public GoodsReceiptNoteDto inspectGRN(Long grnId, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();

        GoodsReceiptNote grn = grnRepository.findById(grnId)
                .orElseThrow(() -> new IllegalStateException("GRN not found"));

        if (!grn.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("GRN does not belong to this tenant");
        }

        grn.setStatus("INSPECTED");
        grn.setInspectedBy(userId);
        grn.setInspectedAt(LocalDateTime.now());
        grn = grnRepository.save(grn);

        // Update PO status to RECEIVED if all items received
        PurchaseOrder po = grn.getPurchaseOrder();
        long pendingItems = poItemRepository.countPartiallyReceivedItems(po.getId());
        if (pendingItems == 0) {
            po.setStatus("RECEIVED");
            po.setActualDeliveryDate(LocalDateTime.now());
            poRepository.save(po);
        }

        // Audit log
        auditService.logEvent(tenantId, userId, "UPDATE", "GoodsReceiptNote", grn.getId(),
                Map.of("status", "RECEIVED"),
                Map.of("status", "INSPECTED"),
                null, null);

        log.info("Inspected GRN: {}", grnId);

        return mapToDto(grn);
    }

    /**
     * Get GRN by ID
     */
    public GoodsReceiptNoteDto getGRN(Long grnId) {
        Long tenantId = tenantContextService.requireTenantId();

        GoodsReceiptNote grn = grnRepository.findById(grnId)
                .orElseThrow(() -> new IllegalStateException("GRN not found"));

        if (!grn.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("GRN does not belong to this tenant");
        }

        return mapToDto(grn);
    }

    /**
     * Get GRNs by status
     */
    public List<GoodsReceiptNoteDto> getGRNsByStatus(String status) {
        Long tenantId = tenantContextService.requireTenantId();

        List<GoodsReceiptNote> grns = grnRepository.findByTenantAndStatus(tenantId, status);
        return grns.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // Helper methods
    private String generateGrnNumber(Long tenantId) {
        String prefix = String.format("GRN-%s-", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")));
        long count = grnRepository.findByTenantAndStatus(tenantId, "RECEIVED").size() +
                     grnRepository.findByTenantAndStatus(tenantId, "INSPECTED").size();
        return prefix + String.format("%05d", count + 1);
    }

    private GoodsReceiptNoteDto mapToDto(GoodsReceiptNote grn) {
        return GoodsReceiptNoteDto.builder()
                .id(grn.getId())
                .grnNumber(grn.getGrnNumber())
                .purchaseOrderId(grn.getPurchaseOrder().getId())
                .poNumber(grn.getPurchaseOrder().getPoNumber())
                .supplierId(grn.getSupplier().getId())
                .supplierName(grn.getSupplier().getName())
                .receiptDate(grn.getReceiptDate())
                .status(grn.getStatus())
                .totalQuantityReceived(grn.getTotalQuantityReceived())
                .createdAt(grn.getCreatedAt())
                .build();
    }

    private GRNItemDto mapItemToDto(GRNItem item) {
        return GRNItemDto.builder()
                .id(item.getId())
                .poItemId(item.getPoItem().getId())
                .quantityReceived(item.getQuantityReceived())
                .quantityAccepted(item.getQuantityAccepted())
                .quantityRejected(item.getQuantityRejected())
                .build();
    }
}
