package com.jomariabejo.connectly_api.supplier_api.controller;

import com.jomariabejo.connectly_api.supplier_api.dto.*;
import com.jomariabejo.connectly_api.supplier_api.service.PurchaseInvoiceService;
import com.jomariabejo.connectly_api.tenant_api.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-invoices")
@RequiredArgsConstructor
@Slf4j
public class PurchaseInvoiceController {

    private final PurchaseInvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<PurchaseInvoiceDto> createInvoice(@RequestBody CreatePurchaseInvoiceRequest request) {
        Long userId = TenantContext.getUserId();
        PurchaseInvoiceDto invoice = invoiceService.createInvoice(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
    }

    @PostMapping("/{invoiceId}/items")
    public ResponseEntity<InvoiceItemDto> addLineItem(
            @PathVariable Long invoiceId,
            @RequestBody AddInvoiceItemRequest request) {
        Long userId = TenantContext.getUserId();
        InvoiceItemDto item = invoiceService.addLineItem(invoiceId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @PutMapping("/{invoiceId}/approve")
    public ResponseEntity<PurchaseInvoiceDto> approveInvoice(@PathVariable Long invoiceId) {
        Long userId = TenantContext.getUserId();
        PurchaseInvoiceDto invoice = invoiceService.approveInvoice(invoiceId, userId);
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/{invoiceId}")
    public ResponseEntity<PurchaseInvoiceDto> getInvoice(@PathVariable Long invoiceId) {
        PurchaseInvoiceDto invoice = invoiceService.getInvoice(invoiceId);
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/supplier/{supplierId}/unpaid")
    public ResponseEntity<List<PurchaseInvoiceDto>> getUnpaidInvoicesBySupplier(
            @PathVariable Long supplierId) {
        List<PurchaseInvoiceDto> invoices = invoiceService.getUnpaidInvoicesBySupplier(supplierId);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<PurchaseInvoiceDto>> getOverdueInvoices() {
        List<PurchaseInvoiceDto> invoices = invoiceService.getOverdueInvoices();
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PurchaseInvoiceDto>> getInvoicesByStatus(@PathVariable String status) {
        List<PurchaseInvoiceDto> invoices = invoiceService.getInvoicesByStatus(status);
        return ResponseEntity.ok(invoices);
    }
}
