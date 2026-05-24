package com.jomariabejo.connectly_api.analytics_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfitEstimationDto {
    private LocalDate estimationDate;
    private LocalDate forecastStartDate;
    private LocalDate forecastEndDate;
    private String forecastPeriod;
    private BigDecimal estimatedTotalRevenue;
    private BigDecimal estimatedTotalCost;
    private BigDecimal estimatedGrossProfit;
    private BigDecimal estimatedProfitMarginPercent;
    private String confidenceLevel;
    private String methodology;
    private Integer basedOnHistoricalPeriods;
    private String notes;
}
