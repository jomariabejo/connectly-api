package com.jomariabejo.connectly_api.crm_api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCrmNoteRequest {
    @NotBlank
    private String content;
}
