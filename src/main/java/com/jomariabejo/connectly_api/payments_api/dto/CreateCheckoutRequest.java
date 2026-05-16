package com.jomariabejo.connectly_api.payments_api.dto;

import com.jomariabejo.connectly_api.payments_api.entity.PaymentIntentType;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCheckoutRequest {
    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Payment provider is required")
    private PaymentProvider provider;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Success URL is required")
    private String successUrl;

    @NotBlank(message = "Cancel URL is required")
    private String cancelUrl;

    private String idempotencyKey;

    @Builder.Default
    private PaymentIntentType intentType = PaymentIntentType.CAPTURE_NOW;
}
