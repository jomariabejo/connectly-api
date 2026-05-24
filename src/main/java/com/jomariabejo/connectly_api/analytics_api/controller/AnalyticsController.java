package com.jomariabejo.connectly_api.analytics_api.controller;

import com.jomariabejo.connectly_api.analytics_api.dto.ProfitEstimationDto;
import com.jomariabejo.connectly_api.analytics_api.service.ProfitEstimationService;
import com.jomariabejo.connectly_api.tenant_api.service.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final ProfitEstimationService estimationService;
    private final PricingService pricingService;

    /**
     * Generate profit estimation using linear trend
     */
    @PostMapping("/profit-estimate")
    public ResponseEntity<ProfitEstimationDto> generateProfitEstimate(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate forecastStart,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate forecastEnd,
        @RequestParam(defaultValue = "MONTHLY") String forecastPeriod,
        @RequestParam(defaultValue = "12") int historicalPeriods,
        @RequestHeader(name = "X-User-Id", required = false) Long userId
    ) {
        Long tenantId = com.jomariabejo.connectly_api.tenant_api.context.TenantContext.getTenantId();
        pricingService.requireFeatureAccess(tenantId, "ANALYTICS");
        
        log.info("Generating profit estimate from {} to {}", forecastStart, forecastEnd);
        
        Long effectiveUserId = userId != null ? userId : 0L;
        ProfitEstimationDto estimation = estimationService.estimateProfitWithLinearTrend(
            forecastStart, forecastEnd, forecastPeriod, historicalPeriods, effectiveUserId
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(estimation);
    }

    /**
     * Get profit estimations from a date onwards
     */
    @GetMapping("/profit-estimates")
    public ResponseEntity<List<ProfitEstimationDto>> getEstimations(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate
    ) {
        Long tenantId = com.jomariabejo.connectly_api.tenant_api.context.TenantContext.getTenantId();
        pricingService.requireFeatureAccess(tenantId, "ANALYTICS");
        
        log.info("Fetching profit estimates from {}", fromDate);
        List<ProfitEstimationDto> estimations = estimationService.getEstimationsFromDate(fromDate);
        
        return ResponseEntity.ok(estimations);
    }

    /**
     * Get latest profit estimation for a period
     */
    @GetMapping("/profit-estimate/latest/{period}")
    public ResponseEntity<ProfitEstimationDto> getLatestEstimation(
        @PathVariable String period
    ) {
        Long tenantId = com.jomariabejo.connectly_api.tenant_api.context.TenantContext.getTenantId();
        pricingService.requireFeatureAccess(tenantId, "ANALYTICS");
        
        log.info("Fetching latest profit estimate for period {}", period);
        ProfitEstimationDto estimation = estimationService.getLatestEstimation(period);
        
        return ResponseEntity.ok(estimation);
    }
}
