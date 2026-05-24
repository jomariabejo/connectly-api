package com.jomariabejo.connectly_api.inventory_api.repository;

import com.jomariabejo.connectly_api.inventory_api.entity.TingiSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TingiSaleRepository extends JpaRepository<TingiSale, Long> {

    @Query("SELECT ts FROM TingiSale ts WHERE ts.tenant.id = :tenantId AND ts.orderId = :orderId")
    List<TingiSale> findByTenantAndOrderId(@Param("tenantId") Long tenantId, @Param("orderId") Long orderId);

    @Query("SELECT ts FROM TingiSale ts WHERE ts.tenant.id = :tenantId AND ts.variant.id = :variantId ORDER BY ts.createdAt DESC")
    List<TingiSale> findByTenantAndVariantId(@Param("tenantId") Long tenantId, @Param("variantId") Long variantId);

    @Query("SELECT COALESCE(SUM(ts.totalAmount), 0) FROM TingiSale ts WHERE ts.tenant.id = :tenantId AND ts.createdAt >= :startDate AND ts.createdAt <= :endDate")
    BigDecimal getTotalSalesByDateRange(@Param("tenantId") Long tenantId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT ts.variant.id, SUM(ts.quantitySold) FROM TingiSale ts WHERE ts.tenant.id = :tenantId AND ts.createdAt >= :startDate AND ts.createdAt <= :endDate GROUP BY ts.variant.id ORDER BY SUM(ts.quantitySold) DESC")
    List<Object[]> getTopSellingVariantsByDateRange(@Param("tenantId") Long tenantId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
