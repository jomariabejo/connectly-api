package com.jomariabejo.connectly_api.tenant_api.service;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.tenant_api.dto.TenantSummaryDto;
import com.jomariabejo.connectly_api.tenant_api.entity.TenantRole;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
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
            return "/admin/setup";
        }

        if (tenants.size() > 1) {
            return "/admin/select-tenant";
        }

        Set<TenantRole> roles = tenants.stream()
                .map(TenantSummaryDto::getRole)
                .collect(Collectors.toSet());

        EnumSet<TenantRole> adminRoles = EnumSet.of(TenantRole.OWNER, TenantRole.ADMIN, TenantRole.STAFF);
        boolean hasAdmin = roles.stream().anyMatch(adminRoles::contains);
        boolean hasCustomer = roles.contains(TenantRole.CUSTOMER);
        boolean hasEmployee = roles.contains(TenantRole.EMPLOYEE);

        if (hasAdmin && (hasCustomer || hasEmployee)) {
            return "/portals";
        }

        TenantRole role = tenants.get(0).getRole();
        if (role == TenantRole.CUSTOMER) {
            return "/customer";
        }
        if (role == TenantRole.EMPLOYEE) {
            return "/employee";
        }
        return "/admin";
    }
}
