package com.jomariabejo.connectly_api.supplier_api.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddInvoiceItemRequest {
    private Long poItemId;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
}
