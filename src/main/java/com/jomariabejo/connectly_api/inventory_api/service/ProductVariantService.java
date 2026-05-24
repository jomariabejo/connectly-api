package com.jomariabejo.connectly_api.inventory_api.service;

import com.jomariabejo.connectly_api.inventory_api.dto.*;
import com.jomariabejo.connectly_api.inventory_api.entity.*;
import com.jomariabejo.connectly_api.inventory_api.repository.*;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import com.jomariabejo.connectly_api.tenant_api.service.PricingService;
import com.jomariabejo.connectly_api.tenant_api.service.AuditLoggingService;
import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import com.jomariabejo.connectly_api.tenant_api.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final VariantPricingRepository pricingRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final TenantContextService tenantContextService;
    private final PricingService pricingService;
    private final AuditLoggingService auditService;
    private final TenantRepository tenantRepository;

    /**
     * Create a product variant
     */
    @Transactional
    public ProductVariantDto createVariant(CreateProductVariantRequest request, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();

        // Get parent product
        InventoryItem parentProduct = inventoryItemRepository.findById(request.getParentProductId())
                .orElseThrow(() -> new IllegalStateException("Parent product not found"));
        
        // Verify tenant access
        if (!parentProduct.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Parent product does not belong to this tenant");
        }

        // Check if variant sku already exists
        var existingVariant = variantRepository.findByTenantAndSku(tenantId, request.getSku());
        if (existingVariant.isPresent()) {
            throw new IllegalStateException("Variant SKU already exists: " + request.getSku());
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));

        // Create variant
        ProductVariant variant = ProductVariant.builder()
                .tenant(tenant)
                .parentProduct(parentProduct)
                .sku(request.getSku())
                .name(request.getName())
                .unitType(request.getUnitType())
                .quantityPerUnit(request.getQuantityPerUnit())
                .retailPrice(request.getRetailPrice())
                .wholesalePrice(request.getWholesalePrice())
                .reorderLevel(request.getReorderLevel())
                .notes(request.getNotes())
                .status("ACTIVE")
                .build();

        variant = variantRepository.save(variant);

        // Audit log
        auditService.logEvent(tenantId, userId, "CREATE", "ProductVariant", variant.getId(), null,
                Map.of("sku", request.getSku(), "name", request.getName(), "parentProductId", request.getParentProductId()),
                null, null);

        log.info("Created product variant: {} with SKU: {} for parent product: {}", variant.getId(), request.getSku(), request.getParentProductId());

        return mapToDto(variant);
    }

    /**
     * Get variant by ID
     */
    public ProductVariantDto getVariant(Long variantId) {
        Long tenantId = tenantContextService.requireTenantId();

        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalStateException("Variant not found"));

        if (!variant.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Variant does not belong to this tenant");
        }

        return mapToDto(variant);
    }

    /**
     * Get all variants for a parent product
     */
    public List<ProductVariantDto> getVariantsByParentProduct(Long parentProductId) {
        Long tenantId = tenantContextService.requireTenantId();

        List<ProductVariant> variants = variantRepository.findByTenantAndParentProduct(tenantId, parentProductId);

        return variants.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get product variant details with pricing tiers
     */
    public ProductVariantDetailDto getVariantDetail(Long variantId) {
        Long tenantId = tenantContextService.requireTenantId();

        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalStateException("Variant not found"));

        if (!variant.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Variant does not belong to this tenant");
        }

        List<VariantPricing> pricings = pricingRepository.findByVariantId(variantId);
        List<VariantPricingDto> pricingDtos = pricings.stream()
                .map(this::mapPricingToDto)
                .collect(Collectors.toList());

        return ProductVariantDetailDto.builder()
                .id(variant.getId())
                .parentProductId(variant.getParentProduct().getId())
                .sku(variant.getSku())
                .name(variant.getName())
                .unitType(variant.getUnitType())
                .quantityPerUnit(variant.getQuantityPerUnit())
                .retailPrice(variant.getRetailPrice())
                .wholesalePrice(variant.getWholesalePrice())
                .onHandQuantity(variant.getOnHandQuantity())
                .reservedQuantity(variant.getReservedQuantity())
                .availableQuantity(variant.getAvailableQuantity())
                .reorderLevel(variant.getReorderLevel())
                .status(variant.getStatus())
                .imageKey(variant.getImageKey())
                .notes(variant.getNotes())
                .pricingTiers(pricingDtos)
                .build();
    }

    /**
     * Update variant
     */
    @Transactional
    public ProductVariantDto updateVariant(Long variantId, CreateProductVariantRequest request, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();

        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalStateException("Variant not found"));

        if (!variant.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Variant does not belong to this tenant");
        }

        // Store old values for audit
        Map<String, Object> oldValues = Map.of(
                "name", variant.getName(),
                "retailPrice", variant.getRetailPrice(),
                "wholesalePrice", variant.getWholesalePrice() != null ? variant.getWholesalePrice() : "NULL"
        );

        // Update variant
        variant.setName(request.getName());
        variant.setUnitType(request.getUnitType());
        variant.setQuantityPerUnit(request.getQuantityPerUnit());
        variant.setRetailPrice(request.getRetailPrice());
        variant.setWholesalePrice(request.getWholesalePrice());
        variant.setReorderLevel(request.getReorderLevel());
        variant.setNotes(request.getNotes());

        variant = variantRepository.save(variant);

        // Audit log
        auditService.logEvent(tenantId, userId, "UPDATE", "ProductVariant", variant.getId(), oldValues,
                Map.of("name", variant.getName(), "retailPrice", variant.getRetailPrice()),
                null, null);

        return mapToDto(variant);
    }

    /**
     * Add pricing tier for variant
     */
    @Transactional
    public VariantPricingDto addPricingTier(CreateVariantPricingRequest request, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();

        ProductVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new IllegalStateException("Variant not found"));

        if (!variant.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Variant does not belong to this tenant");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));

        // Create pricing
        VariantPricing pricing = VariantPricing.builder()
                .tenant(tenant)
                .variant(variant)
                .quantityThreshold(request.getQuantityThreshold())
                .price(request.getPrice())
                .priceType(request.getPriceType())
                .notes(request.getNotes())
                .build();

        pricing = pricingRepository.save(pricing);

        // Audit log
        auditService.logEvent(tenantId, userId, "CREATE", "VariantPricing", pricing.getId(), null,
                Map.of("variantId", request.getVariantId(), "quantity", request.getQuantityThreshold(), "price", request.getPrice()),
                null, null);

        return mapPricingToDto(pricing);
    }

    /**
     * Get best price for variant at given quantity
     */
    public BigDecimal getPriceForQuantity(Long variantId, BigDecimal quantity) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalStateException("Variant not found"));

        // Try to find a pricing tier that matches the quantity
        var pricingTier = pricingRepository.findBestPriceForQuantity(variantId, quantity);
        if (pricingTier.isPresent()) {
            return pricingTier.get().getPrice();
        }

        // Default to retail price
        return variant.getRetailPrice();
    }

    /**
     * Get low stock variants
     */
    public List<ProductVariantDto> getLowStockVariants() {
        Long tenantId = tenantContextService.requireTenantId();

        List<ProductVariant> variants = variantRepository.findLowStockVariants(tenantId);

        return variants.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all active variants
     */
    public List<ProductVariantDto> getAllActiveVariants() {
        Long tenantId = tenantContextService.requireTenantId();

        List<ProductVariant> variants = variantRepository.findActiveByTenant(tenantId);

        return variants.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // Mapping methods
    private ProductVariantDto mapToDto(ProductVariant variant) {
        return ProductVariantDto.builder()
                .id(variant.getId())
                .parentProductId(variant.getParentProduct().getId())
                .sku(variant.getSku())
                .name(variant.getName())
                .unitType(variant.getUnitType())
                .quantityPerUnit(variant.getQuantityPerUnit())
                .retailPrice(variant.getRetailPrice())
                .wholesalePrice(variant.getWholesalePrice())
                .onHandQuantity(variant.getOnHandQuantity())
                .reservedQuantity(variant.getReservedQuantity())
                .availableQuantity(variant.getAvailableQuantity())
                .reorderLevel(variant.getReorderLevel())
                .status(variant.getStatus())
                .imageKey(variant.getImageKey())
                .notes(variant.getNotes())
                .createdAt(variant.getCreatedAt())
                .updatedAt(variant.getUpdatedAt())
                .build();
    }

    private VariantPricingDto mapPricingToDto(VariantPricing pricing) {
        return VariantPricingDto.builder()
                .id(pricing.getId())
                .variantId(pricing.getVariant().getId())
                .quantityThreshold(pricing.getQuantityThreshold())
                .price(pricing.getPrice())
                .priceType(pricing.getPriceType())
                .notes(pricing.getNotes())
                .build();
    }
}
