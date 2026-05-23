package com.jomariabejo.connectly_api.supplier_api.repository;

import com.jomariabejo.connectly_api.supplier_api.entity.PurchasePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchasePaymentRepository extends JpaRepository<PurchasePayment, Long> {
    @Query("SELECT pp FROM PurchasePayment pp WHERE pp.tenant.id = :tenantId AND pp.paymentNumber = :paymentNumber")
    Optional<PurchasePayment> findByTenantAndPaymentNumber(@Param("tenantId") Long tenantId, @Param("paymentNumber") String paymentNumber);

    @Query("SELECT pp FROM PurchasePayment pp WHERE pp.supplier.id = :supplierId AND pp.paymentDate >= :startDate AND pp.paymentDate <= :endDate ORDER BY pp.paymentDate DESC")
    List<PurchasePayment> findBySupplierAndDateRange(@Param("supplierId") Long supplierId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT pp FROM PurchasePayment pp WHERE pp.tenant.id = :tenantId AND pp.paymentDate = :date ORDER BY pp.createdAt DESC")
    List<PurchasePayment> findByTenantAndDate(@Param("tenantId") Long tenantId, @Param("date") LocalDate date);
}
