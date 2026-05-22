package com.jomariabejo.connectly_api.workforce_api.repository;

import com.jomariabejo.connectly_api.workforce_api.entity.WorkforceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkforceScheduleRepository extends JpaRepository<WorkforceSchedule, Long> {
    List<WorkforceSchedule> findByTenantIdAndScheduledDateBetween(Long tenantId, LocalDate from, LocalDate to);
    long countByTenantId(Long tenantId);
}
