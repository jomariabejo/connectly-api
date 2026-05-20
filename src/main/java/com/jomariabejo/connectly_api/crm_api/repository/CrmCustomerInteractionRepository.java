package com.jomariabejo.connectly_api.crm_api.repository;

import com.jomariabejo.connectly_api.crm_api.entity.CrmCustomerInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrmCustomerInteractionRepository extends JpaRepository<CrmCustomerInteraction, Long> {
    List<CrmCustomerInteraction> findByCustomerIdOrderByOccurredAtDesc(Long customerId);
}
