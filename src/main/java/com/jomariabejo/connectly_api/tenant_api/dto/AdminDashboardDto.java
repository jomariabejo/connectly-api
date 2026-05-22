package com.jomariabejo.connectly_api.tenant_api.dto;

import com.jomariabejo.connectly_api.tenant_api.entity.ProductCode;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

@Getter
@Builder
public class AdminDashboardDto {
    private Long tenantId;
    private String tenantName;
    private Set<ProductCode> subscribedProducts;
    private Map<String, Long> metrics;
}
