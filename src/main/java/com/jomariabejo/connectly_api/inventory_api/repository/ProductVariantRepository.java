package com.jomariabejo.connectly_api.inventory_api.repository;

import com.jomariabejo.connectly_api.inventory_api.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    @Query("SELECT pv FROM ProductVariant pv WHERE pv.tenant.id = :tenantId AND pv.sku = :sku")
    Optional<ProductVariant> findByTenantAndSku(@Param("tenantId") Long tenantId, @Param("sku") String sku);

    @Query("SELECT pv FROM ProductVariant pv WHERE pv.tenant.id = :tenantId AND pv.parentProduct.id = :parentProductId ORDER BY pv.name ASC")
    List<ProductVariant> findByTenantAndParentProduct(@Param("tenantId") Long tenantId, @Param("parentProductId") Long parentProductId);

    @Query("SELECT pv FROM ProductVariant pv WHERE pv.tenant.id = :tenantId AND pv.status = 'ACTIVE' ORDER BY pv.name ASC")
    List<ProductVariant> findActiveByTenant(@Param("tenantId") Long tenantId);

    @Query("SELECT pv FROM ProductVariant pv WHERE pv.tenant.id = :tenantId AND pv.status = 'ACTIVE' AND pv.reorderLevel IS NOT NULL AND (pv.onHandQuantity - pv.reservedQuantity) <= pv.reorderLevel")
    List<ProductVariant> findLowStockVariants(@Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(pv) > 0 FROM ProductVariant pv WHERE pv.tenant.id = :tenantId AND pv.parentProduct.id = :parentProductId")
    boolean hasVariants(@Param("tenantId") Long tenantId, @Param("parentProductId") Long parentProductId);
}
