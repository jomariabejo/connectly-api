package com.jomariabejo.connectly_api.analytics_api.entity;

import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "profit_estimations", indexes = {
    @Index(name = "idx_profit_estimations_tenant_forecast", columnList = "tenant_id, forecast_start_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfitEstimation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    
    @Column(nullable = false)
    private LocalDate estimationDate;
    
    @Column(nullable = false)
    private LocalDate forecastStartDate;
    
    @Column(nullable = false)
    private LocalDate forecastEndDate;
    
    @Column(nullable = false)
    private String forecastPeriod; // WEEKLY, MONTHLY, QUARTERLY, YEARLY
    
    @Column(nullable = false)
    private BigDecimal estimatedTotalRevenue;
    
    @Column(nullable = false)
    private BigDecimal estimatedTotalCost;
    
    @Column(nullable = false)
    private BigDecimal estimatedGrossProfit;
    
    private BigDecimal estimatedProfitMarginPercent;
    
    private String confidenceLevel; // LOW, MEDIUM, HIGH
    
    private String methodology; // LINEAR_TREND, SEASONAL_ADJUSTED, MOVING_AVERAGE
    
    private Integer basedOnHistoricalPeriods; // Number of past periods used for forecast
    
    private String notes;
    
    private Long auditUserId;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
