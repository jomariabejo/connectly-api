package com.jomariabejo.connectly_api.payments_api.exception;

public class PaymentNotFoundException extends PaymentException {
    public PaymentNotFoundException(Long paymentId) {
        super("Payment with ID " + paymentId + " not found");
    }
}
