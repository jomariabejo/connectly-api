package com.jomariabejo.connectly_api.analytics_api.entity;

import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "dashboard_metrics", indexes = {
    @Index(name = "idx_dashboard_metrics_tenant_date", columnList = "tenant_id, metric_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardMetric {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    
    @Column(nullable = false)
    private LocalDate metricDate;
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private BigDecimal totalSalesMtd = BigDecimal.ZERO; // Month to date
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private BigDecimal totalSalesYtd = BigDecimal.ZERO; // Year to date
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private BigDecimal grossProfitMtd = BigDecimal.ZERO;
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private BigDecimal grossProfitYtd = BigDecimal.ZERO;
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private Long totalOrders = 0L;
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private Integer totalCustomers = 0;
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private BigDecimal inventoryValue = BigDecimal.ZERO;
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private Integer lowStockCount = 0;
    
    private Long topSellingProductId;
    
    private BigDecimal avgProfitMarginPercent;
    
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
