package com.jomariabejo.connectly_api.analytics_api.controller;

import com.jomariabejo.connectly_api.analytics_api.dto.SalesReportDto;
import com.jomariabejo.connectly_api.analytics_api.service.SalesAnalyticsService;
import com.jomariabejo.connectly_api.tenant_api.service.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports/sales")
@RequiredArgsConstructor
@Slf4j
public class SalesReportsController {

    private final SalesAnalyticsService salesService;
    private final PricingService pricingService;

    /**
     * Get sales report for date range
     * Requires PROFESSIONAL or higher pricing tier
     */
    @GetMapping
    public ResponseEntity<List<SalesReportDto>> getSalesReport(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(defaultValue = "DAILY") String periodType
    ) {
        Long tenantId = com.jomariabejo.connectly_api.tenant_api.context.TenantContext.getTenantId();
        pricingService.requireFeatureAccess(tenantId, "ANALYTICS");
        
        log.info("Fetching sales report from {} to {} with period {}", startDate, endDate, periodType);
        List<SalesReportDto> report = salesService.getSalesReport(startDate, endDate, periodType);
        
        return ResponseEntity.ok(report);
    }

    /**
     * Get total sales for a specific date
     */
    @GetMapping("/daily/{date}")
    public ResponseEntity<java.math.BigDecimal> getDailySales(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long tenantId = com.jomariabejo.connectly_api.tenant_api.context.TenantContext.getTenantId();
        pricingService.requireFeatureAccess(tenantId, "ANALYTICS");
        
        java.math.BigDecimal totalSales = salesService.getTotalSalesForDate(date);
        return ResponseEntity.ok(totalSales);
    }
}
