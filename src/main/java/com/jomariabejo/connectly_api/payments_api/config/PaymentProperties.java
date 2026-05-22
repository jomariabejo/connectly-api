package com.jomariabejo.connectly_api.payments_api.config;

import com.jomariabejo.connectly_api.payments_api.entity.PaymentProvider;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "payments")
public class PaymentProperties {
    private PaymentProvider defaultProvider = PaymentProvider.PAYMONGO;
    private Paymongo paymongo = new Paymongo();
    private Paypal paypal = new Paypal();

    @Getter
    @Setter
    public static class Paymongo {
        private boolean enabled = true;
        private String secretKey = "";
        private String webhookSecret = "";
        private String baseUrl = "https://api.paymongo.com";
    }

    @Getter
    @Setter
    public static class Paypal {
        private boolean enabled = true;
        private String clientId = "";
        private String clientSecret = "";
        private String webhookId = "";
        private String baseUrl = "https://api-m.sandbox.paypal.com";
    }
}
