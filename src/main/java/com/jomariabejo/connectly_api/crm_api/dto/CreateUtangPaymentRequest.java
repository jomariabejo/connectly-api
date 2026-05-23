package com.jomariabejo.connectly_api.crm_api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUtangPaymentRequest {
    private Long customerId;
    private BigDecimal amount;
    private String paymentMethod; // CASH, CHEQUE, TRANSFER, GCASH, INSTALLMENT
    private String referenceNumber;
    private LocalDateTime paymentDate;
    private String notes;
}
