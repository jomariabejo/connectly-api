package com.jomariabejo.connectly_api.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class FrontendUrlBuilder {

    private final String frontendBaseUrl;

    public FrontendUrlBuilder(
            @Value("${app.frontend.public-base-url:http://localhost:3000}") String frontendBaseUrl
    ) {
        this.frontendBaseUrl = normalizeBaseUrl(frontendBaseUrl);
    }

    public String inviteRegisterUrl(String token) {
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/register/invite")
                .queryParam("token", token)
                .build()
                .toUriString();
    }

    public String customerRegisterUrl(String tenantSlug) {
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/register/customer/" + tenantSlug)
                .build()
                .toUriString();
    }

    public String verifyEmailUrl(String token) {
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/verify-email")
                .queryParam("token", token)
                .build()
                .toUriString();
    }

    public String resetPasswordUrl(String token) {
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/reset-password")
                .queryParam("token", token)
                .build()
                .toUriString();
    }

    public String checkEmailUrl() {
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/check-email")
                .build()
                .toUriString();
    }

    static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:3000";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
