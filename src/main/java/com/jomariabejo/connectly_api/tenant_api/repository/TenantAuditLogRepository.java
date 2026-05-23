package com.jomariabejo.connectly_api.tenant_api.repository;

import com.jomariabejo.connectly_api.tenant_api.entity.TenantAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TenantAuditLogRepository extends JpaRepository<TenantAuditLog, Long> {
    Page<TenantAuditLog> findByTenantId(Long tenantId, Pageable pageable);

    Page<TenantAuditLog> findByTenantIdAndCreatedAtAfter(Long tenantId, LocalDateTime since, Pageable pageable);

    Page<TenantAuditLog> findByTenantIdAndAction(Long tenantId, String action, Pageable pageable);

    Page<TenantAuditLog> findByTenantIdAndUserIdAndCreatedAtAfter(Long tenantId, Long userId, LocalDateTime since, Pageable pageable);
}
