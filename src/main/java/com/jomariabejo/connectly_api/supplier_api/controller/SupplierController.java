package com.jomariabejo.connectly_api.supplier_api.controller;

import com.jomariabejo.connectly_api.supplier_api.dto.CreateSupplierRequest;
import com.jomariabejo.connectly_api.supplier_api.dto.SupplierDto;
import com.jomariabejo.connectly_api.supplier_api.service.SupplierService;
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
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@Slf4j
public class SupplierController {

    private final SupplierService supplierService;
    private final AuthenticationService authenticationService;

    /**
     * Create a supplier
     * POST /api/v1/suppliers
     */
    @PostMapping
    public ResponseEntity<SupplierDto> createSupplier(@Valid @RequestBody CreateSupplierRequest request) {
        User user = authenticationService.getAuthenticatedUser();
        SupplierDto supplier = supplierService.createSupplier(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(supplier);
    }

    /**
     * Get supplier by ID
     * GET /api/v1/suppliers/:supplierId
     */
    @GetMapping("/{supplierId}")
    public ResponseEntity<SupplierDto> getSupplier(@PathVariable Long supplierId) {
        SupplierDto supplier = supplierService.getSupplier(supplierId);
        return ResponseEntity.ok(supplier);
    }

    /**
     * Get all active suppliers
     * GET /api/v1/suppliers/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<SupplierDto>> getAllActiveSuppliers() {
        List<SupplierDto> suppliers = supplierService.getAllActiveSuppliers();
        return ResponseEntity.ok(suppliers);
    }

    /**
     * Get all suppliers
     * GET /api/v1/suppliers
     */
    @GetMapping
    public ResponseEntity<List<SupplierDto>> getAllSuppliers() {
        List<SupplierDto> suppliers = supplierService.getAllSuppliers();
        return ResponseEntity.ok(suppliers);
    }

    /**
     * Update supplier
     * PUT /api/v1/suppliers/:supplierId
     */
    @PutMapping("/{supplierId}")
    public ResponseEntity<SupplierDto> updateSupplier(
            @PathVariable Long supplierId,
            @Valid @RequestBody CreateSupplierRequest request) {
        User user = authenticationService.getAuthenticatedUser();
        SupplierDto supplier = supplierService.updateSupplier(supplierId, request, user.getId());
        return ResponseEntity.ok(supplier);
    }

    /**
     * Deactivate supplier
     * DELETE /api/v1/suppliers/:supplierId
     */
    @DeleteMapping("/{supplierId}")
    public ResponseEntity<Void> deactivateSupplier(@PathVariable Long supplierId) {
        User user = authenticationService.getAuthenticatedUser();
        supplierService.deactivateSupplier(supplierId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
