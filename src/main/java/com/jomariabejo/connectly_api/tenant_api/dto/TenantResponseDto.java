package com.jomariabejo.connectly_api.tenant_api.dto;

import com.jomariabejo.connectly_api.tenant_api.entity.ProductCode;
import com.jomariabejo.connectly_api.tenant_api.entity.TenantStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class TenantResponseDto {
    private Long id;
    private String name;
    private String slug;
    private TenantStatus status;
    private String timezone;
    private String currency;
    private Set<ProductCode> subscribedProducts;
    private LocalDateTime createdAt;
}
