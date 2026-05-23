package com.jomariabejo.connectly_api.inventory_api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TingiSaleDto {
    private Long id;
    private Long orderId;
    private Long variantId;
    private String variantName;
    private String unitType;
    private BigDecimal quantitySold;
    private BigDecimal pricePerUnit;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
}
