package com.jomariabejo.connectly_api.tenant_api.dto;

import com.jomariabejo.connectly_api.tenant_api.entity.ProductCode;
import com.jomariabejo.connectly_api.tenant_api.entity.TenantRole;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class TenantSummaryDto {
    private Long id;
    private String name;
    private String slug;
    private TenantRole role;
    private Set<ProductCode> subscribedProducts;
}
