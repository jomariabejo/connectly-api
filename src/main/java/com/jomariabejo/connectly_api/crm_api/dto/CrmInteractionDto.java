package com.jomariabejo.connectly_api.crm_api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CrmInteractionDto {
    private Long id;
    private Long customerId;
    private String interactionType;
    private String subject;
    private String description;
    private LocalDateTime occurredAt;
}
