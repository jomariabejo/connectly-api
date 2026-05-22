package com.jomariabejo.connectly_api.payments_api.provider;

import com.jomariabejo.connectly_api.payments_api.entity.PaymentStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProviderPaymentStatus {
    String providerPaymentId;
    String providerCheckoutId;
    PaymentStatus status;
    String failureCode;
    String failureMessage;
}
