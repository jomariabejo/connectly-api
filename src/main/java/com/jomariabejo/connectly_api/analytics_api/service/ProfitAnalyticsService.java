package com.jomariabejo.connectly_api.analytics_api.service;

import com.jomariabejo.connectly_api.analytics_api.dto.ProfitReportDto;
import com.jomariabejo.connectly_api.analytics_api.entity.ProfitAnalytic;
import com.jomariabejo.connectly_api.analytics_api.repository.ProfitAnalyticsRepository;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfitAnalyticsService {

    private final ProfitAnalyticsRepository profitRepository;
    private final TenantContextService tenantContextService;

    /**
     * Get profit report for a date range
     */
    @Transactional(readOnly = true)
    public List<ProfitReportDto> getProfitReport(LocalDate startDate, LocalDate endDate, String periodType) {
        Long tenantId = tenantContextService.requireTenantId();
        
        log.info("Generating profit report for tenant {} from {} to {}", tenantId, startDate, endDate);
        
        List<ProfitAnalytic> analytics = profitRepository.findByTenantAndDateRange(
            tenantId, startDate, endDate, periodType
        );
        
        return analytics.stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    /**
     * Get profit report for a specific product
     */
    @Transactional(readOnly = true)
    public List<ProfitReportDto> getProfitReportByProduct(Long productId, LocalDate startDate, 
                                                          LocalDate endDate, String periodType) {
        Long tenantId = tenantContextService.requireTenantId();
        
        List<ProfitAnalytic> analytics = profitRepository.findByTenantAndDateRange(
            tenantId, startDate, endDate, periodType
        );
        
        return analytics.stream()
            .filter(p -> p.getProductId() != null && p.getProductId().equals(productId))
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    /**
     * Get profit report for a category
     */
    @Transactional(readOnly = true)
    public List<ProfitReportDto> getProfitReportByCategory(Long categoryId, LocalDate startDate,
                                                           LocalDate endDate, String periodType) {
        Long tenantId = tenantContextService.requireTenantId();
        
        List<ProfitAnalytic> analytics = profitRepository.findByCategoryAndDateRange(
            tenantId, categoryId, startDate, endDate, periodType
        );
        
        return analytics.stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    /**
     * Get average profit margin for a date
     */
    @Transactional(readOnly = true)
    public BigDecimal getAvgProfitMargin(LocalDate date) {
        Long tenantId = tenantContextService.requireTenantId();
        return profitRepository.getAvgProfitMarginForDate(tenantId, date);
    }

    /**
     * Create or update profit analytics
     */
    @Transactional
    public ProfitAnalytic saveProfitAnalytic(ProfitAnalytic analytic, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();
        analytic.setAuditUserId(userId);
        
        ProfitAnalytic saved = profitRepository.save(analytic);
        log.info("Profit analytic saved for tenant {}", tenantId);
        
        return saved;
    }

    private ProfitReportDto mapToDto(ProfitAnalytic analytic) {
        return new ProfitReportDto(
            analytic.getAnalyticsDate(),
            analytic.getPeriodType(),
            analytic.getProductId(),
            null, // Product name
            analytic.getCategoryId(),
            null, // Category name
            analytic.getTotalRevenue(),
            analytic.getTotalCost(),
            analytic.getGrossProfit(),
            analytic.getProfitMarginPercent(),
            analytic.getUnitsSold(),
            analytic.getAvgCostPerUnit(),
            analytic.getAvgSellingPrice()
        );
    }
}
