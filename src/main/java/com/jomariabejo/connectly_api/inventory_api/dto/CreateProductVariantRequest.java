package com.jomariabejo.connectly_api.inventory_api.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProductVariantRequest {
    private Long parentProductId;
    private String sku;
    private String name;
    private String unitType; // PIECES, GLASS, PACK, BOTTLE
    private BigDecimal quantityPerUnit;
    private BigDecimal retailPrice;
    private BigDecimal wholesalePrice;
    private BigDecimal reorderLevel;
    private String notes;
}
