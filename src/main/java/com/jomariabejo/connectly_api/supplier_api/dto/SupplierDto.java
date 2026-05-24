package com.jomariabejo.connectly_api.supplier_api.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierDto {
    private Long id;
    private String code;
    private String name;
    private String email;
    private String phone;
    private String city;
    private String province;
    private String paymentTerms;
    private String currencyCode;
    private String supplierType;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
