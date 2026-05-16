package com.jomariabejo.connectly_api.payments_api.provider.paypal;

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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class PaypalPaymentGateway implements PaymentGateway {
    private final PaymentProperties paymentProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PaypalPaymentGateway(PaymentProperties paymentProperties, ObjectMapper objectMapper) {
        this.paymentProperties = paymentProperties;
        this.restClient = RestClient.builder().build();
        this.objectMapper = objectMapper;
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.PAYPAL;
    }

    @Override
    public CreateCheckoutResult createCheckout(CreateCheckoutCommand command) {
        PaymentProperties.Paypal config = paymentProperties.getPaypal();
        ensureConfigured(config);

        Map<String, Object> payload = Map.of(
                "intent", "CAPTURE",
                "purchase_units", List.of(Map.of(
                        "reference_id", command.getOrderReference(),
                        "custom_id", String.valueOf(command.getPaymentId()),
                        "invoice_id", "CONNECTLY-" + command.getOrderId() + "-" + command.getPaymentId(),
                        "amount", Map.of(
                                "currency_code", command.getCurrency(),
                                "value", formatMajorAmount(command.getAmountMinor())
                        )
                )),
                "application_context", Map.of(
                        "return_url", command.getSuccessUrl(),
                        "cancel_url", command.getCancelUrl(),
                        "user_action", "PAY_NOW"
                )
        );

        try {
            JsonNode response = restClient.post()
                    .uri(config.getBaseUrl() + "/v2/checkout/orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(config))
                    .header("PayPal-Request-Id", command.getIdempotencyKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            String paypalOrderId = ProviderJson.textAt(response, "/id");
            String approveUrl = approveUrl(response);

            return CreateCheckoutResult.builder()
                    .providerCheckoutId(paypalOrderId)
                    .providerPaymentId(paypalOrderId)
                    .checkoutUrl(approveUrl)
                    .status(PaymentStatus.PENDING)
                    .build();
        } catch (Exception ex) {
            throw new PaymentGatewayException("PayPal order creation failed", ex);
        }
    }

    @Override
    public ProviderPaymentStatus retrievePayment(String providerPaymentId) {
        PaymentProperties.Paypal config = paymentProperties.getPaypal();
        ensureConfigured(config);

        try {
            JsonNode response = restClient.get()
                    .uri(config.getBaseUrl() + "/v2/checkout/orders/" + providerPaymentId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(config))
                    .retrieve()
                    .body(JsonNode.class);

            return ProviderPaymentStatus.builder()
                    .providerPaymentId(providerPaymentId)
                    .providerCheckoutId(providerPaymentId)
                    .status(mapOrderStatus(ProviderJson.textAt(response, "/status")))
                    .build();
        } catch (Exception ex) {
            throw new PaymentGatewayException("PayPal order retrieval failed", ex);
        }
    }

    @Override
    public WebhookVerificationResult verifyWebhook(String rawPayload, Map<String, String> headers) {
        PaymentProperties.Paypal config = paymentProperties.getPaypal();
        if (!config.isEnabled()) {
            return WebhookVerificationResult.invalid("PayPal gateway is disabled");
        }
        if (config.getWebhookId() == null || config.getWebhookId().isBlank()) {
            return WebhookVerificationResult.invalid("PayPal webhook ID is not configured");
        }

        String transmissionId = header(headers, "paypal-transmission-id");
        String transmissionTime = header(headers, "paypal-transmission-time");
        String certUrl = header(headers, "paypal-cert-url");
        String authAlgo = header(headers, "paypal-auth-algo");
        String transmissionSig = header(headers, "paypal-transmission-sig");

        if (transmissionId == null || transmissionTime == null || certUrl == null || authAlgo == null || transmissionSig == null) {
            return WebhookVerificationResult.invalid("Missing PayPal verification headers");
        }

        try {
            Map<String, Object> payload = Map.of(
                    "auth_algo", authAlgo,
                    "cert_url", certUrl,
                    "transmission_id", transmissionId,
                    "transmission_sig", transmissionSig,
                    "transmission_time", transmissionTime,
                    "webhook_id", config.getWebhookId(),
                    "webhook_event", objectMapper.readValue(rawPayload, Map.class)
            );

            JsonNode response = restClient.post()
                    .uri(config.getBaseUrl() + "/v1/notifications/verify-webhook-signature")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(config))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            boolean verified = "SUCCESS".equalsIgnoreCase(ProviderJson.textAt(response, "/verification_status"));
            return verified ? WebhookVerificationResult.valid() : WebhookVerificationResult.invalid("PayPal rejected webhook signature");
        } catch (Exception ex) {
            throw new PaymentGatewayException("PayPal webhook verification failed", ex);
        }
    }

    @Override
    public ProviderWebhookEvent parseWebhook(String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String eventId = ProviderJson.textAt(root, "/id");
            String eventType = ProviderJson.textAt(root, "/event_type");
            JsonNode resource = root.at("/resource");

            String providerPaymentId = ProviderJson.textAt(resource, "/id");
            String checkoutId = ProviderJson.textAt(resource, "/supplementary_data/related_ids/order_id");
            if (checkoutId == null) {
                checkoutId = providerPaymentId;
            }

            return ProviderWebhookEvent.builder()
                    .providerEventId(eventId)
                    .eventType(eventType)
                    .providerPaymentId(providerPaymentId)
                    .providerCheckoutId(checkoutId)
                    .paymentStatus(mapEventStatus(eventType))
                    .build();
        } catch (Exception ex) {
            throw new PaymentGatewayException("Unable to parse PayPal webhook", ex);
        }
    }

    private void ensureConfigured(PaymentProperties.Paypal config) {
        if (!config.isEnabled()) {
            throw new PaymentGatewayException("PayPal gateway is disabled");
        }
        if (config.getClientId() == null || config.getClientId().isBlank() ||
                config.getClientSecret() == null || config.getClientSecret().isBlank()) {
            throw new PaymentGatewayException("PayPal client credentials are not configured");
        }
    }

    private String accessToken(PaymentProperties.Paypal config) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");

        JsonNode response = restClient.post()
                .uri(config.getBaseUrl() + "/v1/oauth2/token")
                .header(HttpHeaders.AUTHORIZATION, basicAuth(config.getClientId(), config.getClientSecret()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        String token = ProviderJson.textAt(response, "/access_token");
        if (token == null || token.isBlank()) {
            throw new PaymentGatewayException("PayPal did not return an access token");
        }
        return token;
    }

    private String approveUrl(JsonNode response) {
        JsonNode links = response.path("links");
        if (links.isArray()) {
            for (JsonNode link : links) {
                if ("approve".equalsIgnoreCase(link.path("rel").asText())) {
                    return link.path("href").asText(null);
                }
            }
        }
        return null;
    }

    private String basicAuth(String clientId, String clientSecret) {
        String credentials = clientId + ":" + clientSecret;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private String formatMajorAmount(Long amountMinor) {
        return String.format("%.2f", amountMinor / 100.0);
    }

    private PaymentStatus mapOrderStatus(String status) {
        if ("COMPLETED".equalsIgnoreCase(status)) {
            return PaymentStatus.PAID;
        }
        if ("APPROVED".equalsIgnoreCase(status) || "PAYER_ACTION_REQUIRED".equalsIgnoreCase(status)) {
            return PaymentStatus.REQUIRES_ACTION;
        }
        if ("VOIDED".equalsIgnoreCase(status)) {
            return PaymentStatus.CANCELLED;
        }
        return PaymentStatus.PENDING;
    }

    private PaymentStatus mapEventStatus(String eventType) {
        if ("PAYMENT.CAPTURE.COMPLETED".equalsIgnoreCase(eventType) ||
                "CHECKOUT.ORDER.COMPLETED".equalsIgnoreCase(eventType)) {
            return PaymentStatus.PAID;
        }
        if ("PAYMENT.CAPTURE.DENIED".equalsIgnoreCase(eventType) ||
                "PAYMENT.CAPTURE.DECLINED".equalsIgnoreCase(eventType)) {
            return PaymentStatus.FAILED;
        }
        if ("PAYMENT.CAPTURE.REFUNDED".equalsIgnoreCase(eventType)) {
            return PaymentStatus.REFUNDED;
        }
        return PaymentStatus.PROCESSING;
    }

    private String header(Map<String, String> headers, String key) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
