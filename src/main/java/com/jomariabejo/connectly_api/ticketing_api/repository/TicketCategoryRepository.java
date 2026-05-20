package com.jomariabejo.connectly_api.ticketing_api.repository;

import com.jomariabejo.connectly_api.ticketing_api.entity.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long> {
    List<TicketCategory> findByTenantId(Long tenantId);
}
