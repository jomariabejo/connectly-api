package com.jomariabejo.connectly_api.payments_api.provider;

import com.jomariabejo.connectly_api.payments_api.entity.PaymentIntentType;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentProvider;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CreateCheckoutCommand {
    Long paymentId;
    Long orderId;
    String orderReference;
    PaymentProvider provider;
    Long amountMinor;
    String currency;
    String successUrl;
    String cancelUrl;
    String idempotencyKey;
    PaymentIntentType intentType;
}
