package com.jomariabejo.connectly_api.analytics_api.controller;

import com.jomariabejo.connectly_api.analytics_api.dto.DashboardMetricsDto;
import com.jomariabejo.connectly_api.analytics_api.service.DashboardMetricsService;
import com.jomariabejo.connectly_api.tenant_api.service.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardMetricsService dashboardService;
    private final PricingService pricingService;

    /**
     * Get latest dashboard metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<DashboardMetricsDto> getLatestMetrics() {
        Long tenantId = com.jomariabejo.connectly_api.tenant_api.context.TenantContext.getTenantId();
        pricingService.requireFeatureAccess(tenantId, "ANALYTICS");
        
        log.info("Fetching latest dashboard metrics");
        DashboardMetricsDto metrics = dashboardService.getLatestDashboardMetrics();
        
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get dashboard metrics for a specific date
     */
    @GetMapping("/metrics/{date}")
    public ResponseEntity<DashboardMetricsDto> getMetricsForDate(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long tenantId = com.jomariabejo.connectly_api.tenant_api.context.TenantContext.getTenantId();
        pricingService.requireFeatureAccess(tenantId, "ANALYTICS");
        
        log.info("Fetching dashboard metrics for {}", date);
        DashboardMetricsDto metrics = dashboardService.getDashboardMetricsForDate(date);
        
        return ResponseEntity.ok(metrics);
    }
}
