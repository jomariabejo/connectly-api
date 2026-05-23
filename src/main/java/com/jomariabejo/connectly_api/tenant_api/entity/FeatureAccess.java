package com.jomariabejo.connectly_api.tenant_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Feature access control table for tier-based feature flags
 * Determines which features are available for each pricing tier
 */
@Entity
@Table(name = "feature_access", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"pricing_tier", "feature_code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String pricingTier;

    @Column(nullable = false, length = 100)
    private String featureCode;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // Common feature codes
    public static final String BASIC_INVENTORY = "BASIC_INVENTORY";
    public static final String BASIC_SALES = "BASIC_SALES";
    public static final String BASIC_REPORTS = "BASIC_REPORTS";
    public static final String EMAIL_SUPPORT = "EMAIL_SUPPORT";
    public static final String CREDIT_TRACKING = "CREDIT_TRACKING";
    public static final String ADVANCED_ANALYTICS = "ADVANCED_ANALYTICS";
    public static final String API_ACCESS = "API_ACCESS";
    public static final String CUSTOM_INTEGRATIONS = "CUSTOM_INTEGRATIONS";
    public static final String SMS_NOTIFICATIONS = "SMS_NOTIFICATIONS";
    public static final String MULTI_LOCATION = "MULTI_LOCATION";
    public static final String ADVANCED_REPORTS = "ADVANCED_REPORTS";
    public static final String PRIORITY_SUPPORT = "PRIORITY_SUPPORT";
    public static final String WHITE_LABEL = "WHITE_LABEL";
    public static final String DATA_EXPORT = "DATA_EXPORT";
}
