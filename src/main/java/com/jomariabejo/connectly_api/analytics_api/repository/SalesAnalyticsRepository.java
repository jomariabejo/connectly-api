package com.jomariabejo.connectly_api.analytics_api.repository;

import com.jomariabejo.connectly_api.analytics_api.entity.SalesAnalytic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesAnalyticsRepository extends JpaRepository<SalesAnalytic, Long> {
    
    @Query("SELECT s FROM SalesAnalytic s WHERE s.tenant.id = :tenantId " +
           "AND s.analyticsDate BETWEEN :startDate AND :endDate " +
           "AND s.periodType = :periodType " +
           "ORDER BY s.analyticsDate DESC")
    List<SalesAnalytic> findByTenantAndDateRange(
        @Param("tenantId") Long tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("periodType") String periodType
    );
    
    @Query("SELECT s FROM SalesAnalytic s WHERE s.tenant.id = :tenantId " +
           "AND s.analyticsDate = :date AND s.periodType = :periodType")
    Optional<SalesAnalytic> findByTenantAndDateAndPeriod(
        @Param("tenantId") Long tenantId,
        @Param("date") LocalDate date,
        @Param("periodType") String periodType
    );
    
    @Query("SELECT COALESCE(SUM(s.totalSales), 0) FROM SalesAnalytic s " +
           "WHERE s.tenant.id = :tenantId AND s.analyticsDate = :date")
    java.math.BigDecimal getTotalSalesForDate(
        @Param("tenantId") Long tenantId,
        @Param("date") LocalDate date
    );
}
