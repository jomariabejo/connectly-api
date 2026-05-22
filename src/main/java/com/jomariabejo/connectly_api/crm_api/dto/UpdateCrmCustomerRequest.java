package com.jomariabejo.connectly_api.crm_api.dto;

import com.jomariabejo.connectly_api.crm_api.entity.CrmCustomerStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCrmCustomerRequest {
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private String company;
    private CrmCustomerStatus status;
}
