package com.jomariabejo.connectly_api.supplier_api.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GRNItemDto {
    private Long id;
    private Long poItemId;
    private BigDecimal quantityReceived;
    private BigDecimal quantityAccepted;
    private BigDecimal quantityRejected;
}
