package com.jomariabejo.connectly_api.payroll_api.repository;

import com.jomariabejo.connectly_api.payroll_api.entity.PayrollPayslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayrollPayslipRepository extends JpaRepository<PayrollPayslip, Long> {
    List<PayrollPayslip> findByPayrollRunId(Long payrollRunId);
}
