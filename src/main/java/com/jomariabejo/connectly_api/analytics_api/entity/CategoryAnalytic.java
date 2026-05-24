package com.jomariabejo.connectly_api.analytics_api.entity;

import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "category_analytics", indexes = {
    @Index(name = "idx_category_analytics_tenant_date", columnList = "tenant_id, analytics_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryAnalytic {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    
    @Column(nullable = false)
    private Long categoryId;
    
    @Column(nullable = false)
    private LocalDate analyticsDate;
    
    @Column(nullable = false)
    private String periodType; // DAILY, WEEKLY, MONTHLY, YEARLY
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private BigDecimal totalSales = BigDecimal.ZERO;
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private BigDecimal totalUnitsSold = BigDecimal.ZERO;
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private Integer totalTransactions = 0;
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private BigDecimal grossProfit = BigDecimal.ZERO;
    
    private BigDecimal profitMarginPercent;
    
    private BigDecimal avgProductPrice;
    
    private Integer productCount;
    
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
