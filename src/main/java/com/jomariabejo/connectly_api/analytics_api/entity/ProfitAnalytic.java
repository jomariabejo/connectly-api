package com.jomariabejo.connectly_api.analytics_api.entity;

import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "profit_analytics", indexes = {
    @Index(name = "idx_profit_analytics_tenant_product_date", columnList = "tenant_id, product_id, analytics_date"),
    @Index(name = "idx_profit_analytics_period", columnList = "tenant_id, period_type, analytics_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfitAnalytic {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    
    private Long productId;
    
    private Long categoryId;
    
    @Column(nullable = false)
    private LocalDate analyticsDate;
    
    @Column(nullable = false)
    private String periodType; // DAILY, WEEKLY, MONTHLY, YEARLY
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private BigDecimal totalCost = BigDecimal.ZERO;
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private BigDecimal grossProfit = BigDecimal.ZERO;
    
    private BigDecimal profitMarginPercent; // 0-100
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private BigDecimal unitsSold = BigDecimal.ZERO;
    
    private BigDecimal avgCostPerUnit;
    
    private BigDecimal avgSellingPrice;
    
    private Long auditUserId;
    
    @Column(nullable = false, updatable = false)
    @lombok.Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
