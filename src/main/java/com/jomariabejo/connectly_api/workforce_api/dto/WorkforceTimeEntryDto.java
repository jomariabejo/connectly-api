package com.jomariabejo.connectly_api.workforce_api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class WorkforceTimeEntryDto {
    private Long id;
    private Long employeeId;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    private String notes;
}
