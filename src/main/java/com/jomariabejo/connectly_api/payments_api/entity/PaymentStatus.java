package com.jomariabejo.connectly_api.payments_api.entity;

public enum PaymentStatus {
    PENDING,
    REQUIRES_ACTION,
    PROCESSING,
    PAID,
    FAILED,
    CANCELLED,
    REFUNDED,
    PARTIALLY_REFUNDED
}
