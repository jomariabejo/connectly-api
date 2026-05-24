package com.jomariabejo.connectly_api.supplier_api.controller;

import com.jomariabejo.connectly_api.supplier_api.dto.*;
import com.jomariabejo.connectly_api.supplier_api.service.PayablesService;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payables")
@RequiredArgsConstructor
@Slf4j
public class PayablesController {

    private final PayablesService payablesService;
    private final AuthenticationService authenticationService;

    @PostMapping("/payments")
    public ResponseEntity<PurchasePaymentDto> recordPayment(@RequestBody RecordPaymentRequest request) {
        User user = authenticationService.getAuthenticatedUser();
        PurchasePaymentDto payment = payablesService.recordPayment(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @GetMapping("/supplier/{supplierId}/ledger")
    public ResponseEntity<List<PayablesLedgerDto>> getSupplierPayablesLedger(
            @PathVariable Long supplierId) {
        List<PayablesLedgerDto> ledgers = payablesService.getSupplierPayablesLedger(supplierId);
        return ResponseEntity.ok(ledgers);
    }

    @GetMapping("/supplier/{supplierId}/balance")
    public ResponseEntity<BigDecimal> getSupplierBalance(@PathVariable Long supplierId) {
        BigDecimal balance = payablesService.getSupplierBalance(supplierId);
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/aging-report")
    public ResponseEntity<List<SupplierAgingReportDto>> getAgingReport() {
        List<SupplierAgingReportDto> report = payablesService.getAgingReport();
        return ResponseEntity.ok(report);
    }
}
