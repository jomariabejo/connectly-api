package com.jomariabejo.connectly_api.inventory_api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantDetailDto {
    private Long id;
    private Long parentProductId;
    private String sku;
    private String name;
    private String unitType;
    private BigDecimal quantityPerUnit;
    private BigDecimal retailPrice;
    private BigDecimal wholesalePrice;
    private BigDecimal onHandQuantity;
    private BigDecimal reservedQuantity;
    private BigDecimal availableQuantity;
    private BigDecimal reorderLevel;
    private String status;
    private String imageKey;
    private String notes;
    private List<VariantPricingDto> pricingTiers;
}
