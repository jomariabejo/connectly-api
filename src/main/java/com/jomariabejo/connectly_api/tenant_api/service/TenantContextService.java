package com.jomariabejo.connectly_api.tenant_api.service;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.tenant_api.context.TenantContext;
import com.jomariabejo.connectly_api.tenant_api.entity.ProductCode;
import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import com.jomariabejo.connectly_api.tenant_api.entity.TenantSubscription;
import com.jomariabejo.connectly_api.tenant_api.entity.TenantRole;
import com.jomariabejo.connectly_api.tenant_api.entity.TenantUser;
import com.jomariabejo.connectly_api.tenant_api.exception.ProductNotSubscribedException;
import com.jomariabejo.connectly_api.tenant_api.exception.TenantAccessDeniedException;
import com.jomariabejo.connectly_api.tenant_api.exception.TenantNotFoundException;
import com.jomariabejo.connectly_api.tenant_api.repository.TenantRepository;
import com.jomariabejo.connectly_api.tenant_api.repository.TenantSubscriptionRepository;
import com.jomariabejo.connectly_api.tenant_api.repository.TenantUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TenantContextService {
    private final TenantRepository tenantRepository;
    private final TenantUserRepository tenantUserRepository;
    private final TenantSubscriptionRepository subscriptionRepository;

    public TenantContextService(
            TenantRepository tenantRepository,
            TenantUserRepository tenantUserRepository,
            TenantSubscriptionRepository subscriptionRepository) {
        this.tenantRepository = tenantRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional(readOnly = true)
    public void resolveAndSetContext(Long tenantId, User user, boolean platformAdmin) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        if (!platformAdmin) {
            TenantUser membership = tenantUserRepository.findByTenantIdAndUserIdAndActiveTrue(tenantId, user.getId())
                    .orElseThrow(() -> new TenantAccessDeniedException("You do not have access to this organization"));
            Set<ProductCode> products = loadActiveProducts(tenantId);
            TenantContext.setFromTenant(tenant, membership.getTenantRole(), products);
        } else {
            Set<ProductCode> products = loadActiveProducts(tenantId);
            TenantContext.setFromTenant(tenant, null, products);
        }
    }

    public Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new TenantAccessDeniedException("X-Tenant-Id header is required");
        }
        return tenantId;
    }

    public TenantRole requireTenantRole() {
        TenantRole role = TenantContext.getTenantRole();
        if (role == null) {
            throw new TenantAccessDeniedException("Tenant role is not set for this request");
        }
        return role;
    }

    public void requireProduct(ProductCode productCode) {
        requireTenantId();
        if (!TenantContext.hasProduct(productCode)) {
            throw new ProductNotSubscribedException("Product not subscribed: " + productCode);
        }
    }

    @Transactional(readOnly = true)
    public Tenant requireTenant() {
        Long tenantId = requireTenantId();
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
    }

    private Set<ProductCode> loadActiveProducts(Long tenantId) {
        return subscriptionRepository.findByTenantIdAndActiveTrue(tenantId).stream()
                .map(TenantSubscription::getProductCode)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ProductCode.class)));
    }
}
