package com.jomariabejo.connectly_api.tenant_api.dto;

import com.jomariabejo.connectly_api.tenant_api.entity.ProductCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class CreateTenantRequest {
    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must be lowercase alphanumeric with hyphens")
    private String slug;

    private Set<ProductCode> products;
}
