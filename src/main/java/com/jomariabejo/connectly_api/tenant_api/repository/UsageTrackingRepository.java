package com.jomariabejo.connectly_api.tenant_api.repository;

import com.jomariabejo.connectly_api.tenant_api.entity.UsageTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UsageTrackingRepository extends JpaRepository<UsageTracking, Long> {
    Optional<UsageTracking> findByTenantIdAndTrackingMonth(Long tenantId, LocalDate trackingMonth);
}
