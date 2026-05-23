package com.jomariabejo.connectly_api.crm_api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerLedgerDto {
    private Long id;
    private Long customerId;
    private BigDecimal creditLimit;
    private BigDecimal availableCredit;
    private BigDecimal totalOutstanding;
    private String status;
    private LocalDateTime creditLineApprovedAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
