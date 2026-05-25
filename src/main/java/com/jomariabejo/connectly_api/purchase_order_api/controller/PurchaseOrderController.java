package com.jomariabejo.connectly_api.purchase_order_api.controller;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.purchase_order_api.dto.CreatePurchaseOrderRequest;
import com.jomariabejo.connectly_api.purchase_order_api.dto.PurchaseOrderResponse;
import com.jomariabejo.connectly_api.purchase_order_api.dto.PurchaseOrderSummaryDto;
import com.jomariabejo.connectly_api.purchase_order_api.dto.ReceivePurchaseOrderRequest;
import com.jomariabejo.connectly_api.purchase_order_api.service.PurchaseOrderService;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.tenant_api.annotation.RequiresProduct;
import com.jomariabejo.connectly_api.tenant_api.entity.ProductCode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/purchase-orders")
@RequiresProduct(ProductCode.INVENTORY_MANAGEMENT)
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final AuthenticationService authenticationService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService,
                                   AuthenticationService authenticationService) {
        this.purchaseOrderService = purchaseOrderService;
        this.authenticationService = authenticationService;
    }

    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> createPurchaseOrder(
            @RequestBody @Valid CreatePurchaseOrderRequest request) {
        User currentUser = authenticationService.getAuthenticatedUser();
        PurchaseOrderResponse response = purchaseOrderService.createPurchaseOrder(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PurchaseOrderSummaryDto>> listPurchaseOrders() {
        return ResponseEntity.ok(purchaseOrderService.listPurchaseOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponse> getPurchaseOrder(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrder(id));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<PurchaseOrderResponse> receivePurchaseOrder(
            @PathVariable Long id,
            @RequestBody @Valid ReceivePurchaseOrderRequest request) {
        return ResponseEntity.ok(purchaseOrderService.receivePurchaseOrder(id, request));
    }
}
