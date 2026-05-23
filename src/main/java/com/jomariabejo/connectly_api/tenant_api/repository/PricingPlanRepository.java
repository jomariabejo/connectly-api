package com.jomariabejo.connectly_api.tenant_api.repository;

import com.jomariabejo.connectly_api.tenant_api.entity.PricingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PricingPlanRepository extends JpaRepository<PricingPlan, Long> {
    Optional<PricingPlan> findByTierAndActiveTrue(String tier);
    Optional<PricingPlan> findByTier(String tier);
}
