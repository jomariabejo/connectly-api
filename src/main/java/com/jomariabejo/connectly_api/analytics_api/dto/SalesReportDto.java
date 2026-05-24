package com.jomariabejo.connectly_api.analytics_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportDto {
    private LocalDate analyticsDate;
    private String periodType;
    private BigDecimal totalSales;
    private BigDecimal totalUnitsSold;
    private Integer totalTransactions;
    private BigDecimal avgTransactionValue;
    private Long topProductId;
    private String topProductName;
    private Long topCategoryId;
    private String topCategoryName;
}
