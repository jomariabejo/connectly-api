package com.jomariabejo.connectly_api.payments_api.provider;

import com.jomariabejo.connectly_api.payments_api.entity.PaymentStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CreateCheckoutResult {
    String providerPaymentId;
    String providerCheckoutId;
    String checkoutUrl;
    PaymentStatus status;
}
