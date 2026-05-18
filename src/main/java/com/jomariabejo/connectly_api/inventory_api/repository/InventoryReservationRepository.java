package com.jomariabejo.connectly_api.inventory_api.repository;

import com.jomariabejo.connectly_api.inventory_api.entity.InventoryReservation;
import com.jomariabejo.connectly_api.inventory_api.entity.InventoryReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
    List<InventoryReservation> findByOrderId(Long orderId);

    List<InventoryReservation> findByOrderIdAndStatus(Long orderId, InventoryReservationStatus status);

    Optional<InventoryReservation> findByOrderIdAndSku(Long orderId, String sku);
}
