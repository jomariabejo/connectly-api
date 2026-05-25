package com.jomariabejo.connectly_api.tenant_api.service;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.tenant_api.dto.TenantSummaryDto;
import com.jomariabejo.connectly_api.tenant_api.entity.TenantRole;
import com.jomariabejo.connectly_api.tenant_api.support.TenantRolePolicy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LoginRedirectService {

    public String resolveRedirect(User user, List<TenantSummaryDto> tenants) {
        boolean platformAdmin = user.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (platformAdmin) {
            return "/super-admin";
        }

        if (tenants == null || tenants.isEmpty()) {
            return "/onboarding/store";
        }

        if (tenants.size() > 1) {
            return "/admin/select-tenant";
        }

        Set<TenantRole> roles = tenants.stream()
                .map(TenantSummaryDto::getRole)
                .collect(Collectors.toSet());

        boolean hasStoreStaff = roles.stream().anyMatch(TenantRolePolicy::isStoreStaff);
        boolean hasCustomer = roles.contains(TenantRole.CUSTOMER);

        if (hasStoreStaff && hasCustomer) {
            return "/portals";
        }

        TenantRole role = tenants.get(0).getRole();
        if (role == TenantRole.CUSTOMER) {
            return "/customer";
        }
        if (TenantRolePolicy.isStoreStaff(role)) {
            return "/dashboard";
        }
        return "/onboarding/store";
    }
}
