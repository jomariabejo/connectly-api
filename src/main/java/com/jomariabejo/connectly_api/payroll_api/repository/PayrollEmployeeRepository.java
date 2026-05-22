package com.jomariabejo.connectly_api.payroll_api.repository;

import com.jomariabejo.connectly_api.payroll_api.entity.PayrollEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayrollEmployeeRepository extends JpaRepository<PayrollEmployee, Long> {
    List<PayrollEmployee> findByTenantId(Long tenantId);
    long countByTenantId(Long tenantId);
}
