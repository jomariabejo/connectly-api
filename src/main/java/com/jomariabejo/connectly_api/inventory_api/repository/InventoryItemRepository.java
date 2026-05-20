package com.jomariabejo.connectly_api.inventory_api.repository;

import com.jomariabejo.connectly_api.inventory_api.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findBySku(String sku);

    Optional<InventoryItem> findBySkuAndActiveTrue(String sku);

    Optional<InventoryItem> findByTenantIdAndSku(Long tenantId, String sku);

    Optional<InventoryItem> findByTenantIdAndSkuAndActiveTrue(Long tenantId, String sku);

    List<InventoryItem> findByActiveTrueOrderByNameAsc();

    List<InventoryItem> findByTenantIdAndActiveTrueOrderByNameAsc(Long tenantId);

    boolean existsBySku(String sku);

    boolean existsByTenantIdAndSku(Long tenantId, String sku);

    long countByTenantIdAndActiveTrue(Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InventoryItem> findWithLockBySku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InventoryItem> findWithLockByTenantIdAndSku(Long tenantId, String sku);
}
