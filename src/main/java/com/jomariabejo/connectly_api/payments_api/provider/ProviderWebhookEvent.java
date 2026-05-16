package com.jomariabejo.connectly_api.payments_api.provider;

import com.jomariabejo.connectly_api.payments_api.entity.PaymentStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProviderWebhookEvent {
    String providerEventId;
    String eventType;
    String providerPaymentId;
    String providerCheckoutId;
    PaymentStatus paymentStatus;
    String failureCode;
    String failureMessage;
}
