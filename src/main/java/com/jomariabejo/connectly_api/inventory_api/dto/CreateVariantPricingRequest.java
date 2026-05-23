package com.jomariabejo.connectly_api.inventory_api.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVariantPricingRequest {
    private Long variantId;
    private BigDecimal quantityThreshold;
    private BigDecimal price;
    private String priceType; // RETAIL, WHOLESALE, BULK
    private String notes;
}
