package com.jomariabejo.connectly_api.supplier_api.repository;

import com.jomariabejo.connectly_api.supplier_api.entity.PayablesLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PayablesLedgerRepository extends JpaRepository<PayablesLedger, Long> {
    @Query("SELECT pl FROM PayablesLedger pl WHERE pl.supplier.id = :supplierId ORDER BY pl.transactionDate DESC")
    List<PayablesLedger> findBySupplier(@Param("supplierId") Long supplierId);

    @Query("SELECT pl FROM PayablesLedger pl WHERE pl.invoice.id = :invoiceId ORDER BY pl.transactionDate ASC")
    List<PayablesLedger> findByInvoice(@Param("invoiceId") Long invoiceId);

    @Query("SELECT COALESCE(SUM(pl.balance), 0) FROM PayablesLedger pl WHERE pl.supplier.id = :supplierId")
    BigDecimal getTotalPayablesBySupplier(@Param("supplierId") Long supplierId);
}
