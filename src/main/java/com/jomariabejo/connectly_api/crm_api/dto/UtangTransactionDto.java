package com.jomariabejo.connectly_api.crm_api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtangTransactionDto {
    private Long id;
    private Long customerId;
    private BigDecimal amount;
    private String transactionType;
    private String referenceType;
    private Long referenceId;
    private LocalDateTime dueDate;
    private String notes;
    private LocalDateTime createdAt;
}
