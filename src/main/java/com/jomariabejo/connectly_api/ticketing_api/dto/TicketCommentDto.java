package com.jomariabejo.connectly_api.ticketing_api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TicketCommentDto {
    private Long id;
    private Long ticketId;
    private String body;
    private Boolean internalNote;
    private String authorEmail;
    private LocalDateTime createdAt;
}
