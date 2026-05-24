package com.jomariabejo.connectly_api.analytics_api.repository;

import com.jomariabejo.connectly_api.analytics_api.entity.CustomerAnalytic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAnalyticsRepository extends JpaRepository<CustomerAnalytic, Long> {
    
    @Query("SELECT c FROM CustomerAnalytic c WHERE c.tenant.id = :tenantId " +
           "AND c.customerId = :customerId AND c.analyticsDate = :date " +
           "AND c.periodType = :periodType")
    Optional<CustomerAnalytic> findByTenantCustomerAndDate(
        @Param("tenantId") Long tenantId,
        @Param("customerId") Long customerId,
        @Param("date") LocalDate date,
        @Param("periodType") String periodType
    );
    
    @Query("SELECT c FROM CustomerAnalytic c WHERE c.tenant.id = :tenantId " +
           "AND c.analyticsDate BETWEEN :startDate AND :endDate " +
           "ORDER BY c.totalPurchases DESC LIMIT :limit")
    List<CustomerAnalytic> findTopCustomersByPeriod(
        @Param("tenantId") Long tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("limit") int limit
    );
}
