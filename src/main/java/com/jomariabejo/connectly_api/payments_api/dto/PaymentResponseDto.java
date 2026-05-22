package com.jomariabejo.connectly_api.payments_api.dto;

import com.jomariabejo.connectly_api.payments_api.entity.PaymentIntentType;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentProvider;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDto {
    private Long paymentId;
    private Long orderId;
    private Long customerId;
    private PaymentProvider provider;
    private PaymentStatus status;
    private PaymentIntentType intentType;
    private Long amount;
    private String currency;
    private String checkoutUrl;
    private String providerPaymentId;
    private String providerCheckoutId;
    private String failureCode;
    private String failureMessage;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
