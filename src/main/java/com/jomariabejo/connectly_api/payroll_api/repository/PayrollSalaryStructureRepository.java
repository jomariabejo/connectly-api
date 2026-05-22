package com.jomariabejo.connectly_api.payroll_api.repository;

import com.jomariabejo.connectly_api.payroll_api.entity.PayrollSalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayrollSalaryStructureRepository extends JpaRepository<PayrollSalaryStructure, Long> {
    Optional<PayrollSalaryStructure> findFirstByEmployeeIdAndActiveTrueOrderByEffectiveFromDesc(Long employeeId);
}
