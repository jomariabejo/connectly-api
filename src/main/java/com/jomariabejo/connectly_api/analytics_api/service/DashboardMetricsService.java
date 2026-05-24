package com.jomariabejo.connectly_api.analytics_api.service;

import com.jomariabejo.connectly_api.analytics_api.dto.DashboardMetricsDto;
import com.jomariabejo.connectly_api.analytics_api.entity.DashboardMetric;
import com.jomariabejo.connectly_api.analytics_api.repository.DashboardMetricsRepository;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardMetricsService {

    private final DashboardMetricsRepository dashboardRepository;
    private final TenantContextService tenantContextService;

    /**
     * Get latest dashboard metrics for tenant
     */
    @Transactional(readOnly = true)
    public DashboardMetricsDto getLatestDashboardMetrics() {
        Long tenantId = tenantContextService.requireTenantId();
        
        log.info("Retrieving latest dashboard metrics for tenant {}", tenantId);
        
        return dashboardRepository.findLatestByTenant(tenantId)
            .map(this::mapToDto)
            .orElseThrow(() -> new IllegalStateException("No dashboard metrics available for tenant"));
    }

    /**
     * Get dashboard metrics for a specific date
     */
    @Transactional(readOnly = true)
    public DashboardMetricsDto getDashboardMetricsForDate(LocalDate date) {
        Long tenantId = tenantContextService.requireTenantId();
        
        return dashboardRepository.findByTenantAndDate(tenantId, date)
            .map(this::mapToDto)
            .orElseThrow(() -> new IllegalStateException("No metrics found for date: " + date));
    }

    /**
     * Create or update dashboard metrics
     */
    @Transactional
    public DashboardMetric saveDashboardMetrics(DashboardMetric metric, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();
        metric.setAuditUserId(userId);
        
        DashboardMetric saved = dashboardRepository.save(metric);
        log.info("Dashboard metrics saved for tenant {}", tenantId);
        
        return saved;
    }

    private DashboardMetricsDto mapToDto(DashboardMetric metric) {
        DashboardMetricsDto dto = new DashboardMetricsDto();
        dto.setMetricDate(metric.getMetricDate());
        dto.setTotalSalesMtd(metric.getTotalSalesMtd());
        dto.setTotalSalesYtd(metric.getTotalSalesYtd());
        dto.setGrossProfitMtd(metric.getGrossProfitMtd());
        dto.setGrossProfitYtd(metric.getGrossProfitYtd());
        dto.setTotalOrders(metric.getTotalOrders());
        dto.setTotalCustomers(metric.getTotalCustomers());
        dto.setInventoryValue(metric.getInventoryValue());
        dto.setLowStockCount(metric.getLowStockCount());
        dto.setTopSellingProductId(metric.getTopSellingProductId());
        dto.setAvgProfitMarginPercent(metric.getAvgProfitMarginPercent());
        
        // Calculate profit margin MTD
        if (metric.getTotalSalesMtd().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal marginMtd = metric.getGrossProfitMtd()
                .divide(metric.getTotalSalesMtd(), 2, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100));
            dto.setProfitMarginMtdPercent(marginMtd);
        }
        
        return dto;
    }
}
