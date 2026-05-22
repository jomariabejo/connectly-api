package com.jomariabejo.connectly_api.tenant_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tenant_subscriptions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "product_code"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_code", nullable = false, length = 50)
    private ProductCode productCode;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "subscribed_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime subscribedAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
