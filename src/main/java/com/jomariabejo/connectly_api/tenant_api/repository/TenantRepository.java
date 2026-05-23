package com.jomariabejo.connectly_api.tenant_api.repository;

import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /**
     * Find tenant by subdomain for subdomain-based multi-tenancy
     */
    Optional<Tenant> findBySubdomain(String subdomain);

    /**
     * Check if subdomain is already taken
     */
    boolean existsBySubdomain(String subdomain);

    /**
     * Find active tenant by subdomain
     */
    @Query("SELECT t FROM Tenant t WHERE t.subdomain = :subdomain AND t.status = 'ACTIVE'")
    Optional<Tenant> findActiveBySubdomain(@Param("subdomain") String subdomain);

    /**
     * Check if tenant subscription is valid
     */
    @Query("SELECT t FROM Tenant t WHERE t.id = :tenantId AND t.subscriptionActive = true")
    Optional<Tenant> findActiveSubscription(@Param("tenantId") Long tenantId);
}
