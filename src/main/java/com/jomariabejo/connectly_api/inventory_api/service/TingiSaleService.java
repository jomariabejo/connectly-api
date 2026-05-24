package com.jomariabejo.connectly_api.inventory_api.service;

import com.jomariabejo.connectly_api.inventory_api.dto.TingiSaleDto;
import com.jomariabejo.connectly_api.inventory_api.entity.*;
import com.jomariabejo.connectly_api.inventory_api.repository.*;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import com.jomariabejo.connectly_api.tenant_api.service.AuditLoggingService;
import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import com.jomariabejo.connectly_api.tenant_api.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TingiSaleService {

    private final TingiSaleRepository tingiSaleRepository;
    private final ProductVariantRepository variantRepository;
    private final TenantContextService tenantContextService;
    private final AuditLoggingService auditService;
    private final TenantRepository tenantRepository;

    /**
     * Record a tingi sale (individual units from multipack)
     */
    @Transactional
    public TingiSaleDto recordTingiSale(Long orderId, Long variantId, BigDecimal quantitySold, BigDecimal pricePerUnit, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();

        // Get variant
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalStateException("Variant not found"));

        if (!variant.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Variant does not belong to this tenant");
        }

        // Verify variant has sufficient stock
        if (!variant.isAvailable(quantitySold)) {
            throw new IllegalStateException("Insufficient stock. Available: " + variant.getAvailableQuantity());
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));

        BigDecimal totalAmount = quantitySold.multiply(pricePerUnit);

        // Create tingi sale record
        TingiSale tingiSale = TingiSale.builder()
                .tenant(tenant)
                .orderId(orderId)
                .variant(variant)
                .quantitySold(quantitySold)
                .pricePerUnit(pricePerUnit)
                .totalAmount(totalAmount)
                .build();

        tingiSale = tingiSaleRepository.save(tingiSale);

        // Update variant inventory
        variant.setReservedQuantity(variant.getReservedQuantity().add(quantitySold));
        variantRepository.save(variant);

        // Update parent product inventory
        InventoryItem parentProduct = variant.getParentProduct();
        BigDecimal parentQuantityReduced = quantitySold.multiply(variant.getQuantityPerUnit());
        BigDecimal currentReserved = BigDecimal.valueOf(parentProduct.getReservedQuantity() != null ? parentProduct.getReservedQuantity() : 0);
        parentProduct.setReservedQuantity(currentReserved.add(parentQuantityReduced).intValue());
        // Note: In real implementation, would also reduce on_hand_quantity when payment confirmed

        // Audit log
        auditService.logEvent(tenantId, userId, "CREATE", "TingiSale", tingiSale.getId(), null,
                Map.of("orderId", orderId, "variantId", variantId, "quantity", quantitySold, "amount", totalAmount),
                null, null);

        log.info("Recorded tingi sale: {} for variant: {}, quantity: {}, amount: {}", tingiSale.getId(), variantId, quantitySold, totalAmount);

        return mapToDto(tingiSale);
    }

    /**
     * Get tingi sales for an order
     */
    public List<TingiSaleDto> getTingiSalesByOrder(Long orderId) {
        Long tenantId = tenantContextService.requireTenantId();

        List<TingiSale> sales = tingiSaleRepository.findByTenantAndOrderId(tenantId, orderId);

        return sales.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get tingi sales for a variant
     */
    public List<TingiSaleDto> getTingiSalesByVariant(Long variantId) {
        Long tenantId = tenantContextService.requireTenantId();

        List<TingiSale> sales = tingiSaleRepository.findByTenantAndVariantId(tenantId, variantId);

        return sales.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get total tingi sales for date range
     */
    public BigDecimal getTotalSalesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Long tenantId = tenantContextService.requireTenantId();

        return tingiSaleRepository.getTotalSalesByDateRange(tenantId, startDate, endDate);
    }

    /**
     * Get top selling variants for date range
     */
    public List<Map<String, Object>> getTopSellingVariantsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Long tenantId = tenantContextService.requireTenantId();

        List<Object[]> results = tingiSaleRepository.getTopSellingVariantsByDateRange(tenantId, startDate, endDate);

        return results.stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("variantId", row[0]);
                    map.put("totalQuantitySold", row[1]);
                    return map;
                })
                .collect(Collectors.toList());
    }

    // Mapping method
    private TingiSaleDto mapToDto(TingiSale tingiSale) {
        return TingiSaleDto.builder()
                .id(tingiSale.getId())
                .orderId(tingiSale.getOrderId())
                .variantId(tingiSale.getVariant().getId())
                .variantName(tingiSale.getVariant().getName())
                .unitType(tingiSale.getVariant().getUnitType())
                .quantitySold(tingiSale.getQuantitySold())
                .pricePerUnit(tingiSale.getPricePerUnit())
                .totalAmount(tingiSale.getTotalAmount())
                .createdAt(tingiSale.getCreatedAt())
                .build();
    }
}
