package com.jomariabejo.connectly_api.payroll_api.repository;

import com.jomariabejo.connectly_api.payroll_api.entity.PayrollRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {
    List<PayrollRun> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    long countByTenantId(Long tenantId);
}
