package com.jomariabejo.connectly_api.tenant_api.repository;

import com.jomariabejo.connectly_api.tenant_api.entity.FeatureAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface FeatureAccessRepository extends JpaRepository<FeatureAccess, Long> {
    Optional<FeatureAccess> findByPricingTierAndFeatureCodeAndEnabledTrue(String pricingTier, String featureCode);

    List<FeatureAccess> findByPricingTierAndEnabledTrue(String pricingTier);

    @Query("SELECT fa.featureCode FROM FeatureAccess fa WHERE fa.pricingTier = :tier AND fa.enabled = true")
    Set<String> findEnabledFeatureCodes(@Param("tier") String tier);

    boolean existsByPricingTierAndFeatureCodeAndEnabledTrue(String pricingTier, String featureCode);
}
