package com.jomariabejo.connectly_api.supplier_api.repository;

import com.jomariabejo.connectly_api.supplier_api.entity.PurchaseInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseInvoiceRepository extends JpaRepository<PurchaseInvoice, Long> {
    @Query("SELECT pi FROM PurchaseInvoice pi WHERE pi.tenant.id = :tenantId AND pi.invoiceNumber = :invoiceNumber")
    Optional<PurchaseInvoice> findByTenantAndInvoiceNumber(@Param("tenantId") Long tenantId, @Param("invoiceNumber") String invoiceNumber);

    @Query("SELECT pi FROM PurchaseInvoice pi WHERE pi.supplier.id = :supplierId AND pi.paymentStatus != 'PAID' ORDER BY pi.dueDate ASC")
    List<PurchaseInvoice> findUnpaidBySupplier(@Param("supplierId") Long supplierId);

    @Query("SELECT pi FROM PurchaseInvoice pi WHERE pi.tenant.id = :tenantId AND pi.paymentStatus = 'OVERDUE' ORDER BY pi.dueDate ASC")
    List<PurchaseInvoice> findOverdueInvoices(@Param("tenantId") Long tenantId);

    @Query("SELECT pi FROM PurchaseInvoice pi WHERE pi.tenant.id = :tenantId AND pi.status = :status ORDER BY pi.invoiceDate DESC")
    List<PurchaseInvoice> findByTenantAndStatus(@Param("tenantId") Long tenantId, @Param("status") String status);
}
