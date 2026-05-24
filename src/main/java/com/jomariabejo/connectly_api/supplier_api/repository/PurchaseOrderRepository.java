package com.jomariabejo.connectly_api.supplier_api.repository;

import com.jomariabejo.connectly_api.supplier_api.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    @Query("SELECT po FROM PurchaseOrder po WHERE po.tenant.id = :tenantId AND po.poNumber = :poNumber")
    Optional<PurchaseOrder> findByTenantAndPoNumber(@Param("tenantId") Long tenantId, @Param("poNumber") String poNumber);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.tenant.id = :tenantId AND po.status = :status ORDER BY po.createdAt DESC")
    List<PurchaseOrder> findByTenantAndStatus(@Param("tenantId") Long tenantId, @Param("status") String status);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.supplier.id = :supplierId AND po.status = :status ORDER BY po.createdAt DESC")
    List<PurchaseOrder> findBySupplierAndStatus(@Param("supplierId") Long supplierId, @Param("status") String status);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.tenant.id = :tenantId AND po.createdAt >= :startDate AND po.createdAt <= :endDate ORDER BY po.createdAt DESC")
    List<PurchaseOrder> findByTenantAndDateRange(@Param("tenantId") Long tenantId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(po) FROM PurchaseOrder po WHERE po.tenant.id = :tenantId AND po.status = :status")
    long countByTenantAndStatus(@Param("tenantId") Long tenantId, @Param("status") String status);
}
