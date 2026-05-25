package com.jomariabejo.connectly_api.tenant_api.repository;

import com.jomariabejo.connectly_api.tenant_api.entity.TenantRole;
import com.jomariabejo.connectly_api.tenant_api.entity.TenantUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantUserRepository extends JpaRepository<TenantUser, Long> {
    Optional<TenantUser> findByTenantIdAndUserIdAndActiveTrue(Long tenantId, Long userId);

    List<TenantUser> findByUserIdAndActiveTrue(Long userId);

    @Query("SELECT tu FROM TenantUser tu JOIN FETCH tu.tenant t LEFT JOIN FETCH t.subscriptions WHERE tu.user.id = :userId AND tu.active = true")
    List<TenantUser> findByUserIdWithTenantAndSubscriptions(@Param("userId") Long userId);

    boolean existsByTenantIdAndUserId(Long tenantId, Long userId);

    List<TenantUser> findByTenantIdAndActiveTrueOrderByJoinedAtAsc(Long tenantId);

    long countByTenantIdAndTenantRoleAndActiveTrue(Long tenantId, TenantRole tenantRole);

    @Query("SELECT tu FROM TenantUser tu JOIN FETCH tu.user WHERE tu.tenant.id = :tenantId AND tu.active = true ORDER BY tu.joinedAt ASC")
    List<TenantUser> findByTenantIdWithUserAndActiveTrue(@Param("tenantId") Long tenantId);
}
