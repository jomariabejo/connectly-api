package com.jomariabejo.connectly_api.analytics_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfitReportDto {
    private LocalDate analyticsDate;
    private String periodType;
    private Long productId;
    private String productName;
    private Long categoryId;
    private String categoryName;
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal grossProfit;
    private BigDecimal profitMarginPercent;
    private BigDecimal unitsSold;
    private BigDecimal avgCostPerUnit;
    private BigDecimal avgSellingPrice;
}
