package com.jomariabejo.connectly_api.crm_api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CrmCustomerNoteDto {
    private Long id;
    private Long customerId;
    private String content;
    private String authorEmail;
    private LocalDateTime createdAt;
}
