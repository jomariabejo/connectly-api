package com.jomariabejo.connectly_api.crm_api.repository;

import com.jomariabejo.connectly_api.crm_api.entity.UtangTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UtangTransactionRepository extends JpaRepository<UtangTransaction, Long> {

    @Query("SELECT ut FROM UtangTransaction ut WHERE ut.tenant.id = :tenantId AND ut.customer.id = :customerId AND ut.deletedAt IS NULL ORDER BY ut.createdAt DESC")
    List<UtangTransaction> findByTenantAndCustomer(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId);

    @Query("SELECT ut FROM UtangTransaction ut WHERE ut.tenant.id = :tenantId AND ut.transactionType = :type AND ut.deletedAt IS NULL ORDER BY ut.createdAt DESC")
    List<UtangTransaction> findByTenantAndType(@Param("tenantId") Long tenantId, @Param("type") String type);

    @Query("SELECT ut FROM UtangTransaction ut WHERE ut.tenant.id = :tenantId AND ut.customer.id = :customerId AND ut.transactionType = 'DEBIT' AND ut.deletedAt IS NULL")
    List<UtangTransaction> findOutstandingDebitsByCustomer(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId);

    @Query("SELECT COALESCE(SUM(ut.amount), 0) FROM UtangTransaction ut WHERE ut.tenant.id = :tenantId AND ut.customer.id = :customerId AND ut.transactionType = 'DEBIT' AND ut.deletedAt IS NULL")
    BigDecimal getTotalOutstandingAmount(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId);

    @Query("SELECT ut FROM UtangTransaction ut WHERE ut.tenant.id = :tenantId AND ut.dueDate < :dueDate AND ut.transactionType = 'DEBIT' AND ut.deletedAt IS NULL")
    List<UtangTransaction> findOverdueTransactions(@Param("tenantId") Long tenantId, @Param("dueDate") LocalDateTime dueDate);

    @Query("SELECT ut FROM UtangTransaction ut WHERE ut.orderId = :orderId AND ut.deletedAt IS NULL")
    List<UtangTransaction> findByOrderId(@Param("orderId") Long orderId);
}
