package com.jomariabejo.connectly_api.tenant_api.repository;

import com.jomariabejo.connectly_api.tenant_api.entity.TenantInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantInvitationRepository extends JpaRepository<TenantInvitation, Long> {
    Optional<TenantInvitation> findByToken(String token);

    List<TenantInvitation> findByTenantIdAndAcceptedAtIsNullOrderByCreatedAtDesc(Long tenantId);

    boolean existsByTenantIdAndEmailAndAcceptedAtIsNull(Long tenantId, String email);

    Optional<TenantInvitation> findByIdAndTenantId(Long id, Long tenantId);
}
