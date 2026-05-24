package com.jomariabejo.connectly_api.supplier_api.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSupplierRequest {
    private String code;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String province;
    private String postalCode;
    private String country;
    private String paymentTerms;
    private String supplierType;
    private String taxId;
    private String notes;
}
