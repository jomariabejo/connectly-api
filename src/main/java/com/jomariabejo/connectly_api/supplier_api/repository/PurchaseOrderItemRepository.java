package com.jomariabejo.connectly_api.supplier_api.repository;

import com.jomariabejo.connectly_api.supplier_api.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {
    @Query("SELECT poi FROM PurchaseOrderItem poi WHERE poi.purchaseOrder.id = :poId ORDER BY poi.lineNumber ASC")
    List<PurchaseOrderItem> findByPurchaseOrder(@Param("poId") Long poId);

    @Query("SELECT COUNT(poi) FROM PurchaseOrderItem poi WHERE poi.purchaseOrder.id = :poId AND poi.quantityReceived < poi.quantityOrdered")
    long countPartiallyReceivedItems(@Param("poId") Long poId);
}
