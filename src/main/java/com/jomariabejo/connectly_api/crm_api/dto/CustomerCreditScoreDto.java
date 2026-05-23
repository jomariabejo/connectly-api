package com.jomariabejo.connectly_api.crm_api.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerCreditScoreDto {
    private Long customerId;
    private BigDecimal score;
    private Integer onTimePayments;
    private Integer latePayments;
    private Integer missedPayments;
    private Integer totalTransactions;
    private BigDecimal paymentPercentage;
    private String riskCategory;
}
