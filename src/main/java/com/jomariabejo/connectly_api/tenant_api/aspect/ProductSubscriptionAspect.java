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

    @Before("@annotation(requiresProduct)")
    public void checkMethodLevelProduct(RequiresProduct requiresProduct) {
        validateProductAccess(requiresProduct);
    }

    @Before("""
        @within(requiresProduct) &&
        !@annotation(com.jomariabejo.connectly_api.tenant_api.annotation.RequiresProduct)
        """)
    public void checkClassLevelProduct(RequiresProduct requiresProduct) {
        validateProductAccess(requiresProduct);
    }

    private void validateProductAccess(RequiresProduct requiresProduct) {
        tenantContextService.requireProduct(requiresProduct.value());
    }
}
