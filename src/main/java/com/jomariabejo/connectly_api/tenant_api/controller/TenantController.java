package com.jomariabejo.connectly_api.tenant_api.controller;

import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.tenant_api.dto.CreateTenantRequest;
import com.jomariabejo.connectly_api.tenant_api.dto.TenantResponseDto;
import com.jomariabejo.connectly_api.tenant_api.dto.TenantSummaryDto;
import com.jomariabejo.connectly_api.tenant_api.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/tenants")
public class TenantController {
    private final TenantService tenantService;
    private final AuthenticationService authenticationService;

    public TenantController(TenantService tenantService, AuthenticationService authenticationService) {
        this.tenantService = tenantService;
        this.authenticationService = authenticationService;
    }

    @GetMapping("/mine")
    public ResponseEntity<List<TenantSummaryDto>> getMyTenants() {
        User user = authenticationService.getAuthenticatedUser();
        return ResponseEntity.ok(tenantService.getTenantsForUser(user));
    }

    @PostMapping
    public ResponseEntity<TenantResponseDto> createTenant(@RequestBody @Valid CreateTenantRequest request) {
        User user = authenticationService.getAuthenticatedUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantService.createTenant(request, user));
    }

    @GetMapping("/current")
    public ResponseEntity<TenantResponseDto> getCurrentTenant() {
        return ResponseEntity.ok(tenantService.getCurrentTenant());
    }
}
