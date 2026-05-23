package com.jomariabejo.connectly_api.tenant_api.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility for extracting and validating subdomains from HTTP requests
 * Supports subdomain-based multi-tenancy pattern: tenant.platform.com
 */
public class SubdomainExtractor {
    private static final String LOCALHOST = "localhost";

    /**
     * Extract subdomain from HTTP request
     * Handles formats like:
     * - tenant.yourplatform.ph
     * - tenant.localhost:8080
     * - localhost:8080 (returns null)
     */
    public static String extractSubdomain(HttpServletRequest request) {
        try {
            String host = request.getHeader("Host");
            if (host == null || host.isEmpty()) {
                return null;
            }

            // Remove port if present
            String hostname = host.split(":")[0];

            // Check if it's localhost - if so, subdomain may come from header
            if (hostname.equals(LOCALHOST)) {
                return request.getHeader("X-Subdomain");
            }

            // Split by dots
            String[] parts = hostname.split("\\.");
            if (parts.length < 2) {
                return null;
            }

            // The subdomain is the first part
            String subdomain = parts[0];

            // Validate it's not the main domain
            if (!isMainDomain(subdomain)) {
                return subdomain;
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if the given part is a main domain (like www, api, etc.)
     */
    private static boolean isMainDomain(String part) {
        return part.equalsIgnoreCase("www")
                || part.equalsIgnoreCase("api")
                || part.equalsIgnoreCase("admin")
                || part.equalsIgnoreCase("mail");
    }

    /**
     * Validate subdomain format
     * - Must be 3-50 characters
     * - Must contain only lowercase letters, numbers, and hyphens
     * - Must start with a letter
     * - Must not start or end with hyphen
     */
    public static boolean isValidSubdomain(String subdomain) {
        if (subdomain == null || subdomain.isEmpty()) {
            return false;
        }

        if (subdomain.length() < 3 || subdomain.length() > 50) {
            return false;
        }

        // Pattern: start with lowercase letter, followed by lowercase letters/numbers/hyphens, end with lowercase letter/number
        if (!subdomain.matches("^[a-z]([a-z0-9-]*[a-z0-9])?$")) {
            return false;
        }

        return true;
    }

    /**
     * Normalize subdomain (convert to lowercase, trim spaces)
     */
    public static String normalizeSubdomain(String subdomain) {
        if (subdomain == null) {
            return null;
        }
        return subdomain.trim().toLowerCase();
    }

    /**
     * Build full domain URL from subdomain
     */
    public static String buildTenantUrl(String subdomain, String baseDomain) {
        return String.format("https://%s.%s", subdomain, baseDomain);
    }
}
