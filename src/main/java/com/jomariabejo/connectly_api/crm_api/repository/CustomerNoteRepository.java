package com.jomariabejo.connectly_api.crm_api.repository;

import com.jomariabejo.connectly_api.crm_api.entity.CustomerNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerNoteRepository extends JpaRepository<CustomerNote, Long> {
    List<CustomerNote> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
