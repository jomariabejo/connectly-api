package com.jomariabejo.connectly_api.inventory_api.repository;

import com.jomariabejo.connectly_api.inventory_api.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    List<InventoryMovement> findBySkuOrderByCreatedDateDesc(String sku);
}
