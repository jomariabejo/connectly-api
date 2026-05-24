package com.jomariabejo.connectly_api.analytics_api.controller;

import com.jomariabejo.connectly_api.analytics_api.dto.ProfitReportDto;
import com.jomariabejo.connectly_api.analytics_api.service.ProfitAnalyticsService;
import com.jomariabejo.connectly_api.tenant_api.service.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports/profit")
@RequiredArgsConstructor
@Slf4j
public class ProfitAnalyticsController {

    private final ProfitAnalyticsService profitService;
    private final PricingService pricingService;

    /**
     * Get profit report for date range
     */
    @GetMapping
    public ResponseEntity<List<ProfitReportDto>> getProfitReport(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(defaultValue = "MONTHLY") String periodType
    ) {
        Long tenantId = com.jomariabejo.connectly_api.tenant_api.context.TenantContext.getTenantId();
        pricingService.requireFeatureAccess(tenantId, "ANALYTICS");
        
        log.info("Fetching profit report from {} to {} with period {}", startDate, endDate, periodType);
        List<ProfitReportDto> report = profitService.getProfitReport(startDate, endDate, periodType);
        
        return ResponseEntity.ok(report);
    }

    /**
     * Get profit report filtered by product
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProfitReportDto>> getProfitReportByProduct(
        @PathVariable Long productId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(defaultValue = "MONTHLY") String periodType
    ) {
        Long tenantId = com.jomariabejo.connectly_api.tenant_api.context.TenantContext.getTenantId();
        pricingService.requireFeatureAccess(tenantId, "ANALYTICS");
        
        log.info("Fetching profit report for product {} from {} to {}", productId, startDate, endDate);
        List<ProfitReportDto> report = profitService.getProfitReportByProduct(productId, startDate, endDate, periodType);
        
        return ResponseEntity.ok(report);
    }

    /**
     * Get profit report filtered by category
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProfitReportDto>> getProfitReportByCategory(
        @PathVariable Long categoryId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(defaultValue = "MONTHLY") String periodType
    ) {
        Long tenantId = com.jomariabejo.connectly_api.tenant_api.context.TenantContext.getTenantId();
        pricingService.requireFeatureAccess(tenantId, "ANALYTICS");
        
        log.info("Fetching profit report for category {} from {} to {}", categoryId, startDate, endDate);
        List<ProfitReportDto> report = profitService.getProfitReportByCategory(categoryId, startDate, endDate, periodType);
        
        return ResponseEntity.ok(report);
    }

    /**
     * Get average profit margin for a date
     */
    @GetMapping("/margin/{date}")
    public ResponseEntity<java.math.BigDecimal> getAvgProfitMargin(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long tenantId = com.jomariabejo.connectly_api.tenant_api.context.TenantContext.getTenantId();
        pricingService.requireFeatureAccess(tenantId, "ANALYTICS");
        
        java.math.BigDecimal avgMargin = profitService.getAvgProfitMargin(date);
        return ResponseEntity.ok(avgMargin);
    }
}
