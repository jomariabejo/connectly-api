package com.jomariabejo.connectly_api.crm_api.repository;

import com.jomariabejo.connectly_api.crm_api.entity.UtangPaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UtangPaymentAllocationRepository extends JpaRepository<UtangPaymentAllocation, Long> {

    @Query("SELECT upa FROM UtangPaymentAllocation upa WHERE upa.payment.id = :paymentId")
    List<UtangPaymentAllocation> findByPaymentId(@Param("paymentId") Long paymentId);

    @Query("SELECT upa FROM UtangPaymentAllocation upa WHERE upa.transaction.id = :transactionId")
    List<UtangPaymentAllocation> findByTransactionId(@Param("transactionId") Long transactionId);
}
