package com.jomariabejo.connectly_api.ticketing_api.dto;

import com.jomariabejo.connectly_api.ticketing_api.entity.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTicketRequest {
    @NotBlank
    private String subject;
    private String description;
    private TicketPriority priority;
    private Long categoryId;
    private Long customerId;
    private String requesterEmail;
}
