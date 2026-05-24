package com.jomariabejo.connectly_api.crm_api.repository;

import com.jomariabejo.connectly_api.crm_api.entity.CustomerCreditScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerCreditScoreRepository extends JpaRepository<CustomerCreditScore, Long> {

    @Query("SELECT ccs FROM CustomerCreditScore ccs WHERE ccs.tenant.id = :tenantId AND ccs.customer.id = :customerId")
    Optional<CustomerCreditScore> findByTenantAndCustomer(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId);
}
