package com.jomariabejo.connectly_api.analytics_api.repository;

import com.jomariabejo.connectly_api.analytics_api.entity.CategoryAnalytic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryAnalyticsRepository extends JpaRepository<CategoryAnalytic, Long> {
    
    @Query("SELECT c FROM CategoryAnalytic c WHERE c.tenant.id = :tenantId " +
           "AND c.categoryId = :categoryId AND c.analyticsDate = :date " +
           "AND c.periodType = :periodType")
    Optional<CategoryAnalytic> findByTenantCategoryAndDate(
        @Param("tenantId") Long tenantId,
        @Param("categoryId") Long categoryId,
        @Param("date") LocalDate date,
        @Param("periodType") String periodType
    );
    
    @Query("SELECT c FROM CategoryAnalytic c WHERE c.tenant.id = :tenantId " +
           "AND c.analyticsDate BETWEEN :startDate AND :endDate " +
           "ORDER BY c.totalSales DESC")
    List<CategoryAnalytic> findByTenantAndDateRangeOrderByRevenue(
        @Param("tenantId") Long tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
