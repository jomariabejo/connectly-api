package com.jomariabejo.connectly_api.analytics_api.repository;

import com.jomariabejo.connectly_api.analytics_api.entity.ProfitEstimation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProfitEstimationsRepository extends JpaRepository<ProfitEstimation, Long> {
    
    @Query("SELECT p FROM ProfitEstimation p WHERE p.tenant.id = :tenantId " +
           "AND p.forecastStartDate >= :fromDate " +
           "ORDER BY p.forecastStartDate DESC")
    List<ProfitEstimation> findByTenantFromDate(
        @Param("tenantId") Long tenantId,
        @Param("fromDate") LocalDate fromDate
    );
    
    @Query("SELECT p FROM ProfitEstimation p WHERE p.tenant.id = :tenantId " +
           "AND p.forecastStartDate <= :startDate AND p.forecastEndDate >= :endDate " +
           "AND p.forecastPeriod = :period")
    Optional<ProfitEstimation> findByTenantAndDateRangeAndPeriod(
        @Param("tenantId") Long tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("period") String period
    );
    
    @Query("SELECT p FROM ProfitEstimation p WHERE p.tenant.id = :tenantId " +
           "AND p.forecastPeriod = :period ORDER BY p.estimationDate DESC LIMIT 1")
    Optional<ProfitEstimation> findLatestByTenantAndPeriod(
        @Param("tenantId") Long tenantId,
        @Param("period") String period
    );
}
