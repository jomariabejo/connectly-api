package com.jomariabejo.connectly_api.supplier_api.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierAgingReportDto {
    private Long supplierId;
    private String supplierName;
    private BigDecimal currentAmount;
    private BigDecimal thirtyDaysAmount;
    private BigDecimal sixtyDaysAmount;
    private BigDecimal ninetyDaysAmount;
    private BigDecimal totalAmount;
}
