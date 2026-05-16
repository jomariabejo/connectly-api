package com.jomariabejo.connectly_api.payments_api.exception;

public class InvalidPaymentRequestException extends PaymentException {
    public InvalidPaymentRequestException(String message) {
        super(message);
    }
}
