package com.jomariabejo.connectly_api.crm_api.dto;

import com.jomariabejo.connectly_api.crm_api.entity.CrmCustomerStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCrmCustomerRequest {
    private String email;
    private String phone;
    @NotBlank
    private String firstName;
    private String lastName;
    private String company;
    private CrmCustomerStatus status;
}
