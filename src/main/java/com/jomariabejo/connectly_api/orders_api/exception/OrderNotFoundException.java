package com.jomariabejo.connectly_api.orders_api.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) {
        super(message);
    }

    public OrderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public OrderNotFoundException(Long orderId) {
        super("Order with ID " + orderId + " not found");
    }
}
