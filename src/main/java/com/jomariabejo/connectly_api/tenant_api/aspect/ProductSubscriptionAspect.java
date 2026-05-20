package com.jomariabejo.connectly_api.tenant_api.aspect;

import com.jomariabejo.connectly_api.tenant_api.annotation.RequiresProduct;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ProductSubscriptionAspect {
    private final TenantContextService tenantContextService;

    public ProductSubscriptionAspect(TenantContextService tenantContextService) {
        this.tenantContextService = tenantContextService;
    }

    @Before("@within(requiresProduct) || @annotation(requiresProduct)")
    public void checkProductSubscription(RequiresProduct requiresProduct) {
        tenantContextService.requireProduct(requiresProduct.value());
    }
}
