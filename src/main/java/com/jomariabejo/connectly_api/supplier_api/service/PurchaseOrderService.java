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
public class PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderItemRepository poItemRepository;
    private final SupplierRepository supplierRepository;
    private final TenantContextService tenantContextService;
    private final AuditLoggingService auditService;
    private final TenantRepository tenantRepository;

    /**
     * Create a new purchase order
     */
    @Transactional
    public PurchaseOrderDto createPurchaseOrder(CreatePurchaseOrderRequest request, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();

        // Get supplier
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new IllegalStateException("Supplier not found"));

        if (!supplier.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Supplier does not belong to this tenant");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));

        // Generate PO number
        String poNumber = generatePoNumber(tenantId);

        PurchaseOrder po = PurchaseOrder.builder()
                .tenant(tenant)
                .poNumber(poNumber)
                .supplier(supplier)
                .orderDate(LocalDateTime.now())
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .status("DRAFT")
                .totalAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .grandTotal(BigDecimal.ZERO)
                .currencyCode("PHP")
                .notes(request.getNotes())
                .createdBy(userId)
                .build();

        po = poRepository.save(po);

        // Audit log
        auditService.logEvent(tenantId, userId, "CREATE", "PurchaseOrder", po.getId(), null,
                Map.of("poNumber", poNumber, "supplierId", request.getSupplierId()),
                null, null);

        log.info("Created purchase order: {} for supplier: {}", po.getId(), request.getSupplierId());

        return mapToDto(po);
    }

    /**
     * Get purchase order by ID
     */
    public PurchaseOrderDto getPurchaseOrder(Long poId) {
        Long tenantId = tenantContextService.requireTenantId();

        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new IllegalStateException("Purchase order not found"));

        if (!po.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Purchase order does not belong to this tenant");
        }

        return mapToDto(po);
    }

    /**
     * Add line item to purchase order
     */
    @Transactional
    public PurchaseOrderItemDto addLineItem(Long poId, AddPurchaseOrderItemRequest request, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();

        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new IllegalStateException("Purchase order not found"));

        if (!po.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Purchase order does not belong to this tenant");
        }

        if (!"DRAFT".equals(po.getStatus())) {
            throw new IllegalStateException("Can only add items to draft purchase orders");
        }

        // Determine line number
        List<PurchaseOrderItem> items = poItemRepository.findByPurchaseOrder(poId);
        int lineNumber = items.size() + 1;

        BigDecimal totalCost = request.getQuantityOrdered().multiply(request.getUnitCost());

        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .tenant(po.getTenant())
                .purchaseOrder(po)
                .itemCode(request.getItemCode())
                .description(request.getDescription())
                .quantityOrdered(request.getQuantityOrdered())
                .quantityReceived(BigDecimal.ZERO)
                .unitOfMeasure(request.getUnitOfMeasure())
                .unitCost(request.getUnitCost())
                .totalCost(totalCost)
                .lineNumber(lineNumber)
                .build();

        item = poItemRepository.save(item);

        // Update PO totals
        recalculatePOTotals(po);

        // Audit log
        auditService.logEvent(tenantId, userId, "CREATE", "PurchaseOrderItem", item.getId(), null,
                Map.of("poId", poId, "description", request.getDescription(), "quantity", request.getQuantityOrdered()),
                null, null);

        return mapItemToDto(item);
    }

    /**
     * Submit purchase order for approval
     */
    @Transactional
    public PurchaseOrderDto submitPurchaseOrder(Long poId, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();

        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new IllegalStateException("Purchase order not found"));

        if (!po.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Purchase order does not belong to this tenant");
        }

        if (!"DRAFT".equals(po.getStatus())) {
            throw new IllegalStateException("Can only submit draft purchase orders");
        }

        List<PurchaseOrderItem> items = poItemRepository.findByPurchaseOrder(poId);
        if (items.isEmpty()) {
            throw new IllegalStateException("Purchase order must have at least one line item");
        }

        po.setStatus("SUBMITTED");
        po = poRepository.save(po);

        // Audit log
        auditService.logEvent(tenantId, userId, "UPDATE", "PurchaseOrder", po.getId(),
                Map.of("status", "DRAFT"),
                Map.of("status", "SUBMITTED"),
                null, null);

        log.info("Submitted purchase order: {}", poId);

        return mapToDto(po);
    }

    /**
     * Approve purchase order
     */
    @Transactional
    public PurchaseOrderDto approvePurchaseOrder(Long poId, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();

        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new IllegalStateException("Purchase order not found"));

        if (!po.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Purchase order does not belong to this tenant");
        }

        if (!("DRAFT".equals(po.getStatus()) || "SUBMITTED".equals(po.getStatus()))) {
            throw new IllegalStateException("Can only approve draft or submitted purchase orders");
        }

        po.setStatus("APPROVED");
        po.setApprovedBy(userId);
        po.setApprovedAt(LocalDateTime.now());
        po = poRepository.save(po);

        // Audit log
        auditService.logEvent(tenantId, userId, "UPDATE", "PurchaseOrder", po.getId(),
                Map.of("status", "SUBMITTED"),
                Map.of("status", "APPROVED", "approvedBy", userId),
                null, null);

        log.info("Approved purchase order: {}", poId);

        return mapToDto(po);
    }

    /**
     * Cancel purchase order
     */
    @Transactional
    public PurchaseOrderDto cancelPurchaseOrder(Long poId, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();

        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new IllegalStateException("Purchase order not found"));

        if (!po.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Purchase order does not belong to this tenant");
        }

        String oldStatus = po.getStatus();
        po.setStatus("CANCELLED");
        po = poRepository.save(po);

        // Audit log
        auditService.logEvent(tenantId, userId, "UPDATE", "PurchaseOrder", po.getId(),
                Map.of("status", oldStatus),
                Map.of("status", "CANCELLED"),
                null, null);

        log.info("Cancelled purchase order: {}", poId);

        return mapToDto(po);
    }

    /**
     * Get purchase orders by status
     */
    public List<PurchaseOrderDto> getPurchaseOrdersByStatus(String status) {
        Long tenantId = tenantContextService.requireTenantId();

        List<PurchaseOrder> orders = poRepository.findByTenantAndStatus(tenantId, status);
        return orders.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // Helper methods
    private String generatePoNumber(Long tenantId) {
        // Format: PO-YYYYMM-XXXXX
        String prefix = String.format("PO-%s-", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")));
        long count = poRepository.countByTenantAndStatus(tenantId, "DRAFT") + 
                     poRepository.countByTenantAndStatus(tenantId, "SUBMITTED") +
                     poRepository.countByTenantAndStatus(tenantId, "APPROVED");
        return prefix + String.format("%05d", count + 1);
    }

    private void recalculatePOTotals(PurchaseOrder po) {
        List<PurchaseOrderItem> items = poItemRepository.findByPurchaseOrder(po.getId());
        BigDecimal total = items.stream()
                .map(PurchaseOrderItem::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        po.setTotalAmount(total);
        po.setGrandTotal(total
                .add(po.getTaxAmount() != null ? po.getTaxAmount() : BigDecimal.ZERO)
                .subtract(po.getDiscountAmount() != null ? po.getDiscountAmount() : BigDecimal.ZERO));
        
        poRepository.save(po);
    }

    private PurchaseOrderDto mapToDto(PurchaseOrder po) {
        return PurchaseOrderDto.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .supplierId(po.getSupplier().getId())
                .supplierName(po.getSupplier().getName())
                .orderDate(po.getOrderDate())
                .expectedDeliveryDate(po.getExpectedDeliveryDate())
                .actualDeliveryDate(po.getActualDeliveryDate())
                .status(po.getStatus())
                .totalAmount(po.getTotalAmount())
                .taxAmount(po.getTaxAmount())
                .discountAmount(po.getDiscountAmount())
                .grandTotal(po.getGrandTotal())
                .currencyCode(po.getCurrencyCode())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private PurchaseOrderItemDto mapItemToDto(PurchaseOrderItem item) {
        return PurchaseOrderItemDto.builder()
                .id(item.getId())
                .itemCode(item.getItemCode())
                .description(item.getDescription())
                .quantityOrdered(item.getQuantityOrdered())
                .quantityReceived(item.getQuantityReceived())
                .unitOfMeasure(item.getUnitOfMeasure())
                .unitCost(item.getUnitCost())
                .totalCost(item.getTotalCost())
                .lineNumber(item.getLineNumber())
                .build();
    }
}
