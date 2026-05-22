package com.jomariabejo.connectly_api.ticketing_api.dto;

import com.jomariabejo.connectly_api.ticketing_api.entity.TicketPriority;
import com.jomariabejo.connectly_api.ticketing_api.entity.TicketStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TicketDto {
    private Long id;
    private String subject;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private Long categoryId;
    private String categoryName;
    private Long customerId;
    private Long assigneeId;
    private String requesterEmail;
    private LocalDateTime dueAt;
    private LocalDateTime createdAt;
}
