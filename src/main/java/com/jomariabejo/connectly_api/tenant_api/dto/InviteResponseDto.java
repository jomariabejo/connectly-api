package com.jomariabejo.connectly_api.tenant_api.dto;

import com.jomariabejo.connectly_api.tenant_api.entity.TenantRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InviteResponseDto {
    private Long id;
    private String email;
    private TenantRole role;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
