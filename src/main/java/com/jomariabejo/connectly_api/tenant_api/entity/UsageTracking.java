package com.jomariabejo.connectly_api.tenant_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tracks resource usage by tenant per month
 * Used for enforcing quotas and monitoring consumption
 */
@Entity
@Table(name = "usage_tracking", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "tracking_month"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageTracking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private LocalDate trackingMonth;

    @Column(name = "api_calls_used")
    @Builder.Default
    private Long apiCallsUsed = 0L;

    @Column(name = "storage_used_gb")
    @Builder.Default
    private Long storageUsedGb = 0L;

    @Column(name = "sms_sent")
    @Builder.Default
    private Integer smsSent = 0;

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
