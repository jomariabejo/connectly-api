package com.jomariabejo.connectly_api.tenant_api.controller;

import com.jomariabejo.connectly_api.tenant_api.dto.AdminDashboardDto;
import com.jomariabejo.connectly_api.tenant_api.service.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/tenant/dashboard")
public class AdminDashboardController {
    private final TenantService tenantService;

    public AdminDashboardController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/overview")
    public ResponseEntity<AdminDashboardDto> getOverview() {
        return ResponseEntity.ok(tenantService.getAdminDashboard());
    }
}
