package com.jomariabejo.connectly_api.payments_api.provider;

import com.jomariabejo.connectly_api.payments_api.entity.PaymentProvider;
import com.jomariabejo.connectly_api.payments_api.exception.PaymentGatewayNotFoundException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentGatewayRegistry {
    private final Map<PaymentProvider, PaymentGateway> gateways;

    public PaymentGatewayRegistry(List<PaymentGateway> gateways) {
        this.gateways = new EnumMap<>(PaymentProvider.class);
        for (PaymentGateway gateway : gateways) {
            this.gateways.put(gateway.provider(), gateway);
        }
    }

    public PaymentGateway getGateway(PaymentProvider provider) {
        PaymentGateway gateway = gateways.get(provider);
        if (gateway == null) {
            throw new PaymentGatewayNotFoundException("No payment gateway configured for provider " + provider);
        }
        return gateway;
    }
}
