package com.jomariabejo.connectly_api.payments_api.provider.paymongo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jomariabejo.connectly_api.payments_api.config.PaymentProperties;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentProvider;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentStatus;
import com.jomariabejo.connectly_api.payments_api.exception.PaymentGatewayException;
import com.jomariabejo.connectly_api.payments_api.provider.CreateCheckoutCommand;
import com.jomariabejo.connectly_api.payments_api.provider.CreateCheckoutResult;
import com.jomariabejo.connectly_api.payments_api.provider.PaymentGateway;
import com.jomariabejo.connectly_api.payments_api.provider.ProviderJson;
import com.jomariabejo.connectly_api.payments_api.provider.ProviderPaymentStatus;
import com.jomariabejo.connectly_api.payments_api.provider.ProviderWebhookEvent;
import com.jomariabejo.connectly_api.payments_api.provider.WebhookVerificationResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class PaymongoPaymentGateway implements PaymentGateway {
    private final PaymentProperties paymentProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PaymongoPaymentGateway(PaymentProperties paymentProperties, ObjectMapper objectMapper) {
        this.paymentProperties = paymentProperties;
        this.restClient = RestClient.builder().build();
        this.objectMapper = objectMapper;
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.PAYMONGO;
    }

    @Override
    public CreateCheckoutResult createCheckout(CreateCheckoutCommand command) {
        PaymentProperties.Paymongo config = paymentProperties.getPaymongo();
        ensureConfigured(config);

        Map<String, Object> payload = Map.of(
                "data", Map.of(
                        "attributes", Map.of(
                                "line_items", List.of(Map.of(
                                        "currency", command.getCurrency(),
                                        "amount", command.getAmountMinor(),
                                        "name", "Connectly Order #" + command.getOrderId(),
                                        "quantity", 1
                                )),
                                "payment_method_types", List.of("card", "gcash", "grab_pay", "paymaya"),
                                "success_url", command.getSuccessUrl(),
                                "cancel_url", command.getCancelUrl(),
                                "description", command.getOrderReference()
                        )
                )
        );

        try {
            JsonNode response = restClient.post()
                    .uri(config.getBaseUrl() + "/v2/checkout_sessions")
                    .header(HttpHeaders.AUTHORIZATION, basicAuth(config.getSecretKey()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            String checkoutId = ProviderJson.textAt(response, "/data/id");
            String checkoutUrl = ProviderJson.textAt(response, "/data/attributes/checkout_url");

            return CreateCheckoutResult.builder()
                    .providerCheckoutId(checkoutId)
                    .checkoutUrl(checkoutUrl)
                    .status(PaymentStatus.PENDING)
                    .build();
        } catch (Exception ex) {
            throw new PaymentGatewayException("PayMongo checkout creation failed", ex);
        }
    }

    @Override
    public ProviderPaymentStatus retrievePayment(String providerPaymentId) {
        PaymentProperties.Paymongo config = paymentProperties.getPaymongo();
        ensureConfigured(config);

        try {
            JsonNode response = restClient.get()
                    .uri(config.getBaseUrl() + "/v1/payments/" + providerPaymentId)
                    .header(HttpHeaders.AUTHORIZATION, basicAuth(config.getSecretKey()))
                    .retrieve()
                    .body(JsonNode.class);

            String status = ProviderJson.textAt(response, "/data/attributes/status");
            return ProviderPaymentStatus.builder()
                    .providerPaymentId(providerPaymentId)
                    .status(mapPaymentStatus(status))
                    .build();
        } catch (Exception ex) {
            throw new PaymentGatewayException("PayMongo payment retrieval failed", ex);
        }
    }

    @Override
    public WebhookVerificationResult verifyWebhook(String rawPayload, Map<String, String> headers) {
        String webhookSecret = paymentProperties.getPaymongo().getWebhookSecret();
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return WebhookVerificationResult.invalid("PayMongo webhook secret is not configured");
        }

        String signature = header(headers, "paymongo-signature");
        if (signature == null || signature.isBlank()) {
            return WebhookVerificationResult.invalid("Missing PayMongo signature");
        }

        String expectedSignature = hmacSha256Hex(webhookSecret, rawPayload);
        boolean valid = signature.contains(expectedSignature) || signature.equals(expectedSignature);
        return valid ? WebhookVerificationResult.valid() : WebhookVerificationResult.invalid("Invalid PayMongo signature");
    }

    @Override
    public ProviderWebhookEvent parseWebhook(String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String eventId = ProviderJson.textAt(root, "/data/id");
            String eventType = ProviderJson.textAt(root, "/data/attributes/type");
            JsonNode resource = root.at("/data/attributes/data");

            String providerPaymentId = ProviderJson.textAt(resource, "/id");
            String providerCheckoutId = ProviderJson.textAt(resource, "/attributes/checkout_session_id");
            String failureMessage = ProviderJson.textAt(resource, "/attributes/failed_message");

            return ProviderWebhookEvent.builder()
                    .providerEventId(eventId)
                    .eventType(eventType)
                    .providerPaymentId(providerPaymentId)
                    .providerCheckoutId(providerCheckoutId)
                    .paymentStatus(mapEventStatus(eventType))
                    .failureMessage(failureMessage)
                    .build();
        } catch (Exception ex) {
            throw new PaymentGatewayException("Unable to parse PayMongo webhook", ex);
        }
    }

    private void ensureConfigured(PaymentProperties.Paymongo config) {
        if (!config.isEnabled()) {
            throw new PaymentGatewayException("PayMongo gateway is disabled");
        }
        if (config.getSecretKey() == null || config.getSecretKey().isBlank()) {
            throw new PaymentGatewayException("PayMongo secret key is not configured");
        }
    }

    private String basicAuth(String secretKey) {
        return "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }

    private PaymentStatus mapEventStatus(String eventType) {
        if ("payment.paid".equalsIgnoreCase(eventType)) {
            return PaymentStatus.PAID;
        }
        if ("payment.failed".equalsIgnoreCase(eventType)) {
            return PaymentStatus.FAILED;
        }
        if ("refund.updated".equalsIgnoreCase(eventType) || "payment.refunded".equalsIgnoreCase(eventType)) {
            return PaymentStatus.REFUNDED;
        }
        return PaymentStatus.PROCESSING;
    }

    private PaymentStatus mapPaymentStatus(String providerStatus) {
        if ("paid".equalsIgnoreCase(providerStatus) || "succeeded".equalsIgnoreCase(providerStatus)) {
            return PaymentStatus.PAID;
        }
        if ("failed".equalsIgnoreCase(providerStatus)) {
            return PaymentStatus.FAILED;
        }
        if ("processing".equalsIgnoreCase(providerStatus)) {
            return PaymentStatus.PROCESSING;
        }
        return PaymentStatus.PENDING;
    }

    private String header(Map<String, String> headers, String key) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new PaymentGatewayException("Unable to verify PayMongo webhook signature", ex);
        }
    }
}
