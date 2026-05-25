package com.jomariabejo.connectly_api.purchase_order_api.service;

import com.jomariabejo.connectly_api.inventory_api.entity.InventoryItem;
import com.jomariabejo.connectly_api.inventory_api.exception.InventoryNotFoundException;
import com.jomariabejo.connectly_api.inventory_api.repository.InventoryItemRepository;
import com.jomariabejo.connectly_api.inventory_api.service.InventoryService;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.purchase_order_api.dto.CreatePurchaseOrderLineRequest;
import com.jomariabejo.connectly_api.purchase_order_api.dto.CreatePurchaseOrderRequest;
import com.jomariabejo.connectly_api.purchase_order_api.dto.PurchaseOrderResponse;
import com.jomariabejo.connectly_api.purchase_order_api.dto.PurchaseOrderSummaryDto;
import com.jomariabejo.connectly_api.purchase_order_api.dto.ReceivePurchaseOrderLineRequest;
import com.jomariabejo.connectly_api.purchase_order_api.dto.ReceivePurchaseOrderRequest;
import com.jomariabejo.connectly_api.purchase_order_api.entity.PurchaseOrder;
import com.jomariabejo.connectly_api.purchase_order_api.entity.PurchaseOrderLine;
import com.jomariabejo.connectly_api.purchase_order_api.entity.PurchaseOrderStatus;
import com.jomariabejo.connectly_api.purchase_order_api.exception.InvalidPurchaseOrderRequestException;
import com.jomariabejo.connectly_api.purchase_order_api.exception.PurchaseOrderNotFoundException;
import com.jomariabejo.connectly_api.purchase_order_api.mapper.PurchaseOrderMapper;
import com.jomariabejo.connectly_api.purchase_order_api.repository.PurchaseOrderRepository;
import com.jomariabejo.connectly_api.tenant_api.context.TenantContext;
import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import com.jomariabejo.connectly_api.tenant_api.entity.TenantRole;
import com.jomariabejo.connectly_api.tenant_api.exception.TenantAccessDeniedException;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import com.jomariabejo.connectly_api.tenant_api.support.TenantRolePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PurchaseOrderService {

    private static final DateTimeFormatter PO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryService inventoryService;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final TenantContextService tenantContextService;

    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository,
                                InventoryItemRepository inventoryItemRepository,
                                InventoryService inventoryService,
                                PurchaseOrderMapper purchaseOrderMapper,
                                TenantContextService tenantContextService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryService = inventoryService;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.tenantContextService = tenantContextService;
    }

    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderRequest request, User createdBy) {
        requireInventoryWriteAccess();
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new InvalidPurchaseOrderRequestException("Purchase order must contain at least one line");
        }

        Long tenantId = tenantContextService.requireTenantId();
        Tenant tenant = tenantContextService.requireTenant();

        List<PurchaseOrderLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CreatePurchaseOrderLineRequest lineReq : request.getLines()) {
            String sku = normalizeSku(lineReq.getSku());
            int quantity = requirePositive(lineReq.getQuantity(), "Quantity must be greater than 0");
            InventoryItem item = inventoryItemRepository.findByTenantIdAndSkuAndActiveTrue(tenantId, sku)
                    .orElseThrow(() -> new InventoryNotFoundException(sku));

            BigDecimal unitCost = lineReq.getUnitCost() != null
                    ? requireNonNegative(lineReq.getUnitCost(), "Unit cost must be non-negative")
                    : item.getUnitPrice();
            BigDecimal lineTotal = unitCost.multiply(BigDecimal.valueOf(quantity));
            total = total.add(lineTotal);

            PurchaseOrderLine line = PurchaseOrderLine.builder()
                    .sku(sku)
                    .productName(item.getName())
                    .quantityOrdered(quantity)
                    .quantityReceived(0)
                    .unitCost(unitCost)
                    .lineTotal(lineTotal)
                    .build();
            lines.add(line);
        }

        PurchaseOrder order = PurchaseOrder.builder()
                .tenant(tenant)
                .poNumber(generatePoNumber(tenantId))
                .supplierName(trimToNull(request.getSupplierName()))
                .status(PurchaseOrderStatus.ORDERED)
                .notes(trimToNull(request.getNotes()))
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .totalAmount(total)
                .createdBy(createdBy)
                .build();

        for (PurchaseOrderLine line : lines) {
            line.setPurchaseOrder(order);
            order.getLines().add(line);
        }

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        return purchaseOrderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderSummaryDto> listPurchaseOrders() {
        Long tenantId = tenantContextService.requireTenantId();
        List<PurchaseOrder> orders = purchaseOrderRepository.findTop50ByTenantIdOrderByCreatedDateDesc(tenantId);
        return purchaseOrderMapper.toSummaries(orders);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getPurchaseOrder(Long id) {
        PurchaseOrder order = findOrderForTenant(id);
        return purchaseOrderMapper.toResponse(order);
    }

    @Transactional
    public PurchaseOrderResponse receivePurchaseOrder(Long id, ReceivePurchaseOrderRequest request) {
        requireInventoryWriteAccess();
        PurchaseOrder order = findOrderForTenant(id);

        if (order.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new InvalidPurchaseOrderRequestException("Cannot receive a cancelled purchase order");
        }
        if (order.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new InvalidPurchaseOrderRequestException("Purchase order is already fully received");
        }
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new InvalidPurchaseOrderRequestException("Receive request must contain at least one line");
        }

        Map<String, PurchaseOrderLine> lineBySku = order.getLines().stream()
                .collect(Collectors.toMap(PurchaseOrderLine::getSku, Function.identity()));

        for (ReceivePurchaseOrderLineRequest receiveLine : request.getLines()) {
            String sku = normalizeSku(receiveLine.getSku());
            int qty = requirePositive(receiveLine.getQuantityReceived(), "Receive quantity must be greater than 0");

            PurchaseOrderLine poLine = lineBySku.get(sku);
            if (poLine == null) {
                throw new InvalidPurchaseOrderRequestException("SKU " + sku + " is not on this purchase order");
            }

            int remaining = poLine.getQuantityOrdered() - poLine.getQuantityReceived();
            if (qty > remaining) {
                throw new InvalidPurchaseOrderRequestException(
                        "Cannot receive " + qty + " for SKU " + sku + "; only " + remaining + " remaining");
            }

            inventoryService.receivePurchaseOrderStock(sku, qty, order.getId());
            poLine.setQuantityReceived(poLine.getQuantityReceived() + qty);
        }

        boolean allReceived = order.getLines().stream()
                .allMatch(line -> line.getQuantityReceived().equals(line.getQuantityOrdered()));
        order.setStatus(allReceived ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.PARTIALLY_RECEIVED);

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        return purchaseOrderMapper.toResponse(saved);
    }

    private PurchaseOrder findOrderForTenant(Long id) {
        return purchaseOrderRepository.findByTenantIdAndId(tenantContextService.requireTenantId(), id)
                .orElseThrow(() -> new PurchaseOrderNotFoundException(id));
    }

    private String generatePoNumber(Long tenantId) {
        String datePart = LocalDate.now().format(PO_DATE_FORMAT);
        String prefix = "PO-" + datePart + "-";
        long count = purchaseOrderRepository.countByTenantIdAndPoNumberStartingWith(tenantId, prefix);
        String candidate;
        do {
            count++;
            candidate = prefix + String.format("%03d", count);
        } while (purchaseOrderRepository.existsByTenantIdAndPoNumber(tenantId, candidate));
        return candidate;
    }

    private void requireInventoryWriteAccess() {
        TenantRole role = TenantContext.getTenantRole();
        if (!TenantRolePolicy.canWriteInventory(role)) {
            throw new TenantAccessDeniedException(
                    "Your store role does not have permission to manage purchase orders");
        }
    }

    private String normalizeSku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new InvalidPurchaseOrderRequestException("SKU is required");
        }
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private int requirePositive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new InvalidPurchaseOrderRequestException(message);
        }
        return value;
    }

    private BigDecimal requireNonNegative(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPurchaseOrderRequestException(message);
        }
        return value;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
