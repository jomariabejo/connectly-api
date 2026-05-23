package com.jomariabejo.connectly_api.supplier_api.service;

import com.jomariabejo.connectly_api.supplier_api.dto.CreateSupplierRequest;
import com.jomariabejo.connectly_api.supplier_api.dto.SupplierDto;
import com.jomariabejo.connectly_api.supplier_api.entity.Supplier;
import com.jomariabejo.connectly_api.supplier_api.repository.SupplierRepository;
import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import com.jomariabejo.connectly_api.tenant_api.repository.TenantRepository;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import com.jomariabejo.connectly_api.tenant_api.service.AuditLoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final TenantContextService tenantContextService;
    private final AuditLoggingService auditService;
    private final TenantRepository tenantRepository;

    /**
     * Create a supplier
     */
    @Transactional
    public SupplierDto createSupplier(CreateSupplierRequest request, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();

        // Check if supplier code already exists
        var existingSupplier = supplierRepository.findByTenantAndCode(tenantId, request.getCode());
        if (existingSupplier.isPresent()) {
            throw new IllegalStateException("Supplier code already exists: " + request.getCode());
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));

        Supplier supplier = Supplier.builder()
                .tenant(tenant)
                .code(request.getCode())
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .city(request.getCity())
                .province(request.getProvince())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .paymentTerms(request.getPaymentTerms())
                .supplierType(request.getSupplierType())
                .taxId(request.getTaxId())
                .notes(request.getNotes())
                .isActive(true)
                .build();

        supplier = supplierRepository.save(supplier);

        // Audit log
        auditService.logEvent(tenantId, userId, "CREATE", "Supplier", supplier.getId(), null,
                Map.of("code", request.getCode(), "name", request.getName()),
                null, null);

        log.info("Created supplier: {} with code: {}", supplier.getId(), request.getCode());

        return mapToDto(supplier);
    }

    /**
     * Get supplier by ID
     */
    public SupplierDto getSupplier(Long supplierId) {
        Long tenantId = tenantContextService.requireTenantId();

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalStateException("Supplier not found"));

        if (!supplier.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Supplier does not belong to this tenant");
        }

        return mapToDto(supplier);
    }

    /**
     * Get all active suppliers
     */
    public List<SupplierDto> getAllActiveSuppliers() {
        Long tenantId = tenantContextService.requireTenantId();

        List<Supplier> suppliers = supplierRepository.findActiveByTenant(tenantId);
        return suppliers.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all suppliers
     */
    public List<SupplierDto> getAllSuppliers() {
        Long tenantId = tenantContextService.requireTenantId();

        List<Supplier> suppliers = supplierRepository.findByTenant(tenantId);
        return suppliers.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Update supplier
     */
    @Transactional
    public SupplierDto updateSupplier(Long supplierId, CreateSupplierRequest request, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalStateException("Supplier not found"));

        if (!supplier.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Supplier does not belong to this tenant");
        }

        // Store old values for audit
        Map<String, Object> oldValues = Map.of(
                "name", supplier.getName(),
                "email", supplier.getEmail() != null ? supplier.getEmail() : "NULL",
                "isActive", supplier.getIsActive()
        );

        // Update supplier
        supplier.setName(request.getName());
        supplier.setEmail(request.getEmail());
        supplier.setPhone(request.getPhone());
        supplier.setAddress(request.getAddress());
        supplier.setCity(request.getCity());
        supplier.setProvince(request.getProvince());
        supplier.setPostalCode(request.getPostalCode());
        supplier.setCountry(request.getCountry());
        supplier.setPaymentTerms(request.getPaymentTerms());
        supplier.setSupplierType(request.getSupplierType());
        supplier.setTaxId(request.getTaxId());
        supplier.setNotes(request.getNotes());

        supplier = supplierRepository.save(supplier);

        // Audit log
        auditService.logEvent(tenantId, userId, "UPDATE", "Supplier", supplier.getId(), oldValues,
                Map.of("name", supplier.getName()),
                null, null);

        return mapToDto(supplier);
    }

    /**
     * Deactivate supplier
     */
    @Transactional
    public void deactivateSupplier(Long supplierId, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalStateException("Supplier not found"));

        if (!supplier.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Supplier does not belong to this tenant");
        }

        supplier.setIsActive(false);
        supplierRepository.save(supplier);

        // Audit log
        auditService.logEvent(tenantId, userId, "UPDATE", "Supplier", supplier.getId(), 
                Map.of("isActive", true),
                Map.of("isActive", false),
                null, null);

        log.info("Deactivated supplier: {}", supplierId);
    }

    // Mapping method
    private SupplierDto mapToDto(Supplier supplier) {
        return SupplierDto.builder()
                .id(supplier.getId())
                .code(supplier.getCode())
                .name(supplier.getName())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .city(supplier.getCity())
                .province(supplier.getProvince())
                .paymentTerms(supplier.getPaymentTerms())
                .currencyCode(supplier.getCurrencyCode())
                .supplierType(supplier.getSupplierType())
                .isActive(supplier.getIsActive())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
}
