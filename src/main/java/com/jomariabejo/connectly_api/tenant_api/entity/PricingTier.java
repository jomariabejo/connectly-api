package com.jomariabejo.connectly_api.tenant_api.entity;

import java.math.BigDecimal;

/**
 * Pricing tiers aligned with Philippine market (PH-focused pricing)
 */
public enum PricingTier {
    STARTER("Starter", 499, 9),           // ₱499/month ≈ $9/month
    PROFESSIONAL("Professional", 1499, 27), // ₱1,499/month ≈ $27/month
    BUSINESS("Business", 3999, 72),       // ₱3,999/month ≈ $72/month
    ENTERPRISE("Enterprise", 0, 0);       // Custom pricing

    private final String displayName;
    private final int pricePhp;
    private final int priceUsd;

    PricingTier(String displayName, int pricePhp, int priceUsd) {
        this.displayName = displayName;
        this.pricePhp = pricePhp;
        this.priceUsd = priceUsd;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPricePhp() {
        return pricePhp;
    }

    public int getPriceUsd() {
        return priceUsd;
    }

    public boolean isUnlimitedLocations() {
        return this == PROFESSIONAL || this == BUSINESS || this == ENTERPRISE;
    }

    public boolean isUnlimitedUsers() {
        return this == PROFESSIONAL || this == BUSINESS || this == ENTERPRISE;
    }

    public int getMaxUsers() {
        return switch (this) {
            case STARTER -> 5;
            case PROFESSIONAL, BUSINESS, ENTERPRISE -> -1; // unlimited
        };
    }

    public int getMaxLocations() {
        return switch (this) {
            case STARTER -> 1;
            case PROFESSIONAL -> 3;
            case BUSINESS, ENTERPRISE -> -1; // unlimited
        };
    }

    public int getMaxStorageGb() {
        return switch (this) {
            case STARTER -> 1;
            case PROFESSIONAL -> 10;
            case BUSINESS -> 50;
            case ENTERPRISE -> -1; // unlimited
        };
    }
}
