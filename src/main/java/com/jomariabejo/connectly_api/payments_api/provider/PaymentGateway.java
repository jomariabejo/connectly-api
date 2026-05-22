package com.jomariabejo.connectly_api.payments_api.provider;

import com.jomariabejo.connectly_api.payments_api.entity.PaymentProvider;

import java.util.Map;

public interface PaymentGateway {
    PaymentProvider provider();

    CreateCheckoutResult createCheckout(CreateCheckoutCommand command);

    ProviderPaymentStatus retrievePayment(String providerPaymentId);

    WebhookVerificationResult verifyWebhook(String rawPayload, Map<String, String> headers);

    ProviderWebhookEvent parseWebhook(String rawPayload);
}
