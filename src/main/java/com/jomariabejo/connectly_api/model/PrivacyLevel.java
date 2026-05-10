package com.jomariabejo.connectly_api.model;

public enum PrivacyLevel {
    PUBLIC("public"),
    FOLLOWERS_ONLY("followers_only"),
    PRIVATE("private");

    private final String value;

    PrivacyLevel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PrivacyLevel fromString(String value) {
        if (value == null) {
            return PUBLIC;
        }
        for (PrivacyLevel level : PrivacyLevel.values()) {
            if (level.value.equalsIgnoreCase(value)) {
                return level;
            }
        }
        return PUBLIC; // Default fallback
    }
}
