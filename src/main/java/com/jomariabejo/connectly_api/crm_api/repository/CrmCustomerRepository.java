package com.jomariabejo.connectly_api.crm_api.repository;

import com.jomariabejo.connectly_api.crm_api.entity.CrmCustomer;
import com.jomariabejo.connectly_api.crm_api.entity.CrmCustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CrmCustomerRepository extends JpaRepository<CrmCustomer, Long> {
    Page<CrmCustomer> findByTenantId(Long tenantId, Pageable pageable);
    Page<CrmCustomer> findByTenantIdAndStatus(Long tenantId, CrmCustomerStatus status, Pageable pageable);
    long countByTenantId(Long tenantId);
}
