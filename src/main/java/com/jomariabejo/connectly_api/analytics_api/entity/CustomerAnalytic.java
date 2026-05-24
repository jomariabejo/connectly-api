package com.jomariabejo.connectly_api.analytics_api.entity;

import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_analytics", indexes = {
    @Index(name = "idx_customer_analytics_tenant_customer_date", columnList = "tenant_id, customer_id, analytics_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAnalytic {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    
    @Column(nullable = false)
    private Long customerId;
    
    @Column(nullable = false)
    private LocalDate analyticsDate;
    
    @Column(nullable = false)
    private String periodType; // DAILY, WEEKLY, MONTHLY, YEARLY
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private BigDecimal totalPurchases = BigDecimal.ZERO;
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private Integer purchaseCount = 0;
    
    private BigDecimal avgPurchaseValue;
    
    private LocalDate lastPurchaseDate;
    
    private Integer daysSinceLastPurchase;
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private BigDecimal totalUnitsPurchased = BigDecimal.ZERO;
    
    private Long preferredCategoryId;
    
    private BigDecimal customerLifetimeValue;
    
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
