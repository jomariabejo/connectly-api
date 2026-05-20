package com.jomariabejo.connectly_api.crm_api.dto;

import com.jomariabejo.connectly_api.crm_api.entity.CrmCustomerStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CrmCustomerDto {
    private Long id;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private String company;
    private CrmCustomerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
