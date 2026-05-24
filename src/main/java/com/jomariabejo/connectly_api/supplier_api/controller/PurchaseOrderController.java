package com.jomariabejo.connectly_api.supplier_api.controller;

import com.jomariabejo.connectly_api.supplier_api.dto.*;
import com.jomariabejo.connectly_api.supplier_api.service.PurchaseOrderService;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final AuthenticationService authenticationService;

    /**
     * Create a purchase order
     * POST /api/v1/purchase-orders
     */
    @PostMapping
    public ResponseEntity<PurchaseOrderDto> createPurchaseOrder(
            @Valid @RequestBody CreatePurchaseOrderRequest request) {
        User user = authenticationService.getAuthenticatedUser();
        PurchaseOrderDto po = purchaseOrderService.createPurchaseOrder(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(po);
    }

    /**
     * Get purchase order by ID
     * GET /api/v1/purchase-orders/:poId
     */
    @GetMapping("/{poId}")
    public ResponseEntity<PurchaseOrderDto> getPurchaseOrder(@PathVariable Long poId) {
        PurchaseOrderDto po = purchaseOrderService.getPurchaseOrder(poId);
        return ResponseEntity.ok(po);
    }

    /**
     * Add line item to purchase order
     * POST /api/v1/purchase-orders/:poId/items
     */
    @PostMapping("/{poId}/items")
    public ResponseEntity<PurchaseOrderItemDto> addLineItem(
            @PathVariable Long poId,
            @Valid @RequestBody AddPurchaseOrderItemRequest request) {
        User user = authenticationService.getAuthenticatedUser();
        PurchaseOrderItemDto item = purchaseOrderService.addLineItem(poId, request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    /**
     * Submit purchase order for approval
     * PUT /api/v1/purchase-orders/:poId/submit
     */
    @PutMapping("/{poId}/submit")
    public ResponseEntity<PurchaseOrderDto> submitPurchaseOrder(@PathVariable Long poId) {
        User user = authenticationService.getAuthenticatedUser();
        PurchaseOrderDto po = purchaseOrderService.submitPurchaseOrder(poId, user.getId());
        return ResponseEntity.ok(po);
    }

    /**
     * Approve purchase order
     * PUT /api/v1/purchase-orders/:poId/approve
     */
    @PutMapping("/{poId}/approve")
    public ResponseEntity<PurchaseOrderDto> approvePurchaseOrder(@PathVariable Long poId) {
        User user = authenticationService.getAuthenticatedUser();
        PurchaseOrderDto po = purchaseOrderService.approvePurchaseOrder(poId, user.getId());
        return ResponseEntity.ok(po);
    }

    /**
     * Cancel purchase order
     * PUT /api/v1/purchase-orders/:poId/cancel
     */
    @PutMapping("/{poId}/cancel")
    public ResponseEntity<PurchaseOrderDto> cancelPurchaseOrder(@PathVariable Long poId) {
        User user = authenticationService.getAuthenticatedUser();
        PurchaseOrderDto po = purchaseOrderService.cancelPurchaseOrder(poId, user.getId());
        return ResponseEntity.ok(po);
    }

    /**
     * Get purchase orders by status
     * GET /api/v1/purchase-orders/status/:status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<PurchaseOrderDto>> getPurchaseOrdersByStatus(@PathVariable String status) {
        List<PurchaseOrderDto> orders = purchaseOrderService.getPurchaseOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }
}
