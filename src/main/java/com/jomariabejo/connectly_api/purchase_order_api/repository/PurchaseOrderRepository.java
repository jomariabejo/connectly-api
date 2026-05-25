package com.jomariabejo.connectly_api.purchase_order_api.repository;

import com.jomariabejo.connectly_api.purchase_order_api.entity.PurchaseOrder;
import com.jomariabejo.connectly_api.purchase_order_api.entity.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    @EntityGraph(attributePaths = {"lines", "createdBy"})
    Optional<PurchaseOrder> findByTenantIdAndId(Long tenantId, Long id);

    @EntityGraph(attributePaths = "lines")
    List<PurchaseOrder> findTop50ByTenantIdOrderByCreatedDateDesc(Long tenantId);

    List<PurchaseOrder> findByTenantIdAndStatusInOrderByCreatedDateDesc(
            Long tenantId, List<PurchaseOrderStatus> statuses);

    boolean existsByTenantIdAndPoNumber(Long tenantId, String poNumber);

    long countByTenantIdAndPoNumberStartingWith(Long tenantId, String prefix);
}
