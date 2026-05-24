package com.jomariabejo.connectly_api.inventory_api.entity;

import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "variant_pricing", uniqueConstraints = {
        @UniqueConstraint(name = "uk_variant_pricing_variant_qty", columnNames = {"variant_id", "quantity_threshold"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantPricing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "quantity_threshold", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityThreshold; // minimum quantity for this price

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "price_type", nullable = false, length = 20)
    @Builder.Default
    private String priceType = "RETAIL"; // RETAIL, WHOLESALE, BULK

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
