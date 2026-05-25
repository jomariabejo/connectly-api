package com.jomariabejo.connectly_api.tenant_api.dto;

import com.jomariabejo.connectly_api.tenant_api.entity.TenantRole;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class TenantMemberDto {
    Long userId;
    String email;
    String username;
    String firstName;
    String lastName;
    TenantRole role;
    boolean active;
    LocalDateTime joinedAt;
}
