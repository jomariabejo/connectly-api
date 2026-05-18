package com.jomariabejo.connectly_api.inventory_api.exception;

public class InsufficientInventoryException extends RuntimeException {
    public InsufficientInventoryException(String sku, int requested, int available) {
        super("Insufficient inventory for SKU " + sku + ": requested " + requested + ", available " + available);
    }
}
