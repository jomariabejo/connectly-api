package com.jomariabejo.connectly_api.payments_api.exception;

public class PaymentGatewayNotFoundException extends PaymentException {
    public PaymentGatewayNotFoundException(String message) {
        super(message);
    }
}
