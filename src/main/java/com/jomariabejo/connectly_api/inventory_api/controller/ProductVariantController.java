package com.jomariabejo.connectly_api.inventory_api.controller;

import com.jomariabejo.connectly_api.inventory_api.dto.*;
import com.jomariabejo.connectly_api.inventory_api.service.ProductVariantService;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products/variants")
@RequiredArgsConstructor
@Slf4j
public class ProductVariantController {

    private final ProductVariantService productVariantService;
    private final AuthenticationService authenticationService;

    /**
     * Create a product variant
     * POST /api/v1/products/variants
     */
    @PostMapping
    public ResponseEntity<ProductVariantDto> createVariant(
            @Valid @RequestBody CreateProductVariantRequest request) {
        
        User user = authenticationService.getAuthenticatedUser();
        ProductVariantDto variant = productVariantService.createVariant(request, user.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(variant);
    }

    /**
     * Get variant by ID
     * GET /api/v1/products/variants/:variantId
     */
    @GetMapping("/{variantId}")
    public ResponseEntity<ProductVariantDto> getVariant(@PathVariable Long variantId) {
        ProductVariantDto variant = productVariantService.getVariant(variantId);
        return ResponseEntity.ok(variant);
    }

    /**
     * Get variant details with pricing tiers
     * GET /api/v1/products/variants/:variantId/detail
     */
    @GetMapping("/{variantId}/detail")
    public ResponseEntity<ProductVariantDetailDto> getVariantDetail(@PathVariable Long variantId) {
        ProductVariantDetailDto variant = productVariantService.getVariantDetail(variantId);
        return ResponseEntity.ok(variant);
    }

    /**
     * Get all variants for a parent product
     * GET /api/v1/products/:parentProductId/variants
     */
    @GetMapping("/parent/{parentProductId}")
    public ResponseEntity<List<ProductVariantDto>> getVariantsByParentProduct(@PathVariable Long parentProductId) {
        List<ProductVariantDto> variants = productVariantService.getVariantsByParentProduct(parentProductId);
        return ResponseEntity.ok(variants);
    }

    /**
     * Update variant
     * PUT /api/v1/products/variants/:variantId
     */
    @PutMapping("/{variantId}")
    public ResponseEntity<ProductVariantDto> updateVariant(
            @PathVariable Long variantId,
            @Valid @RequestBody CreateProductVariantRequest request) {
        
        User user = authenticationService.getAuthenticatedUser();
        ProductVariantDto variant = productVariantService.updateVariant(variantId, request, user.getId());
        
        return ResponseEntity.ok(variant);
    }

    /**
     * Add pricing tier for variant
     * POST /api/v1/products/variants/:variantId/pricing
     */
    @PostMapping("/{variantId}/pricing")
    public ResponseEntity<VariantPricingDto> addPricingTier(
            @PathVariable Long variantId,
            @Valid @RequestBody CreateVariantPricingRequest request) {
        
        User user = authenticationService.getAuthenticatedUser();
        // Override variantId in request to match path parameter
        request.setVariantId(variantId);
        VariantPricingDto pricing = productVariantService.addPricingTier(request, user.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(pricing);
    }

    /**
     * Get low stock variants
     * GET /api/v1/products/variants/low-stock
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductVariantDto>> getLowStockVariants() {
        List<ProductVariantDto> variants = productVariantService.getLowStockVariants();
        return ResponseEntity.ok(variants);
    }

    /**
     * Get all active variants
     * GET /api/v1/products/variants/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<ProductVariantDto>> getAllActiveVariants() {
        List<ProductVariantDto> variants = productVariantService.getAllActiveVariants();
        return ResponseEntity.ok(variants);
    }
}
