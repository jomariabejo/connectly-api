package com.jomariabejo.connectly_api.tenant_api.context;

import com.jomariabejo.connectly_api.tenant_api.entity.ProductCode;
import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import com.jomariabejo.connectly_api.tenant_api.entity.TenantRole;

import java.util.EnumSet;
import java.util.Set;

public final class TenantContext {
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<TenantRole> TENANT_ROLE = new ThreadLocal<>();
    private static final ThreadLocal<Set<ProductCode>> SUBSCRIBED_PRODUCTS = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId, TenantRole role, Set<ProductCode> products) {
        TENANT_ID.set(tenantId);
        TENANT_ROLE.set(role);
        SUBSCRIBED_PRODUCTS.set(products == null ? EnumSet.noneOf(ProductCode.class) : EnumSet.copyOf(products));
    }

    public static void setFromTenant(Tenant tenant, TenantRole role, Set<ProductCode> products) {
        set(tenant.getId(), role, products);
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static TenantRole getTenantRole() {
        return TENANT_ROLE.get();
    }

    public static Set<ProductCode> getSubscribedProducts() {
        Set<ProductCode> products = SUBSCRIBED_PRODUCTS.get();
        return products == null ? EnumSet.noneOf(ProductCode.class) : products;
    }

    public static boolean hasProduct(ProductCode productCode) {
        return getSubscribedProducts().contains(productCode);
    }

    public static void clear() {
        TENANT_ID.remove();
        TENANT_ROLE.remove();
        SUBSCRIBED_PRODUCTS.remove();
    }
}
