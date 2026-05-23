package com.jomariabejo.connectly_api.crm_api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtangPaymentDto {
    private Long id;
    private Long customerId;
    private BigDecimal amount;
    private String paymentMethod;
    private String referenceNumber;
    private LocalDateTime paymentDate;
    private String notes;
    private LocalDateTime createdAt;
}
