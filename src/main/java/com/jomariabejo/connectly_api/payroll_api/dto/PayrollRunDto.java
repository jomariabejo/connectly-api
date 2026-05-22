package com.jomariabejo.connectly_api.payroll_api.dto;

import com.jomariabejo.connectly_api.payroll_api.entity.PayrollRunStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class PayrollRunDto {
    private Long id;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private PayrollRunStatus status;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
