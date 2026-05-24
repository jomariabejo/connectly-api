package com.jomariabejo.connectly_api.analytics_api.service;

import com.jomariabejo.connectly_api.analytics_api.dto.ProfitEstimationDto;
import com.jomariabejo.connectly_api.analytics_api.entity.ProfitAnalytic;
import com.jomariabejo.connectly_api.analytics_api.entity.ProfitEstimation;
import com.jomariabejo.connectly_api.analytics_api.repository.ProfitAnalyticsRepository;
import com.jomariabejo.connectly_api.analytics_api.repository.ProfitEstimationsRepository;
import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import com.jomariabejo.connectly_api.tenant_api.repository.TenantRepository;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import com.jomariabejo.connectly_api.tenant_api.service.AuditLoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfitEstimationService {

    private final ProfitEstimationsRepository estimationRepository;
    private final ProfitAnalyticsRepository profitRepository;
    private final TenantContextService tenantContextService;
    private final TenantRepository tenantRepository;
    private final AuditLoggingService auditService;

    /**
     * Estimate profit for upcoming period using linear trend analysis
     */
    @Transactional
    public ProfitEstimationDto estimateProfitWithLinearTrend(LocalDate forecastStart, 
                                                             LocalDate forecastEnd,
                                                             String forecastPeriod,
                                                             int historicalPeriods,
                                                             Long userId) {
        Long tenantId = tenantContextService.requireTenantId();
        
        // Get historical data
        LocalDate historyStart = forecastStart.minusMonths(historicalPeriods);
        List<ProfitAnalytic> historicalData = profitRepository.findByTenantAndDateRange(
            tenantId, historyStart, LocalDate.now(), "MONTHLY"
        );
        
        if (historicalData.isEmpty()) {
            throw new IllegalStateException("Insufficient historical data for forecast");
        }
        
        // Calculate trend
        BigDecimal avgRevenue = historicalData.stream()
            .map(ProfitAnalytic::getTotalRevenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(new BigDecimal(historicalData.size()), 2, java.math.RoundingMode.HALF_UP);
        
        BigDecimal avgCost = historicalData.stream()
            .map(ProfitAnalytic::getTotalCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(new BigDecimal(historicalData.size()), 2, java.math.RoundingMode.HALF_UP);
        
        BigDecimal avgMargin = historicalData.stream()
            .map(ProfitAnalytic::getProfitMarginPercent)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(new BigDecimal(historicalData.size()), 2, java.math.RoundingMode.HALF_UP);
        
        // Estimate future profit
        BigDecimal estimatedRevenue = avgRevenue;
        BigDecimal estimatedCost = avgCost;
        BigDecimal estimatedProfit = estimatedRevenue.subtract(estimatedCost);
        
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalStateException("Tenant not found"));
        
        ProfitEstimation estimation = ProfitEstimation.builder()
            .tenant(tenant)
            .estimationDate(LocalDate.now())
            .forecastStartDate(forecastStart)
            .forecastEndDate(forecastEnd)
            .forecastPeriod(forecastPeriod)
            .estimatedTotalRevenue(estimatedRevenue)
            .estimatedTotalCost(estimatedCost)
            .estimatedGrossProfit(estimatedProfit)
            .estimatedProfitMarginPercent(avgMargin)
            .confidenceLevel("MEDIUM")
            .methodology("LINEAR_TREND")
            .basedOnHistoricalPeriods(historicalPeriods)
            .auditUserId(userId)
            .build();
        
        ProfitEstimation saved = estimationRepository.save(estimation);
        log.info("Profit estimation created for tenant {} with linear trend", tenantId);
        
        return mapToDto(saved);
    }

    /**
     * Get profit estimations for tenant
     */
    @Transactional(readOnly = true)
    public List<ProfitEstimationDto> getEstimationsFromDate(LocalDate fromDate) {
        Long tenantId = tenantContextService.requireTenantId();
        
        return estimationRepository.findByTenantFromDate(tenantId, fromDate)
            .stream()
            .map(this::mapToDto)
            .toList();
    }

    /**
     * Get latest estimation for a period
     */
    @Transactional(readOnly = true)
    public ProfitEstimationDto getLatestEstimation(String period) {
        Long tenantId = tenantContextService.requireTenantId();
        
        return estimationRepository.findLatestByTenantAndPeriod(tenantId, period)
            .map(this::mapToDto)
            .orElseThrow(() -> new IllegalStateException("No estimation found for period: " + period));
    }

    private ProfitEstimationDto mapToDto(ProfitEstimation estimation) {
        return new ProfitEstimationDto(
            estimation.getEstimationDate(),
            estimation.getForecastStartDate(),
            estimation.getForecastEndDate(),
            estimation.getForecastPeriod(),
            estimation.getEstimatedTotalRevenue(),
            estimation.getEstimatedTotalCost(),
            estimation.getEstimatedGrossProfit(),
            estimation.getEstimatedProfitMarginPercent(),
            estimation.getConfidenceLevel(),
            estimation.getMethodology(),
            estimation.getBasedOnHistoricalPeriods(),
            estimation.getNotes()
        );
    }
}
