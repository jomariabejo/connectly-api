package com.jomariabejo.connectly_api.tenant_api.dto;

import com.jomariabejo.connectly_api.tenant_api.entity.TenantRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InvitePreviewDto {
    private String tenantName;
    private String tenantSlug;
    private TenantRole role;
    private String email;
    private boolean valid;
    private String message;
}
