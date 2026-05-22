package com.jomariabejo.connectly_api.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Builds absolute API URLs for emails and external links.
 * Base URL must include the servlet context path (e.g. {@code http://localhost:8080/api}).
 */
@Component
public class ApiUrlBuilder {

    private final String publicBaseUrl;

    public ApiUrlBuilder(
            @Value("${app.api.public-base-url:http://localhost:8080/api}") String publicBaseUrl
    ) {
        this.publicBaseUrl = normalizeBaseUrl(publicBaseUrl);
    }

    public String authVerifyUrl(String token) {
        return uriBuilder()
                .path(ApiPaths.V1_AUTH + "/verify")
                .queryParam("token", token)
                .build()
                .toUriString();
    }

    public String authRegistrationConfirmUrl(String token) {
        return uriBuilder()
                .path(ApiPaths.V1_AUTH + "/registrationConfirm")
                .queryParam("token", token)
                .build()
                .toUriString();
    }

    public String authResetPasswordUrl(String token) {
        return uriBuilder()
                .path(ApiPaths.V1_AUTH + "/reset-password")
                .queryParam("token", token)
                .build()
                .toUriString();
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    private UriComponentsBuilder uriBuilder() {
        return UriComponentsBuilder.fromUriString(publicBaseUrl);
    }

    static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8080/api";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
