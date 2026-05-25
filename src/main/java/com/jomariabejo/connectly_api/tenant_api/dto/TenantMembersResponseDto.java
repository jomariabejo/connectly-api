package com.jomariabejo.connectly_api.tenant_api.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TenantMembersResponseDto {
    List<TenantMemberDto> members;
    List<InviteResponseDto> pendingInvites;
}
