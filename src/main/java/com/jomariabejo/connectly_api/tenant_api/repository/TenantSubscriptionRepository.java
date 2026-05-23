package com.jomariabejo.connectly_api.tenant_api.repository;

import com.jomariabejo.connectly_api.tenant_api.entity.ProductCode;
import com.jomariabejo.connectly_api.tenant_api.entity.TenantSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, Long> {
    List<TenantSubscription> findByTenantIdAndActiveTrue(Long tenantId);
    Optional<TenantSubscription> findByTenantIdAndProductCodeAndActiveTrue(Long tenantId, ProductCode productCode);
    boolean existsByTenantIdAndProductCodeAndActiveTrue(Long tenantId, ProductCode productCode);
    
    /**
     * Find subscription by string product code (for flexible feature codes)
     */
    Optional<TenantSubscription> findByTenantIdAndProductCode(Long tenantId, String productCode);
    
    /**
     * Check if subscription exists by string product code
     */
    boolean existsByTenantIdAndProductCodeAndActiveTrue(Long tenantId, String productCode);
}
