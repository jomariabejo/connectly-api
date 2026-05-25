package com.jomariabejo.connectly_api.purchase_order_api.exception;

public class PurchaseOrderNotFoundException extends RuntimeException {
    public PurchaseOrderNotFoundException(Long id) {
        super("Purchase order not found: " + id);
    }
}
