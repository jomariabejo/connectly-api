package com.jomariabejo.connectly_api.inventory_api.controller;

import com.jomariabejo.connectly_api.inventory_api.dto.TingiSaleDto;
import com.jomariabejo.connectly_api.inventory_api.service.TingiSaleService;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sales/tingi")
@RequiredArgsConstructor
@Slf4j
public class TingiSaleController {

    private final TingiSaleService tingiSaleService;
    private final AuthenticationService authenticationService;

    /**
     * Record a tingi sale (individual units from multipack)
     * POST /api/v1/sales/tingi
     */
    @PostMapping
    public ResponseEntity<TingiSaleDto> recordTingiSale(
            @RequestParam Long orderId,
            @RequestParam Long variantId,
            @RequestParam BigDecimal quantitySold,
            @RequestParam BigDecimal pricePerUnit) {
        
        User user = authenticationService.getAuthenticatedUser();
        TingiSaleDto tingiSale = tingiSaleService.recordTingiSale(orderId, variantId, quantitySold, pricePerUnit, user.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(tingiSale);
    }

    /**
     * Get tingi sales for an order
     * GET /api/v1/sales/tingi/order/:orderId
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<TingiSaleDto>> getTingiSalesByOrder(@PathVariable Long orderId) {
        List<TingiSaleDto> sales = tingiSaleService.getTingiSalesByOrder(orderId);
        return ResponseEntity.ok(sales);
    }

    /**
     * Get tingi sales for a variant
     * GET /api/v1/sales/tingi/variant/:variantId
     */
    @GetMapping("/variant/{variantId}")
    public ResponseEntity<List<TingiSaleDto>> getTingiSalesByVariant(@PathVariable Long variantId) {
        List<TingiSaleDto> sales = tingiSaleService.getTingiSalesByVariant(variantId);
        return ResponseEntity.ok(sales);
    }

    /**
     * Get total tingi sales for date range
     * GET /api/v1/sales/tingi/summary?startDate=...&endDate=...
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getTotalSalesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        BigDecimal totalSales = tingiSaleService.getTotalSalesByDateRange(startDate, endDate);
        
        return ResponseEntity.ok(Map.of(
                "totalSales", totalSales,
                "startDate", startDate,
                "endDate", endDate
        ));
    }

    /**
     * Get top selling variants for date range
     * GET /api/v1/sales/tingi/top-selling?startDate=...&endDate=...
     */
    @GetMapping("/top-selling")
    public ResponseEntity<List<Map<String, Object>>> getTopSellingVariantsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        List<Map<String, Object>> topVariants = tingiSaleService.getTopSellingVariantsByDateRange(startDate, endDate);
        return ResponseEntity.ok(topVariants);
    }
}
