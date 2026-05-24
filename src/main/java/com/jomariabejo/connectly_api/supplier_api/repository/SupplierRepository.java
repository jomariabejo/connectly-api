package com.jomariabejo.connectly_api.supplier_api.repository;

import com.jomariabejo.connectly_api.supplier_api.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    @Query("SELECT s FROM Supplier s WHERE s.tenant.id = :tenantId AND s.code = :code")
    Optional<Supplier> findByTenantAndCode(@Param("tenantId") Long tenantId, @Param("code") String code);

    @Query("SELECT s FROM Supplier s WHERE s.tenant.id = :tenantId AND s.isActive = true ORDER BY s.name ASC")
    List<Supplier> findActiveByTenant(@Param("tenantId") Long tenantId);

    @Query("SELECT s FROM Supplier s WHERE s.tenant.id = :tenantId ORDER BY s.name ASC")
    List<Supplier> findByTenant(@Param("tenantId") Long tenantId);

    @Query("SELECT s FROM Supplier s WHERE s.tenant.id = :tenantId AND s.supplierType = :type AND s.isActive = true")
    List<Supplier> findByTenantAndType(@Param("tenantId") Long tenantId, @Param("type") String type);
}
