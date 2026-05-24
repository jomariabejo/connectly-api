package com.jomariabejo.connectly_api.inventory_api.repository;

import com.jomariabejo.connectly_api.inventory_api.entity.VariantPricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface VariantPricingRepository extends JpaRepository<VariantPricing, Long> {

    @Query("SELECT vp FROM VariantPricing vp WHERE vp.variant.id = :variantId ORDER BY vp.quantityThreshold DESC")
    List<VariantPricing> findByVariantId(@Param("variantId") Long variantId);

    @Query("SELECT vp FROM VariantPricing vp WHERE vp.variant.id = :variantId AND vp.quantityThreshold <= :quantity ORDER BY vp.quantityThreshold DESC LIMIT 1")
    Optional<VariantPricing> findBestPriceForQuantity(@Param("variantId") Long variantId, @Param("quantity") BigDecimal quantity);

    @Query("SELECT vp FROM VariantPricing vp WHERE vp.tenant.id = :tenantId AND vp.priceType = :priceType ORDER BY vp.quantityThreshold DESC")
    List<VariantPricing> findByTenantAndPriceType(@Param("tenantId") Long tenantId, @Param("priceType") String priceType);
}
