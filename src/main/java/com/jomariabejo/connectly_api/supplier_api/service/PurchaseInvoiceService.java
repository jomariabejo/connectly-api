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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseInvoiceService {

    private final PurchaseInvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderItemRepository poItemRepository;
    private final PayablesLedgerRepository payablesLedgerRepository;
    private final SupplierRepository supplierRepository;
    private final TenantContextService tenantContextService;
    private final AuditLoggingService auditService;
    private final TenantRepository tenantRepository;

    /**
     * Create purchase invoice
     */
    @Transactional
    public PurchaseInvoiceDto createInvoice(CreatePurchaseInvoiceRequest request, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();

        // Check for duplicate invoice number
        var existing = invoiceRepository.findByTenantAndInvoiceNumber(tenantId, request.getInvoiceNumber());
        if (existing.isPresent()) {
            throw new IllegalStateException("Invoice number already exists: " + request.getInvoiceNumber());
        }

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new IllegalStateException("Supplier not found"));

        if (!supplier.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Supplier does not belong to this tenant");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));

        PurchaseOrder po = null;
        if (request.getPurchaseOrderId() != null) {
            po = poRepository.findById(request.getPurchaseOrderId())
                    .orElseThrow(() -> new IllegalStateException("Purchase order not found"));

            if (!po.getTenant().getId().equals(tenantId)) {
                throw new IllegalStateException("PO does not belong to this tenant");
            }

            if (!"RECEIVED".equals(po.getStatus())) {
                throw new IllegalStateException("Can only invoice received purchase orders");
            }
        }

        BigDecimal grandTotal = request.getTotalAmount()
                .add(request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO)
                .subtract(request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO);

        PurchaseInvoice invoice = PurchaseInvoice.builder()
                .tenant(tenant)
                .invoiceNumber(request.getInvoiceNumber())
                .purchaseOrder(po)
                .supplier(supplier)
                .invoiceDate(request.getInvoiceDate())
                .dueDate(request.getDueDate())
                .totalAmount(request.getTotalAmount())
                .taxAmount(request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO)
                .discountAmount(request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO)
                .grandTotal(grandTotal)
                .currencyCode("PHP")
                .status("DRAFT")
                .paymentStatus("UNPAID")
                .notes(request.getNotes())
                .build();

        invoice = invoiceRepository.save(invoice);

        // Create initial payables ledger entry
        PayablesLedger ledger = PayablesLedger.builder()
                .tenant(tenant)
                .supplier(supplier)
                .invoice(invoice)
                .transactionType("INVOICE")
                .amount(grandTotal)
                .balance(grandTotal)
                .transactionDate(LocalDateTime.now())
                .build();

        payablesLedgerRepository.save(ledger);

        // Audit log
        auditService.logEvent(tenantId, userId, "CREATE", "PurchaseInvoice", invoice.getId(), null,
                Map.of("invoiceNumber", request.getInvoiceNumber(), "total", grandTotal),
                null, null);

        log.info("Created invoice: {} for supplier: {}", invoice.getId(), request.getSupplierId());

        return mapToDto(invoice);
    }

    /**
     * Add line item to invoice
     */
    @Transactional
    public InvoiceItemDto addLineItem(Long invoiceId, AddInvoiceItemRequest request, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();

        PurchaseInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalStateException("Invoice not found"));

        if (!invoice.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Invoice does not belong to this tenant");
        }

        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new IllegalStateException("Can only add items to draft invoices");
        }

        PurchaseOrderItem poItem = null;
        if (request.getPoItemId() != null) {
            poItem = poItemRepository.findById(request.getPoItemId())
                    .orElseThrow(() -> new IllegalStateException("PO item not found"));
        }

        BigDecimal itemTotal = request.getQuantity().multiply(request.getUnitPrice());

        InvoiceItem item = InvoiceItem.builder()
                .tenant(invoice.getTenant())
                .invoice(invoice)
                .poItem(poItem)
                .description(request.getDescription())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .totalAmount(itemTotal)
                .build();

        item = invoiceItemRepository.save(item);

        // Audit log
        auditService.logEvent(tenantId, userId, "CREATE", "InvoiceItem", item.getId(), null,
                Map.of("invoiceId", invoiceId, "quantity", request.getQuantity()),
                null, null);

        return mapItemToDto(item);
    }

    /**
     * Approve/Submit invoice
     */
    @Transactional
    public PurchaseInvoiceDto approveInvoice(Long invoiceId, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();

        PurchaseInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalStateException("Invoice not found"));

        if (!invoice.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Invoice does not belong to this tenant");
        }

        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new IllegalStateException("Can only approve draft invoices");
        }

        invoice.setStatus("APPROVED");
        invoice = invoiceRepository.save(invoice);

        // Update PO status if all invoiced
        if (invoice.getPurchaseOrder() != null) {
            invoice.getPurchaseOrder().setStatus("INVOICED");
            poRepository.save(invoice.getPurchaseOrder());
        }

        // Audit log
        auditService.logEvent(tenantId, userId, "UPDATE", "PurchaseInvoice", invoice.getId(),
                Map.of("status", "DRAFT"),
                Map.of("status", "APPROVED"),
                null, null);

        log.info("Approved invoice: {}", invoiceId);

        return mapToDto(invoice);
    }

    /**
     * Get invoice by ID
     */
    public PurchaseInvoiceDto getInvoice(Long invoiceId) {
        Long tenantId = tenantContextService.requireTenantId();

        PurchaseInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalStateException("Invoice not found"));

        if (!invoice.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Invoice does not belong to this tenant");
        }

        return mapToDto(invoice);
    }

    /**
     * Get unpaid invoices by supplier
     */
    public List<PurchaseInvoiceDto> getUnpaidInvoicesBySupplier(Long supplierId) {
        Long tenantId = tenantContextService.requireTenantId();

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalStateException("Supplier not found"));

        if (!supplier.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Supplier does not belong to this tenant");
        }

        List<PurchaseInvoice> invoices = invoiceRepository.findUnpaidBySupplier(supplierId);
        return invoices.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get overdue invoices
     */
    public List<PurchaseInvoiceDto> getOverdueInvoices() {
        Long tenantId = tenantContextService.requireTenantId();

        List<PurchaseInvoice> invoices = invoiceRepository.findOverdueInvoices(tenantId);
        return invoices.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get invoices by status
     */
    public List<PurchaseInvoiceDto> getInvoicesByStatus(String status) {
        Long tenantId = tenantContextService.requireTenantId();

        List<PurchaseInvoice> invoices = invoiceRepository.findByTenantAndStatus(tenantId, status);
        return invoices.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // Mapping methods
    private PurchaseInvoiceDto mapToDto(PurchaseInvoice invoice) {
        return PurchaseInvoiceDto.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .supplierId(invoice.getSupplier().getId())
                .supplierName(invoice.getSupplier().getName())
                .invoiceDate(invoice.getInvoiceDate().atStartOfDay())
                .dueDate(invoice.getDueDate().atStartOfDay())
                .totalAmount(invoice.getTotalAmount())
                .taxAmount(invoice.getTaxAmount())
                .discountAmount(invoice.getDiscountAmount())
                .grandTotal(invoice.getGrandTotal())
                .status(invoice.getStatus())
                .paymentStatus(invoice.getPaymentStatus())
                .createdAt(invoice.getCreatedAt())
                .build();
    }

    private InvoiceItemDto mapItemToDto(InvoiceItem item) {
        return InvoiceItemDto.builder()
                .id(item.getId())
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalAmount(item.getTotalAmount())
                .build();
    }
}
