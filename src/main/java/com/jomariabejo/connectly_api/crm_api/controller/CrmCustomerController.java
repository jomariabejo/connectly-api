package com.jomariabejo.connectly_api.crm_api.controller;

import com.jomariabejo.connectly_api.crm_api.dto.*;
import com.jomariabejo.connectly_api.crm_api.service.CrmCustomerService;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.tenant_api.annotation.RequiresProduct;
import com.jomariabejo.connectly_api.tenant_api.entity.ProductCode;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/crm/customers")
@RequiresProduct(ProductCode.CRM)
public class CrmCustomerController {
    private final CrmCustomerService crmCustomerService;
    private final AuthenticationService authenticationService;

    public CrmCustomerController(CrmCustomerService crmCustomerService, AuthenticationService authenticationService) {
        this.crmCustomerService = crmCustomerService;
        this.authenticationService = authenticationService;
    }

    @GetMapping
    public ResponseEntity<Page<CrmCustomerDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(crmCustomerService.listCustomers(page, size, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CrmCustomerDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(crmCustomerService.getCustomer(id));
    }

    @PostMapping
    public ResponseEntity<CrmCustomerDto> create(@RequestBody @Valid CreateCrmCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(crmCustomerService.createCustomer(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CrmCustomerDto> update(@PathVariable Long id, @RequestBody UpdateCrmCustomerRequest request) {
        return ResponseEntity.ok(crmCustomerService.updateCustomer(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        crmCustomerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/notes")
    public ResponseEntity<List<CrmCustomerNoteDto>> getNotes(@PathVariable Long id) {
        return ResponseEntity.ok(crmCustomerService.getNotes(id));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<CrmCustomerNoteDto> addNote(@PathVariable Long id, @RequestBody @Valid CreateCrmNoteRequest request) {
        User user = authenticationService.getAuthenticatedUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(crmCustomerService.addNote(id, request, user));
    }

    @GetMapping("/{id}/interactions")
    public ResponseEntity<List<CrmInteractionDto>> getInteractions(@PathVariable Long id) {
        return ResponseEntity.ok(crmCustomerService.getInteractions(id));
    }

    @PostMapping("/{id}/interactions")
    public ResponseEntity<CrmInteractionDto> addInteraction(@PathVariable Long id, @RequestBody @Valid CreateCrmInteractionRequest request) {
        User user = authenticationService.getAuthenticatedUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(crmCustomerService.addInteraction(id, request, user));
    }
}
