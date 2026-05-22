package com.jomariabejo.connectly_api.payroll_api.service;

import com.jomariabejo.connectly_api.payroll_api.dto.*;
import com.jomariabejo.connectly_api.payroll_api.entity.*;
import com.jomariabejo.connectly_api.payroll_api.repository.*;
import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PayrollService {
    private final PayrollEmployeeRepository employeeRepository;
    private final PayrollSalaryStructureRepository salaryRepository;
    private final PayrollRunRepository runRepository;
    private final PayrollPayslipRepository payslipRepository;
    private final TenantContextService tenantContextService;

    public PayrollService(
            PayrollEmployeeRepository employeeRepository,
            PayrollSalaryStructureRepository salaryRepository,
            PayrollRunRepository runRepository,
            PayrollPayslipRepository payslipRepository,
            TenantContextService tenantContextService) {
        this.employeeRepository = employeeRepository;
        this.salaryRepository = salaryRepository;
        this.runRepository = runRepository;
        this.payslipRepository = payslipRepository;
        this.tenantContextService = tenantContextService;
    }

    @Transactional(readOnly = true)
    public List<PayrollEmployeeDto> listEmployees() {
        Long tenantId = tenantContextService.requireTenantId();
        return employeeRepository.findByTenantId(tenantId).stream().map(this::toEmployeeDto).toList();
    }

    @Transactional
    public PayrollEmployeeDto createEmployee(CreatePayrollEmployeeRequest request) {
        Tenant tenant = tenantContextService.requireTenant();
        PayrollEmployee employee = PayrollEmployee.builder()
                .tenant(tenant)
                .employeeNumber(request.getEmployeeNumber())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .department(request.getDepartment())
                .hireDate(request.getHireDate())
                .build();
        PayrollEmployee saved = employeeRepository.save(employee);
        if (request.getBaseSalary() != null) {
            PayrollSalaryStructure salary = PayrollSalaryStructure.builder()
                    .employee(saved)
                    .baseSalary(request.getBaseSalary())
                    .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                    .effectiveFrom(LocalDate.now())
                    .build();
            salaryRepository.save(salary);
        }
        return toEmployeeDto(saved);
    }

    @Transactional(readOnly = true)
    public List<PayrollRunDto> listRuns() {
        Long tenantId = tenantContextService.requireTenantId();
        return runRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toRunDto)
                .toList();
    }

    @Transactional
    public PayrollRunDto createRun(CreatePayrollRunRequest request) {
        Tenant tenant = tenantContextService.requireTenant();
        PayrollRun run = PayrollRun.builder()
                .tenant(tenant)
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .status(PayrollRunStatus.DRAFT)
                .build();
        return toRunDto(runRepository.save(run));
    }

    @Transactional
    public PayrollRunDto processRun(Long runId) {
        Long tenantId = tenantContextService.requireTenantId();
        PayrollRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll run not found"));
        if (!run.getTenant().getId().equals(tenantId)) {
            throw new IllegalArgumentException("Payroll run not found");
        }
        List<PayrollEmployee> employees = employeeRepository.findByTenantId(tenantId);
        for (PayrollEmployee employee : employees) {
            BigDecimal gross = salaryRepository.findFirstByEmployeeIdAndActiveTrueOrderByEffectiveFromDesc(employee.getId())
                    .map(PayrollSalaryStructure::getBaseSalary)
                    .orElse(BigDecimal.ZERO);
            PayrollPayslip payslip = PayrollPayslip.builder()
                    .payrollRun(run)
                    .employee(employee)
                    .grossPay(gross)
                    .totalDeductions(BigDecimal.ZERO)
                    .netPay(gross)
                    .build();
            payslipRepository.save(payslip);
        }
        run.setStatus(PayrollRunStatus.COMPLETED);
        run.setProcessedAt(LocalDateTime.now());
        return toRunDto(runRepository.save(run));
    }

    public long countEmployees(Long tenantId) {
        return employeeRepository.countByTenantId(tenantId);
    }

    public long countPayrollRuns(Long tenantId) {
        return runRepository.countByTenantId(tenantId);
    }

    private PayrollEmployeeDto toEmployeeDto(PayrollEmployee e) {
        return PayrollEmployeeDto.builder()
                .id(e.getId())
                .employeeNumber(e.getEmployeeNumber())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .email(e.getEmail())
                .department(e.getDepartment())
                .hireDate(e.getHireDate())
                .status(e.getStatus())
                .build();
    }

    private PayrollRunDto toRunDto(PayrollRun r) {
        return PayrollRunDto.builder()
                .id(r.getId())
                .periodStart(r.getPeriodStart())
                .periodEnd(r.getPeriodEnd())
                .status(r.getStatus())
                .processedAt(r.getProcessedAt())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
