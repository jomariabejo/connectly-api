package com.jomariabejo.connectly_api.workforce_api.repository;

import com.jomariabejo.connectly_api.workforce_api.entity.WorkforceTimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkforceTimeEntryRepository extends JpaRepository<WorkforceTimeEntry, Long> {
    Optional<WorkforceTimeEntry> findFirstByTenantIdAndEmployeeIdAndClockOutIsNullOrderByClockInDesc(Long tenantId, Long employeeId);
}
