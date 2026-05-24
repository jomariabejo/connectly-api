package com.jomariabejo.connectly_api.supplier_api.repository;

import com.jomariabejo.connectly_api.supplier_api.entity.SupplierContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierContactRepository extends JpaRepository<SupplierContact, Long> {
    @Query("SELECT sc FROM SupplierContact sc WHERE sc.supplier.id = :supplierId ORDER BY sc.isPrimary DESC, sc.createdAt ASC")
    List<SupplierContact> findBySupplier(@Param("supplierId") Long supplierId);

    @Query("SELECT sc FROM SupplierContact sc WHERE sc.supplier.id = :supplierId AND sc.isPrimary = true")
    Optional<SupplierContact> findPrimaryContact(@Param("supplierId") Long supplierId);

    @Query("SELECT sc FROM SupplierContact sc WHERE sc.tenant.id = :tenantId AND sc.contactType = :type")
    List<SupplierContact> findByTenantAndType(@Param("tenantId") Long tenantId, @Param("type") String type);
}
