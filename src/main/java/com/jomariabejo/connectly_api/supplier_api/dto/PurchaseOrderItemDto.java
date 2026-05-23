package com.jomariabejo.connectly_api.supplier_api.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderItemDto {
    private Long id;
    private String itemCode;
    private String description;
    private BigDecimal quantityOrdered;
    private BigDecimal quantityReceived;
    private String unitOfMeasure;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private Integer lineNumber;
}
