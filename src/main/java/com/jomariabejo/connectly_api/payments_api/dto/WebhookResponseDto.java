package com.jomariabejo.connectly_api.payments_api.dto;

import com.jomariabejo.connectly_api.payments_api.entity.PaymentEventProcessingStatus;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookResponseDto {
    private PaymentProvider provider;
    private String providerEventId;
    private String eventType;
    private PaymentEventProcessingStatus processingStatus;
}
