package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.common.ApiPaths;
import com.jomariabejo.connectly_api.tenant_api.dto.PublicTenantDto;
import com.jomariabejo.connectly_api.tenant_api.service.ActorRegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unauthenticated public endpoints (permitted via {@code /v1/public/**} in security config).
 */
@RestController
@RequestMapping(ApiPaths.V1_PUBLIC)
public class PublicController {

    private final ActorRegistrationService actorRegistrationService;

    public PublicController(ActorRegistrationService actorRegistrationService) {
        this.actorRegistrationService = actorRegistrationService;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Connectly API is running!";
    }

    @GetMapping("/tenants/{slug}")
    public ResponseEntity<PublicTenantDto> getTenantBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(actorRegistrationService.resolvePublicTenant(slug));
    }
}
