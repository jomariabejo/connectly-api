package com.jomariabejo.connectly_api.analytics_api.repository;

import com.jomariabejo.connectly_api.analytics_api.entity.ProfitAnalytic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProfitAnalyticsRepository extends JpaRepository<ProfitAnalytic, Long> {
    
    @Query("SELECT p FROM ProfitAnalytic p WHERE p.tenant.id = :tenantId " +
           "AND p.analyticsDate BETWEEN :startDate AND :endDate " +
           "AND p.periodType = :periodType " +
           "ORDER BY p.analyticsDate DESC")
    List<ProfitAnalytic> findByTenantAndDateRange(
        @Param("tenantId") Long tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("periodType") String periodType
    );
    
    @Query("SELECT p FROM ProfitAnalytic p WHERE p.tenant.id = :tenantId " +
           "AND p.productId = :productId AND p.analyticsDate = :date " +
           "AND p.periodType = :periodType")
    Optional<ProfitAnalytic> findByTenantProductAndDate(
        @Param("tenantId") Long tenantId,
        @Param("productId") Long productId,
        @Param("date") LocalDate date,
        @Param("periodType") String periodType
    );
    
    @Query("SELECT p FROM ProfitAnalytic p WHERE p.tenant.id = :tenantId " +
           "AND p.categoryId = :categoryId AND p.analyticsDate BETWEEN :startDate AND :endDate " +
           "AND p.periodType = :periodType ORDER BY p.analyticsDate DESC")
    List<ProfitAnalytic> findByCategoryAndDateRange(
        @Param("tenantId") Long tenantId,
        @Param("categoryId") Long categoryId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("periodType") String periodType
    );
    
    @Query("SELECT AVG(p.profitMarginPercent) FROM ProfitAnalytic p " +
           "WHERE p.tenant.id = :tenantId AND p.analyticsDate = :date")
    java.math.BigDecimal getAvgProfitMarginForDate(
        @Param("tenantId") Long tenantId,
        @Param("date") LocalDate date
    );
}
