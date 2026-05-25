package com.jomariabejo.connectly_api.tenant_api.support;

import com.jomariabejo.connectly_api.tenant_api.entity.TenantRole;

import java.util.EnumSet;
import java.util.Set;

public final class TenantRolePolicy {

    public static final EnumSet<TenantRole> TEAM_MANAGEMENT = EnumSet.of(
            TenantRole.OWNER, TenantRole.ADMIN
    );

    public static final EnumSet<TenantRole> MEMBER_ROLE_CHANGE = EnumSet.of(
            TenantRole.OWNER
    );

    public static final EnumSet<TenantRole> INVITABLE_ROLES = EnumSet.of(
            TenantRole.ADMIN,
            TenantRole.MANAGER,
            TenantRole.WAREHOUSE,
            TenantRole.CASHIER,
            TenantRole.INVENTORY,
            TenantRole.CUSTOMER
    );

    public static final EnumSet<TenantRole> STORE_DASHBOARD = EnumSet.of(
            TenantRole.OWNER,
            TenantRole.ADMIN,
            TenantRole.MANAGER,
            TenantRole.WAREHOUSE,
            TenantRole.CASHIER,
            TenantRole.INVENTORY
    );

    public static final EnumSet<TenantRole> INVENTORY_WRITE = EnumSet.of(
            TenantRole.OWNER,
            TenantRole.ADMIN,
            TenantRole.MANAGER,
            TenantRole.WAREHOUSE,
            TenantRole.INVENTORY
    );

    public static final EnumSet<TenantRole> ADMIN_PORTAL = EnumSet.of(
            TenantRole.OWNER, TenantRole.ADMIN, TenantRole.MANAGER
    );

    private TenantRolePolicy() {
    }

    public static boolean isInvitable(TenantRole role) {
        return INVITABLE_ROLES.contains(role);
    }

    public static boolean canManageTeam(TenantRole role) {
        return TEAM_MANAGEMENT.contains(role);
    }

    public static boolean canChangeMemberRoles(TenantRole role) {
        return MEMBER_ROLE_CHANGE.contains(role);
    }

    public static boolean canWriteInventory(TenantRole role) {
        return role != null && INVENTORY_WRITE.contains(role);
    }

    public static boolean isStoreStaff(TenantRole role) {
        return role != null && STORE_DASHBOARD.contains(role);
    }

    public static Set<TenantRole> parseLegacy(String value) {
        if (value == null) {
            return EnumSet.noneOf(TenantRole.class);
        }
        return switch (value.toUpperCase()) {
            case "STAFF" -> EnumSet.of(TenantRole.MANAGER);
            case "EMPLOYEE" -> EnumSet.of(TenantRole.CASHIER);
            default -> EnumSet.noneOf(TenantRole.class);
        };
    }
}
