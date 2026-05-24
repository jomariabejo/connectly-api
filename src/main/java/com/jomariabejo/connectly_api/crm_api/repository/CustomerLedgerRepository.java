package com.jomariabejo.connectly_api.crm_api.repository;

import com.jomariabejo.connectly_api.crm_api.entity.CustomerLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerLedgerRepository extends JpaRepository<CustomerLedger, Long> {

    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.tenant.id = :tenantId AND cl.customer.id = :customerId")
    Optional<CustomerLedger> findByTenantAndCustomer(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId);

    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.tenant.id = :tenantId AND cl.status = :status")
    List<CustomerLedger> findByTenantAndStatus(@Param("tenantId") Long tenantId, @Param("status") String status);

    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.tenant.id = :tenantId AND cl.totalOutstanding > 0 ORDER BY cl.totalOutstanding DESC")
    List<CustomerLedger> findTopDebtorsByTenant(@Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(cl) > 0 FROM CustomerLedger cl WHERE cl.tenant.id = :tenantId AND cl.customer.id = :customerId AND cl.status = 'ACTIVE'")
    boolean hasActiveCreditLine(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId);
}
