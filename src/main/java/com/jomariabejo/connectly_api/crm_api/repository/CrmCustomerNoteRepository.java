package com.jomariabejo.connectly_api.crm_api.repository;

import com.jomariabejo.connectly_api.crm_api.entity.CrmCustomerNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrmCustomerNoteRepository extends JpaRepository<CrmCustomerNote, Long> {
    List<CrmCustomerNote> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
