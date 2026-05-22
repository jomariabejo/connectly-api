package com.jomariabejo.connectly_api.payroll_api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreatePayrollEmployeeRequest {
    @NotBlank
    private String employeeNumber;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    private String email;
    private String department;
    private LocalDate hireDate;
    private BigDecimal baseSalary;
    private String currency;
}
