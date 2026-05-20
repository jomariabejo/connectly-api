package com.jomariabejo.connectly_api.inventory_api.controller;

import com.jomariabejo.connectly_api.inventory_api.dto.AdjustInventoryRequest;
import com.jomariabejo.connectly_api.inventory_api.dto.CreateInventoryItemRequest;
import com.jomariabejo.connectly_api.inventory_api.dto.InventoryItemDto;
import com.jomariabejo.connectly_api.inventory_api.dto.UpdateInventoryItemRequest;
import com.jomariabejo.connectly_api.inventory_api.service.InventoryService;
import com.jomariabejo.connectly_api.tenant_api.annotation.RequiresProduct;
import com.jomariabejo.connectly_api.tenant_api.entity.ProductCode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1")
@RequiresProduct(ProductCode.INVENTORY_MANAGEMENT)
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/inventory")
    public ResponseEntity<List<InventoryItemDto>> getActiveInventory() {
        return ResponseEntity.ok(inventoryService.getActiveInventory());
    }

    @GetMapping("/inventory/{sku}")
    public ResponseEntity<InventoryItemDto> getActiveInventoryItem(@PathVariable String sku) {
        return ResponseEntity.ok(inventoryService.getActiveInventoryItem(sku));
    }

    @PostMapping("/admin/inventory")
    public ResponseEntity<InventoryItemDto> createInventoryItem(@RequestBody @Valid CreateInventoryItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.createInventoryItem(request));
    }

    @PatchMapping("/admin/inventory/{sku}")
    public ResponseEntity<InventoryItemDto> updateInventoryItem(
            @PathVariable String sku,
            @RequestBody @Valid UpdateInventoryItemRequest request) {
        return ResponseEntity.ok(inventoryService.updateInventoryItem(sku, request));
    }

    @PostMapping("/admin/inventory/{sku}/adjustments")
    public ResponseEntity<InventoryItemDto> adjustInventory(
            @PathVariable String sku,
            @RequestBody @Valid AdjustInventoryRequest request) {
        return ResponseEntity.ok(inventoryService.adjustInventory(sku, request));
    }
}
