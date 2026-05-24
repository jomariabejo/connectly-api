package com.jomariabejo.connectly_api.analytics_api.entity;

import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_metrics", indexes = {
    @Index(name = "idx_inventory_metrics_tenant_product_date", columnList = "tenant_id, product_id, analytics_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMetric {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    
    @Column(nullable = false)
    private Long productId;
    
    @Column(nullable = false)
    private LocalDate analyticsDate;
    
    @Column(nullable = false)
    private String periodType; // DAILY, WEEKLY, MONTHLY, YEARLY
    
    @Column(nullable = false)
    private BigDecimal beginningQuantity;
    
    @Column(nullable = false)
    private BigDecimal endingQuantity;
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private BigDecimal unitsSold = BigDecimal.ZERO;
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private BigDecimal unitsReceived = BigDecimal.ZERO;
    
    private BigDecimal avgInventory;
    
    private BigDecimal inventoryTurnoverRatio;
    
    private Integer daysInventoryOutstanding;
    
    @Column(nullable = false)
    @lombok.Builder.Default
    private Integer stockOutIncidents = 0;
    
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
