package com.jomariabejo.connectly_api.payments_api.provider;

import com.jomariabejo.connectly_api.payments_api.entity.PaymentProvider;
import com.jomariabejo.connectly_api.payments_api.exception.PaymentGatewayNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentGatewayRegistryTest {

    @Test
    void returnsGatewayForProvider() {
        PaymentGateway paymongoGateway = gateway(PaymentProvider.PAYMONGO);
        PaymentGatewayRegistry registry = new PaymentGatewayRegistry(List.of(paymongoGateway));

        assertThat(registry.getGateway(PaymentProvider.PAYMONGO)).isSameAs(paymongoGateway);
    }

    @Test
    void rejectsUnconfiguredProvider() {
        PaymentGatewayRegistry registry = new PaymentGatewayRegistry(List.of(gateway(PaymentProvider.PAYMONGO)));

        assertThatThrownBy(() -> registry.getGateway(PaymentProvider.PAYPAL))
                .isInstanceOf(PaymentGatewayNotFoundException.class)
                .hasMessageContaining("PAYPAL");
    }

    private PaymentGateway gateway(PaymentProvider provider) {
        return new PaymentGateway() {
            @Override
            public PaymentProvider provider() {
                return provider;
            }

            @Override
            public CreateCheckoutResult createCheckout(CreateCheckoutCommand command) {
                return null;
            }

            @Override
            public ProviderPaymentStatus retrievePayment(String providerPaymentId) {
                return null;
            }

            @Override
            public WebhookVerificationResult verifyWebhook(String rawPayload, Map<String, String> headers) {
                return WebhookVerificationResult.valid();
            }

            @Override
            public ProviderWebhookEvent parseWebhook(String rawPayload) {
                return null;
            }
        };
    }
}
