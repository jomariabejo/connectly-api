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
}
