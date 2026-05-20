package com.jomariabejo.connectly_api.workforce_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateScheduleRequest {
    @NotNull
    private Long employeeId;
    private Long shiftId;
    @NotNull
    private LocalDate scheduledDate;
    private String notes;
}
