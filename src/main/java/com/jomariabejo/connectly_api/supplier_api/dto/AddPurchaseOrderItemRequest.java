package com.jomariabejo.connectly_api.supplier_api.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddPurchaseOrderItemRequest {
    private String itemCode;
    private String description;
    private BigDecimal quantityOrdered;
    private String unitOfMeasure;
    private BigDecimal unitCost;
}
