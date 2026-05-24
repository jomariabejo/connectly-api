package com.jomariabejo.connectly_api.supplier_api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayablesLedgerDto {
    private Long id;
    private Long supplierId;
    private String supplierName;
    private Long invoiceId;
    private String invoiceNumber;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal balance;
    private LocalDateTime transactionDate;
    private LocalDateTime createdAt;
}
