package com.jomariabejo.connectly_api.tenant_api.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicTenantDto {
    private Long id;
    private String name;
    private String slug;
}
