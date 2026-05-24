package com.jomariabejo.connectly_api.crm_api.repository;

import com.jomariabejo.connectly_api.crm_api.entity.UtangReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UtangReminderRepository extends JpaRepository<UtangReminder, Long> {

    @Query("SELECT ur FROM UtangReminder ur WHERE ur.tenant.id = :tenantId AND ur.status = :status ORDER BY ur.reminderDate ASC")
    List<UtangReminder> findByTenantAndStatus(@Param("tenantId") Long tenantId, @Param("status") String status);

    @Query("SELECT ur FROM UtangReminder ur WHERE ur.tenant.id = :tenantId AND ur.customer.id = :customerId ORDER BY ur.reminderDate DESC")
    List<UtangReminder> findByTenantAndCustomer(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId);

    @Query("SELECT ur FROM UtangReminder ur WHERE ur.tenant.id = :tenantId AND ur.reminderDate <= :now AND ur.status = 'PENDING'")
    List<UtangReminder> findDueReminders(@Param("tenantId") Long tenantId, @Param("now") LocalDateTime now);
}
