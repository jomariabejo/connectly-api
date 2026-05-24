package com.jomariabejo.connectly_api.crm_api.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCreditLineRequest {
    private Long customerId;
    private BigDecimal creditLimit;
    private String notes;
}
