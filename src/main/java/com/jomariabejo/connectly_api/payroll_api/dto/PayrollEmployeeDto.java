package com.jomariabejo.connectly_api.payroll_api.dto;

import com.jomariabejo.connectly_api.payroll_api.entity.PayrollEmployeeStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class PayrollEmployeeDto {
    private Long id;
    private String employeeNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private LocalDate hireDate;
    private PayrollEmployeeStatus status;
}
