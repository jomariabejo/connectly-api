package com.jomariabejo.connectly_api.supplier_api.repository;

import com.jomariabejo.connectly_api.supplier_api.entity.GoodsReceiptNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsReceiptNoteRepository extends JpaRepository<GoodsReceiptNote, Long> {
    @Query("SELECT grn FROM GoodsReceiptNote grn WHERE grn.tenant.id = :tenantId AND grn.grnNumber = :grnNumber")
    Optional<GoodsReceiptNote> findByTenantAndGrnNumber(@Param("tenantId") Long tenantId, @Param("grnNumber") String grnNumber);

    @Query("SELECT grn FROM GoodsReceiptNote grn WHERE grn.purchaseOrder.id = :poId ORDER BY grn.receiptDate DESC")
    List<GoodsReceiptNote> findByPurchaseOrder(@Param("poId") Long poId);

    @Query("SELECT grn FROM GoodsReceiptNote grn WHERE grn.tenant.id = :tenantId AND grn.status = :status ORDER BY grn.receiptDate DESC")
    List<GoodsReceiptNote> findByTenantAndStatus(@Param("tenantId") Long tenantId, @Param("status") String status);
}
