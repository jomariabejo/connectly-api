package com.jomariabejo.connectly_api.crm_api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateCrmInteractionRequest {
    @NotBlank
    private String interactionType;
    private String subject;
    private String description;
    private LocalDateTime occurredAt;
}
