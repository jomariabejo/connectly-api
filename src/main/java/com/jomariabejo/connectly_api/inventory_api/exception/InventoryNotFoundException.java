package com.jomariabejo.connectly_api.inventory_api.exception;

public class InventoryNotFoundException extends RuntimeException {
    public InventoryNotFoundException(String sku) {
        super("Inventory item with SKU " + sku + " not found");
    }
}
