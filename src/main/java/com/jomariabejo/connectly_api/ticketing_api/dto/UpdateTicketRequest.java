package com.jomariabejo.connectly_api.ticketing_api.dto;

import com.jomariabejo.connectly_api.ticketing_api.entity.TicketPriority;
import com.jomariabejo.connectly_api.ticketing_api.entity.TicketStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTicketRequest {
    private String subject;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private Long categoryId;
    private Long assigneeId;
}
