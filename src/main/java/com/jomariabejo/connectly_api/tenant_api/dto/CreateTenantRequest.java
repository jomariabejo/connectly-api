package com.jomariabejo.connectly_api.tenant_api.dto;

import com.jomariabejo.connectly_api.tenant_api.entity.BusinessType;
import com.jomariabejo.connectly_api.tenant_api.entity.PricingTier;
import com.jomariabejo.connectly_api.tenant_api.entity.ProductCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTenantRequest {
    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must be lowercase alphanumeric with hyphens")
    private String slug;

    // New multi-tenant fields
    @Pattern(regexp = "^[a-z0-9-]{3,50}$", message = "Subdomain must be 3-50 lowercase alphanumeric characters with hyphens")
    private String subdomain;

    private BusinessType businessType;

    private PricingTier pricingTier;

    private Set<ProductCode> products;

    /**
     * Get subdomain with fallback to slug if not provided
     */
    public String getSubdomainOrSlug() {
        return this.subdomain != null ? this.subdomain : this.slug;
    }
}
