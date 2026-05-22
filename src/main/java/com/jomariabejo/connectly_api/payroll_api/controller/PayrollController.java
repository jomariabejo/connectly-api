package com.jomariabejo.connectly_api.payroll_api.controller;

import com.jomariabejo.connectly_api.payroll_api.dto.*;
import com.jomariabejo.connectly_api.payroll_api.service.PayrollService;
import com.jomariabejo.connectly_api.tenant_api.annotation.RequiresProduct;
import com.jomariabejo.connectly_api.tenant_api.entity.ProductCode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/payroll")
@RequiresProduct(ProductCode.PAYROLL)
public class PayrollController {
    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @GetMapping("/employees")
    public ResponseEntity<List<PayrollEmployeeDto>> listEmployees() {
        return ResponseEntity.ok(payrollService.listEmployees());
    }

    @PostMapping("/employees")
    public ResponseEntity<PayrollEmployeeDto> createEmployee(@RequestBody @Valid CreatePayrollEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payrollService.createEmployee(request));
    }

    @GetMapping("/runs")
    public ResponseEntity<List<PayrollRunDto>> listRuns() {
        return ResponseEntity.ok(payrollService.listRuns());
    }

    @PostMapping("/runs")
    public ResponseEntity<PayrollRunDto> createRun(@RequestBody @Valid CreatePayrollRunRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payrollService.createRun(request));
    }

    @PostMapping("/runs/{id}/process")
    public ResponseEntity<PayrollRunDto> processRun(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.processRun(id));
    }
}
