package com.jomariabejo.connectly_api.tenant_api.entity;

/**
 * Business types supported by the platform targeting Philippine SME market
 */
public enum BusinessType {
    SARI_SARI_STORE("Sari-Sari Store"),
    VULCANIZING_SHOP("Vulcanizing Shop"),
    CATERING_BUSINESS("Catering Business"),
    GENERAL("General Business");

    private final String displayName;

    BusinessType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
