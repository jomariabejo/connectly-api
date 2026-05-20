package com.jomariabejo.connectly_api.ticketing_api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTicketCommentRequest {
    @NotBlank
    private String body;
    private Boolean internalNote;
}
