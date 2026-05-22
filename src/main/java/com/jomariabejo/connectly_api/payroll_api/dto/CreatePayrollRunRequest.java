package com.jomariabejo.connectly_api.payroll_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreatePayrollRunRequest {
    @NotNull
    private LocalDate periodStart;
    @NotNull
    private LocalDate periodEnd;
}
