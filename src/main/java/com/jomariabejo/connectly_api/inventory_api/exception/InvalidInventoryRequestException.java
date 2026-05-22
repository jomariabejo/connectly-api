package com.jomariabejo.connectly_api.inventory_api.exception;

public class InvalidInventoryRequestException extends RuntimeException {
    public InvalidInventoryRequestException(String message) {
        super(message);
    }
}
