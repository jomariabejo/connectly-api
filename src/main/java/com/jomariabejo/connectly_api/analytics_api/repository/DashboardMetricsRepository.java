package com.jomariabejo.connectly_api.analytics_api.repository;

import com.jomariabejo.connectly_api.analytics_api.entity.DashboardMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DashboardMetricsRepository extends JpaRepository<DashboardMetric, Long> {
    
    @Query("SELECT d FROM DashboardMetric d WHERE d.tenant.id = :tenantId " +
           "AND d.metricDate = :date")
    Optional<DashboardMetric> findByTenantAndDate(
        @Param("tenantId") Long tenantId,
        @Param("date") LocalDate date
    );
    
    @Query("SELECT d FROM DashboardMetric d WHERE d.tenant.id = :tenantId " +
           "ORDER BY d.metricDate DESC LIMIT 1")
    Optional<DashboardMetric> findLatestByTenant(@Param("tenantId") Long tenantId);
}
