package com.jomariabejo.connectly_api.payments_api.mapper;

import com.jomariabejo.connectly_api.payments_api.dto.PaymentResponseDto;
import com.jomariabejo.connectly_api.payments_api.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
    public PaymentResponseDto toDto(Payment payment) {
        if (payment == null) {
            return null;
        }

        return PaymentResponseDto.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder() == null ? null : payment.getOrder().getId())
                .customerId(payment.getCustomer() == null ? null : payment.getCustomer().getId())
                .provider(payment.getProvider())
                .status(payment.getStatus())
                .intentType(payment.getIntentType())
                .amount(payment.getAmountMinor())
                .currency(payment.getCurrency())
                .checkoutUrl(payment.getCheckoutUrl())
                .providerPaymentId(payment.getProviderPaymentId())
                .providerCheckoutId(payment.getProviderCheckoutId())
                .failureCode(payment.getFailureCode())
                .failureMessage(payment.getFailureMessage())
                .createdDate(payment.getCreatedDate())
                .updatedDate(payment.getUpdatedDate())
                .build();
    }
}
