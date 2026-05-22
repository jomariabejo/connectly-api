package com.jomariabejo.connectly_api.workforce_api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class WorkforceScheduleDto {
    private Long id;
    private Long employeeId;
    private String employeeEmail;
    private Long shiftId;
    private String shiftName;
    private LocalDate scheduledDate;
    private String notes;
}
