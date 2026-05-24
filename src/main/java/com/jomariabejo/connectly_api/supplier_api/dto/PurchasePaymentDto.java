package com.jomariabejo.connectly_api.supplier_api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchasePaymentDto {
    private Long id;
    private Long supplierId;
    private String supplierName;
    private BigDecimal paymentAmount;
    private LocalDateTime paymentDate;
    private String paymentMethod;
    private String referenceNumber;
    private LocalDateTime createdAt;
}
