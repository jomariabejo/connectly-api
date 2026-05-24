package com.jomariabejo.connectly_api.analytics_api.service;

import com.jomariabejo.connectly_api.analytics_api.dto.SalesReportDto;
import com.jomariabejo.connectly_api.analytics_api.entity.SalesAnalytic;
import com.jomariabejo.connectly_api.analytics_api.repository.SalesAnalyticsRepository;
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
public class SalesAnalyticsService {

    private final SalesAnalyticsRepository salesRepository;
    private final TenantContextService tenantContextService;

    /**
     * Get sales report for a date range
     */
    @Transactional(readOnly = true)
    public List<SalesReportDto> getSalesReport(LocalDate startDate, LocalDate endDate, String periodType) {
        Long tenantId = tenantContextService.requireTenantId();
        
        log.info("Generating sales report for tenant {} from {} to {}", tenantId, startDate, endDate);
        
        List<SalesAnalytic> analytics = salesRepository.findByTenantAndDateRange(
            tenantId, startDate, endDate, periodType
        );
        
        return analytics.stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    /**
     * Get total sales for a specific date
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalSalesForDate(LocalDate date) {
        Long tenantId = tenantContextService.requireTenantId();
        return salesRepository.getTotalSalesForDate(tenantId, date);
    }

    /**
     * Create or update sales analytics
     */
    @Transactional
    public SalesAnalytic saveSalesAnalytic(SalesAnalytic analytic, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();
        analytic.setAuditUserId(userId);
        
        SalesAnalytic saved = salesRepository.save(analytic);
        log.info("Sales analytic saved for tenant {}", tenantId);
        
        return saved;
    }

    private SalesReportDto mapToDto(SalesAnalytic analytic) {
        return new SalesReportDto(
            analytic.getAnalyticsDate(),
            analytic.getPeriodType(),
            analytic.getTotalSales(),
            analytic.getTotalUnitsSold(),
            analytic.getTotalTransactions(),
            analytic.getAvgTransactionValue(),
            analytic.getTopProductId(),
            null, // Product name would come from product service
            analytic.getTopCategoryId(),
            null  // Category name would come from category service
        );
    }
}
