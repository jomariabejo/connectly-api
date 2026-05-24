package com.jomariabejo.connectly_api.crm_api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUtangTransactionRequest {
    private Long customerId;
    private BigDecimal amount;
    private String transactionType; // DEBIT, CREDIT, ADJUSTMENT, PENALTY
    private String referenceType;
    private Long referenceId;
    private Long orderId;
    private LocalDateTime dueDate;
    private String notes;
}
