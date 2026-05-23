package com.jomariabejo.connectly_api.tenant_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String tier;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal pricePhp;

    @Column(nullable = false)
    private BigDecimal priceUsd;

    @Column(name = "max_users")
    private Integer maxUsers; // -1 means unlimited

    @Column(name = "max_locations")
    private Integer maxLocations; // -1 means unlimited

    @Column(name = "max_storage_gb")
    private Integer maxStorageGb; // -1 means unlimited

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String billingCycle = "MONTHLY";

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
