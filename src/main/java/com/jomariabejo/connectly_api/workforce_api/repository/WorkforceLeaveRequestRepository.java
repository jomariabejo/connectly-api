package com.jomariabejo.connectly_api.workforce_api.repository;

import com.jomariabejo.connectly_api.workforce_api.entity.LeaveStatus;
import com.jomariabejo.connectly_api.workforce_api.entity.WorkforceLeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkforceLeaveRequestRepository extends JpaRepository<WorkforceLeaveRequest, Long> {
    long countByTenantIdAndStatus(Long tenantId, LeaveStatus status);
}
