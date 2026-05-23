package com.jomariabejo.connectly_api.crm_api.repository;

import com.jomariabejo.connectly_api.crm_api.entity.UtangPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UtangPaymentRepository extends JpaRepository<UtangPayment, Long> {

    @Query("SELECT up FROM UtangPayment up WHERE up.tenant.id = :tenantId AND up.customer.id = :customerId AND up.deletedAt IS NULL ORDER BY up.paymentDate DESC")
    List<UtangPayment> findByTenantAndCustomer(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId);

    @Query("SELECT COALESCE(SUM(up.amount), 0) FROM UtangPayment up WHERE up.tenant.id = :tenantId AND up.customer.id = :customerId AND up.deletedAt IS NULL")
    BigDecimal getTotalPaymentsByCustomer(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId);

    @Query("SELECT up FROM UtangPayment up WHERE up.tenant.id = :tenantId AND up.paymentDate >= :fromDate AND up.paymentDate <= :toDate AND up.deletedAt IS NULL ORDER BY up.paymentDate DESC")
    List<UtangPayment> findPaymentsByDateRange(@Param("tenantId") Long tenantId, @Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);
}
