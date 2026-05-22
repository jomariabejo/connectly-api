package com.jomariabejo.connectly_api.workforce_api.repository;

import com.jomariabejo.connectly_api.workforce_api.entity.WorkforceShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkforceShiftRepository extends JpaRepository<WorkforceShift, Long> {
    List<WorkforceShift> findByTenantIdAndActiveTrue(Long tenantId);
}
