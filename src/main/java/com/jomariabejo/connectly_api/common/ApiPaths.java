package com.jomariabejo.connectly_api.common;

/**
 * Central API path prefixes. All REST controllers should live under {@link #V1}.
 */
public final class ApiPaths {

    public static final String V1 = "/v1";
    public static final String V1_AUTH = V1 + "/auth";
    public static final String V1_PUBLIC = V1 + "/public";
    public static final String V1_TEST = V1 + "/test";
    public static final String V1_ADMIN = V1 + "/admin";
    public static final String V1_USER = V1 + "/user";
    public static final String V1_PAYMENTS_WEBHOOKS = V1 + "/payments/webhooks";

    private ApiPaths() {
    }
}
