package com.jomariabejo.connectly_api.analytics_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsDto {
    private LocalDate metricDate;
    private BigDecimal totalSalesMtd;
    private BigDecimal totalSalesYtd;
    private BigDecimal grossProfitMtd;
    private BigDecimal grossProfitYtd;
    private BigDecimal profitMarginMtdPercent;
    private Long totalOrders;
    private Integer totalCustomers;
    private BigDecimal inventoryValue;
    private Integer lowStockCount;
    private Long topSellingProductId;
    private String topSellingProductName;
    private BigDecimal avgProfitMarginPercent;
}
