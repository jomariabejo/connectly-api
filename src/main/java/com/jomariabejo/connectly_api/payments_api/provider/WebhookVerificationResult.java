package com.jomariabejo.connectly_api.payments_api.provider;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WebhookVerificationResult {
    boolean valid;
    String reason;

    public static WebhookVerificationResult valid() {
        return WebhookVerificationResult.builder().valid(true).build();
    }

    public static WebhookVerificationResult invalid(String reason) {
        return WebhookVerificationResult.builder().valid(false).reason(reason).build();
    }
}
