package com.jomariabejo.connectly_api.supplier_api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordPaymentRequest {
    private Long supplierId;
    private BigDecimal paymentAmount;
    private LocalDate paymentDate;
    private String paymentMethod;
    private String referenceNumber;
    private String notes;
}
