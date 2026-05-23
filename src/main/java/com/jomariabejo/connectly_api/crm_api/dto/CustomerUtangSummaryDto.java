package com.jomariabejo.connectly_api.crm_api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerUtangSummaryDto {
    private Long customerId;
    private BigDecimal creditLimit;
    private BigDecimal availableCredit;
    private BigDecimal totalOutstanding;
    private String creditStatus;
    private CustomerCreditScoreDto creditScore;
    private List<UtangTransactionDto> recentTransactions;
    private List<UtangPaymentDto> recentPayments;
    private Integer overdueDays;
    private BigDecimal overdueAmount;
}
