package com.jomariabejo.connectly_api.ticketing_api.repository;

import com.jomariabejo.connectly_api.ticketing_api.entity.Ticket;
import com.jomariabejo.connectly_api.ticketing_api.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Page<Ticket> findByTenantId(Long tenantId, Pageable pageable);
    Page<Ticket> findByTenantIdAndStatus(Long tenantId, TicketStatus status, Pageable pageable);
    long countByTenantId(Long tenantId);
    long countByTenantIdAndStatus(Long tenantId, TicketStatus status);
}
