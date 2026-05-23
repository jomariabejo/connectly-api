package com.jomariabejo.connectly_api.tenant_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(unique = true, length = 100)
    private String subdomain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TenantStatus status = TenantStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", length = 50)
    @Builder.Default
    private BusinessType businessType = BusinessType.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_tier", length = 50)
    @Builder.Default
    private PricingTier pricingTier = PricingTier.STARTER;

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "subscription_active")
    @Builder.Default
    private Boolean subscriptionActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToOne(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private TenantSettings settings;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TenantSubscription> subscriptions = new HashSet<>();

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TenantUser> members = new HashSet<>();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if tenant is on trial period
     */
    public boolean isOnTrial() {
        return trialEndsAt != null && LocalDateTime.now().isBefore(trialEndsAt);
    }

    /**
     * Check if tenant subscription is active and valid
     */
    public boolean isSubscriptionValid() {
        return subscriptionActive && (trialEndsAt == null || LocalDateTime.now().isBefore(trialEndsAt));
    }
}
