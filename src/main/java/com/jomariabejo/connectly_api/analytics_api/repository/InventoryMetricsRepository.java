package com.jomariabejo.connectly_api.analytics_api.repository;

import com.jomariabejo.connectly_api.analytics_api.entity.InventoryMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryMetricsRepository extends JpaRepository<InventoryMetric, Long> {
    
    @Query("SELECT i FROM InventoryMetric i WHERE i.tenant.id = :tenantId " +
           "AND i.productId = :productId AND i.analyticsDate BETWEEN :startDate AND :endDate " +
           "AND i.periodType = :periodType ORDER BY i.analyticsDate DESC")
    List<InventoryMetric> findByProductAndDateRange(
        @Param("tenantId") Long tenantId,
        @Param("productId") Long productId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("periodType") String periodType
    );
    
    @Query("SELECT AVG(i.inventoryTurnoverRatio) FROM InventoryMetric i " +
           "WHERE i.tenant.id = :tenantId AND i.analyticsDate BETWEEN :startDate AND :endDate")
    java.math.BigDecimal getAvgTurnoverRatio(
        @Param("tenantId") Long tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
