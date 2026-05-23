package com.jomariabejo.connectly_api.crm_api.controller;

import com.jomariabejo.connectly_api.crm_api.dto.*;
import com.jomariabejo.connectly_api.crm_api.service.UtangLedgerService;
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
@RequestMapping("/api/v1/utang")
@RequiredArgsConstructor
@Slf4j
public class UtangController {

    private final UtangLedgerService utangLedgerService;
    private final AuthenticationService authenticationService;

    /**
     * Create a credit line for a customer
     * POST /api/v1/utang/credit-lines
     */
    @PostMapping("/credit-lines")
    public ResponseEntity<CustomerLedgerDto> createCreditLine(
            @Valid @RequestBody CreateCreditLineRequest request) {
        
        User user = authenticationService.getAuthenticatedUser();
        CustomerLedgerDto ledger = utangLedgerService.createCreditLine(request, user.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ledger);
    }

    /**
     * Get credit line for a customer
     * GET /api/v1/utang/credit-lines/:customerId
     */
    @GetMapping("/credit-lines/{customerId}")
    public ResponseEntity<CustomerLedgerDto> getCreditLine(@PathVariable Long customerId) {
        CustomerLedgerDto ledger = utangLedgerService.getCreditLine(customerId);
        return ResponseEntity.ok(ledger);
    }

    /**
     * Record a utang transaction (debit)
     * POST /api/v1/utang/transactions
     */
    @PostMapping("/transactions")
    public ResponseEntity<UtangTransactionDto> recordTransaction(
            @Valid @RequestBody CreateUtangTransactionRequest request) {
        
        User user = authenticationService.getAuthenticatedUser();
        UtangTransactionDto transaction = utangLedgerService.recordUtangTransaction(request, user.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    /**
     * Record a payment against utang
     * POST /api/v1/utang/payments
     */
    @PostMapping("/payments")
    public ResponseEntity<UtangPaymentDto> recordPayment(
            @Valid @RequestBody CreateUtangPaymentRequest request) {
        
        User user = authenticationService.getAuthenticatedUser();
        UtangPaymentDto payment = utangLedgerService.recordPayment(request, user.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    /**
     * Get utang transaction history for a customer
     * GET /api/v1/utang/transactions/:customerId
     */
    @GetMapping("/transactions/{customerId}")
    public ResponseEntity<List<UtangTransactionDto>> getUtangHistory(@PathVariable Long customerId) {
        List<UtangTransactionDto> transactions = utangLedgerService.getUtangHistory(customerId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get payment history for a customer
     * GET /api/v1/utang/payments/:customerId
     */
    @GetMapping("/payments/{customerId}")
    public ResponseEntity<List<UtangPaymentDto>> getPaymentHistory(@PathVariable Long customerId) {
        List<UtangPaymentDto> payments = utangLedgerService.getPaymentHistory(customerId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get credit score for a customer
     * GET /api/v1/utang/credit-score/:customerId
     */
    @GetMapping("/credit-score/{customerId}")
    public ResponseEntity<CustomerCreditScoreDto> getCreditScore(@PathVariable Long customerId) {
        CustomerCreditScoreDto creditScore = utangLedgerService.getCreditScore(customerId);
        return ResponseEntity.ok(creditScore);
    }

    /**
     * Get comprehensive utang summary for a customer
     * GET /api/v1/utang/summary/:customerId
     */
    @GetMapping("/summary/{customerId}")
    public ResponseEntity<CustomerUtangSummaryDto> getUtangSummary(@PathVariable Long customerId) {
        CustomerUtangSummaryDto summary = utangLedgerService.getUtangSummary(customerId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get overdue utang transactions
     * GET /api/v1/utang/overdue
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<UtangTransactionDto>> getOverdueTransactions() {
        List<UtangTransactionDto> transactions = utangLedgerService.getOverdueTransactions();
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get top debtors for the tenant
     * GET /api/v1/utang/debtors/top
     */
    @GetMapping("/debtors/top")
    public ResponseEntity<List<CustomerLedgerDto>> getTopDebtors() {
        List<CustomerLedgerDto> debtors = utangLedgerService.getTopDebtors();
        return ResponseEntity.ok(debtors);
    }
}
