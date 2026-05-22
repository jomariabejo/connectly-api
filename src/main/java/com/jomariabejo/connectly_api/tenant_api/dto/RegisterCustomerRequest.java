package com.jomariabejo.connectly_api.tenant_api.dto;

import com.jomariabejo.connectly_api.dto.RegisterUserDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterCustomerRequest {
    @NotBlank
    private String tenantSlug;

    @Valid
    private RegisterUserDto user;
}
