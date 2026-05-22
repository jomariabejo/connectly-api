package com.jomariabejo.connectly_api.dto;

import com.jomariabejo.connectly_api.tenant_api.dto.TenantSummaryDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LoginResponse {
    private String token;
    private long expiresIn;
    private List<TenantSummaryDto> tenants;
    private String suggestedRedirect;
}
