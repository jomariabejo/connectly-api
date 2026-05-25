package com.jomariabejo.connectly_api.purchase_order_api.repository;

import com.jomariabejo.connectly_api.purchase_order_api.entity.PurchaseOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, Long> {
    List<PurchaseOrderLine> findByPurchaseOrderId(Long purchaseOrderId);
}
