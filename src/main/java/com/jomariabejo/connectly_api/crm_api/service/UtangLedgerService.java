package com.jomariabejo.connectly_api.crm_api.service;

import com.jomariabejo.connectly_api.crm_api.dto.*;
import com.jomariabejo.connectly_api.crm_api.entity.*;
import com.jomariabejo.connectly_api.crm_api.repository.*;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UtangLedgerService {

    private final CustomerLedgerRepository ledgerRepository;
    private final UtangTransactionRepository transactionRepository;
    private final UtangPaymentRepository paymentRepository;
    private final CustomerCreditScoreRepository creditScoreRepository;
    private final UtangPaymentAllocationRepository allocationRepository;
    private final UtangReminderRepository reminderRepository;
    private final CustomerRepository customerRepository;
    private final TenantContextService tenantContextService;
    private final PricingService pricingService;
    private final AuditLoggingService auditService;
    private final TenantRepository tenantRepository;

    /**
     * Create a credit line for a customer
     */
    @Transactional
    public CustomerLedgerDto createCreditLine(CreateCreditLineRequest request, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();
        
        // Verify feature access
        pricingService.requireFeatureAccess(tenantId, "CREDIT_TRACKING");
        
        // Get customer
        Customer customer = customerRepository.findByIdAndTenantId(request.getCustomerId(), tenantId)
                .orElseThrow(() -> new IllegalStateException("Customer not found"));
        
        // Check if customer already has a credit line
        var existingLedger = ledgerRepository.findByTenantAndCustomer(tenantId, request.getCustomerId());
        if (existingLedger.isPresent()) {
            throw new IllegalStateException("Customer already has an active credit line");
        }
        
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));
        
        // Create ledger
        CustomerLedger ledger = CustomerLedger.builder()
                .tenant(tenant)
                .customer(customer)
                .creditLimit(request.getCreditLimit())
                .availableCredit(request.getCreditLimit())
                .totalOutstanding(BigDecimal.ZERO)
                .status("ACTIVE")
                .creditLineApprovedAt(LocalDateTime.now())
                .creditLineApprovedBy(userId)
                .notes(request.getNotes())
                .build();
        
        ledger = ledgerRepository.save(ledger);
        
        // Update customer record
        customer.setHasCreditLine(true);
        customer.setCreditLimit(request.getCreditLimit());
        customer.setCreditStatus("ACTIVE");
        customerRepository.save(customer);
        
        // Audit log
        auditService.logEvent(tenantId, userId, "CREATE", "CustomerLedger", ledger.getId(), null,
                Map.of("customerId", customer.getId(), "creditLimit", request.getCreditLimit()),
                null, null);
        
        return mapToDto(ledger);
    }

    /**
     * Get credit line for a customer
     */
    public CustomerLedgerDto getCreditLine(Long customerId) {
        Long tenantId = tenantContextService.requireTenantId();
        
        CustomerLedger ledger = ledgerRepository.findByTenantAndCustomer(tenantId, customerId)
                .orElseThrow(() -> new IllegalStateException("Credit line not found for customer"));
        
        return mapToDto(ledger);
    }

    /**
     * Record a utang transaction (debit)
     */
    @Transactional
    public UtangTransactionDto recordUtangTransaction(CreateUtangTransactionRequest request, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();
        
        // Verify feature access
        pricingService.requireFeatureAccess(tenantId, "CREDIT_TRACKING");
        
        // Get customer ledger
        CustomerLedger ledger = ledgerRepository.findByTenantAndCustomer(tenantId, request.getCustomerId())
                .orElseThrow(() -> new IllegalStateException("Credit line not found for customer"));
        
        // Verify customer has enough credit
        if (!ledger.hasCreditAvailable(request.getAmount())) {
            throw new IllegalStateException("Insufficient credit available. Available: " + ledger.getAvailableCredit());
        }
        
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));
        
        Customer customer = ledger.getCustomer();
        
        // Create transaction
        UtangTransaction transaction = UtangTransaction.builder()
                .tenant(tenant)
                .customer(customer)
                .customerLedger(ledger)
                .amount(request.getAmount())
                .transactionType(request.getTransactionType())
                .referenceType(request.getReferenceType())
                .referenceId(request.getReferenceId())
                .orderId(request.getOrderId())
                .dueDate(request.getDueDate())
                .notes(request.getNotes())
                .createdBy(userId)
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        // Update ledger
        ledger.setAvailableCredit(ledger.getAvailableCredit().subtract(request.getAmount()));
        ledger.setTotalOutstanding(ledger.getTotalOutstanding().add(request.getAmount()));
        ledgerRepository.save(ledger);
        
        // Update customer
        customer.setTotalUtang(ledger.getTotalOutstanding());
        customerRepository.save(customer);
        
        // Audit log
        auditService.logEvent(tenantId, userId, "CREATE", "UtangTransaction", transaction.getId(), null,
                Map.of("customerId", customer.getId(), "amount", request.getAmount(), "type", request.getTransactionType()),
                null, null);
        
        log.info("Recorded utang transaction: {} for customer: {}", transaction.getId(), customer.getId());
        
        return mapTransactionToDto(transaction);
    }

    /**
     * Record a payment against utang
     */
    @Transactional
    public UtangPaymentDto recordPayment(CreateUtangPaymentRequest request, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();
        
        // Verify feature access
        pricingService.requireFeatureAccess(tenantId, "CREDIT_TRACKING");
        
        // Get customer ledger
        CustomerLedger ledger = ledgerRepository.findByTenantAndCustomer(tenantId, request.getCustomerId())
                .orElseThrow(() -> new IllegalStateException("Credit line not found for customer"));
        
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));
        
        Customer customer = ledger.getCustomer();
        
        // Create payment record
        UtangPayment payment = UtangPayment.builder()
                .tenant(tenant)
                .customer(customer)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .referenceNumber(request.getReferenceNumber())
                .notes(request.getNotes())
                .receivedBy(userId)
                .paymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : LocalDateTime.now())
                .build();
        
        payment = paymentRepository.save(payment);
        
        // Allocate payment to outstanding debits (FIFO)
        allocatePaymentToDebits(payment, ledger, userId);
        
        // Recalculate credit score
        recalculateCreditScore(tenantId, request.getCustomerId());
        
        // Audit log
        auditService.logEvent(tenantId, userId, "CREATE", "UtangPayment", payment.getId(), null,
                Map.of("customerId", customer.getId(), "amount", request.getAmount()),
                null, null);
        
        log.info("Recorded payment: {} for customer: {}", payment.getId(), customer.getId());
        
        return mapPaymentToDto(payment);
    }

    /**
     * Allocate payment to outstanding debits (FIFO - oldest first)
     */
    @Transactional
    protected void allocatePaymentToDebits(UtangPayment payment, CustomerLedger ledger, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();
        
        // Get outstanding debits ordered by date
        List<UtangTransaction> debits = transactionRepository.findOutstandingDebitsByCustomer(tenantId, ledger.getCustomer().getId());
        
        BigDecimal remainingPayment = payment.getAmount();
        
        for (UtangTransaction debit : debits) {
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            
            // Allocate payment to this debit
            BigDecimal allocated = remainingPayment.min(debit.getAmount());
            
            // Create allocation record
            UtangPaymentAllocation allocation = UtangPaymentAllocation.builder()
                    .tenant(payment.getTenant())
                    .payment(payment)
                    .transaction(debit)
                    .amountAllocated(allocated)
                    .build();
            
            allocationRepository.save(allocation);
            
            remainingPayment = remainingPayment.subtract(allocated);
        }
        
        // Update ledger
        BigDecimal totalOutstanding = transactionRepository.getTotalOutstandingAmount(tenantId, ledger.getCustomer().getId());
        ledger.setTotalOutstanding(totalOutstanding);
        ledger.setAvailableCredit(ledger.getCreditLimit().subtract(totalOutstanding));
        ledgerRepository.save(ledger);
        
        // Update customer
        Customer customer = ledger.getCustomer();
        customer.setTotalUtang(totalOutstanding);
        customerRepository.save(customer);
    }

    /**
     * Get customer utang history
     */
    public List<UtangTransactionDto> getUtangHistory(Long customerId) {
        Long tenantId = tenantContextService.requireTenantId();
        
        List<UtangTransaction> transactions = transactionRepository.findByTenantAndCustomer(tenantId, customerId);
        
        return transactions.stream()
                .map(this::mapTransactionToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get customer payment history
     */
    public List<UtangPaymentDto> getPaymentHistory(Long customerId) {
        Long tenantId = tenantContextService.requireTenantId();
        
        List<UtangPayment> payments = paymentRepository.findByTenantAndCustomer(tenantId, customerId);
        
        return payments.stream()
                .map(this::mapPaymentToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get customer credit score
     */
    public CustomerCreditScoreDto getCreditScore(Long customerId) {
        Long tenantId = tenantContextService.requireTenantId();
        
        CustomerCreditScore score = creditScoreRepository.findByTenantAndCustomer(tenantId, customerId)
                .orElseThrow(() -> new IllegalStateException("Credit score not found"));
        
        return mapCreditScoreToDto(score);
    }

    /**
     * Recalculate customer credit score
     */
    @Transactional
    protected void recalculateCreditScore(Long tenantId, Long customerId) {
        CustomerCreditScore score = creditScoreRepository.findByTenantAndCustomer(tenantId, customerId)
                .orElse(null);
        
        if (score == null) {
            score = CustomerCreditScore.builder()
                    .tenant(tenantRepository.findById(tenantId).orElseThrow())
                    .customer(customerRepository.findByIdAndTenantId(customerId, tenantId)
                            .orElseThrow(() -> new IllegalStateException("Customer not found")))
                    .score(BigDecimal.valueOf(100))
                    .onTimePayments(0)
                    .latePayments(0)
                    .missedPayments(0)
                    .totalTransactions(0)
                    .paymentPercentage(BigDecimal.ZERO)
                    .calculatedBy("SYSTEM")
                    .build();
        }
        
        // Get all transactions and payments
        List<UtangTransaction> debits = transactionRepository.findOutstandingDebitsByCustomer(tenantId, customerId);
        List<UtangPayment> payments = paymentRepository.findByTenantAndCustomer(tenantId, customerId);
        
        // Calculate metrics
        int onTimePayments = 0;
        int latePayments = 0;
        int missedPayments = 0;
        
        for (UtangTransaction debit : debits) {
            if (debit.getDueDate() != null) {
                // Check if there's a payment for this debit
                List<UtangPaymentAllocation> allocations = allocationRepository.findByTransactionId(debit.getId());
                
                if (allocations.isEmpty()) {
                    // No payment made
                    if (LocalDateTime.now().isAfter(debit.getDueDate())) {
                        missedPayments++;
                    }
                } else {
                    // Payment made - check if on time
                    UtangPaymentAllocation allocation = allocations.get(0);
                    if (allocation.getPayment().getPaymentDate().isBefore(debit.getDueDate())) {
                        onTimePayments++;
                    } else {
                        latePayments++;
                    }
                }
            }
        }
        
        score.setOnTimePayments(onTimePayments);
        score.setLatePayments(latePayments);
        score.setMissedPayments(missedPayments);
        score.setTotalTransactions(debits.size());
        
        // Calculate payment percentage
        if (debits.size() > 0) {
            BigDecimal paymentPercentage = BigDecimal.valueOf((onTimePayments + latePayments) * 100.0 / debits.size());
            score.setPaymentPercentage(paymentPercentage);
        }
        
        // Calculate score (0-100)
        BigDecimal calculatedScore = calculateScoreFromMetrics(score);
        score.setScore(calculatedScore);
        score.setLastCalculatedAt(LocalDateTime.now());
        
        creditScoreRepository.save(score);
        
        log.info("Recalculated credit score for customer: {}, score: {}", customerId, calculatedScore);
    }

    /**
     * Calculate credit score based on payment history metrics
     * Score = (onTimePayments * 100 + latePayments * 50) / totalTransactions - (missedPayments * 10)
     */
    protected BigDecimal calculateScoreFromMetrics(CustomerCreditScore score) {
        if (score.getTotalTransactions() == 0) {
            return BigDecimal.valueOf(100); // New customer
        }
        
        BigDecimal scoreValue = BigDecimal.valueOf(
                (score.getOnTimePayments() * 100.0 + score.getLatePayments() * 50.0) / score.getTotalTransactions()
                        - (score.getMissedPayments() * 10.0)
        );
        
        // Clamp between 0 and 100
        if (scoreValue.compareTo(BigDecimal.ZERO) < 0) {
            scoreValue = BigDecimal.ZERO;
        } else if (scoreValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            scoreValue = BigDecimal.valueOf(100);
        }
        
        return scoreValue.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Get overdue utang transactions
     */
    public List<UtangTransactionDto> getOverdueTransactions() {
        Long tenantId = tenantContextService.requireTenantId();
        
        List<UtangTransaction> transactions = transactionRepository.findOverdueTransactions(tenantId, LocalDateTime.now());
        
        return transactions.stream()
                .map(this::mapTransactionToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get top debtors for tenant
     */
    public List<CustomerLedgerDto> getTopDebtors() {
        Long tenantId = tenantContextService.requireTenantId();
        
        List<CustomerLedger> ledgers = ledgerRepository.findTopDebtorsByTenant(tenantId);
        
        return ledgers.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get comprehensive utang summary for a customer
     */
    public CustomerUtangSummaryDto getUtangSummary(Long customerId) {
        Long tenantId = tenantContextService.requireTenantId();
        
        CustomerLedger ledger = ledgerRepository.findByTenantAndCustomer(tenantId, customerId)
                .orElseThrow(() -> new IllegalStateException("Credit line not found for customer"));
        
        // Get credit score
        CustomerCreditScore creditScore = creditScoreRepository.findByTenantAndCustomer(tenantId, customerId).orElse(null);
        
        // Get recent transactions
        List<UtangTransaction> transactions = transactionRepository.findByTenantAndCustomer(tenantId, customerId).stream()
                .limit(10)
                .collect(Collectors.toList());
        
        // Get recent payments
        List<UtangPayment> payments = paymentRepository.findByTenantAndCustomer(tenantId, customerId).stream()
                .limit(10)
                .collect(Collectors.toList());
        
        // Get overdue info
        int overdueDays = 0;
        BigDecimal overdueAmount = BigDecimal.ZERO;
        List<UtangTransaction> overdueTransactions = transactionRepository.findOverdueTransactions(tenantId, LocalDateTime.now()).stream()
                .filter(t -> t.getCustomer().getId().equals(customerId))
                .collect(Collectors.toList());
        
        if (!overdueTransactions.isEmpty()) {
            UtangTransaction oldest = overdueTransactions.get(0);
            if (oldest.getDueDate() != null) {
                overdueDays = (int) ChronoUnit.DAYS.between(oldest.getDueDate(), LocalDateTime.now());
                overdueAmount = overdueTransactions.stream()
                        .map(UtangTransaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        }
        
        return CustomerUtangSummaryDto.builder()
                .customerId(customerId)
                .creditLimit(ledger.getCreditLimit())
                .availableCredit(ledger.getAvailableCredit())
                .totalOutstanding(ledger.getTotalOutstanding())
                .creditStatus(ledger.getStatus())
                .creditScore(creditScore != null ? mapCreditScoreToDto(creditScore) : null)
                .recentTransactions(transactions.stream().map(this::mapTransactionToDto).collect(Collectors.toList()))
                .recentPayments(payments.stream().map(this::mapPaymentToDto).collect(Collectors.toList()))
                .overdueDays(overdueDays)
                .overdueAmount(overdueAmount)
                .build();
    }

    // Mapping methods
    private CustomerLedgerDto mapToDto(CustomerLedger ledger) {
        return CustomerLedgerDto.builder()
                .id(ledger.getId())
                .customerId(ledger.getCustomer().getId())
                .creditLimit(ledger.getCreditLimit())
                .availableCredit(ledger.getAvailableCredit())
                .totalOutstanding(ledger.getTotalOutstanding())
                .status(ledger.getStatus())
                .creditLineApprovedAt(ledger.getCreditLineApprovedAt())
                .notes(ledger.getNotes())
                .createdAt(ledger.getCreatedAt())
                .updatedAt(ledger.getUpdatedAt())
                .build();
    }

    private UtangTransactionDto mapTransactionToDto(UtangTransaction transaction) {
        return UtangTransactionDto.builder()
                .id(transaction.getId())
                .customerId(transaction.getCustomer().getId())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .referenceType(transaction.getReferenceType())
                .referenceId(transaction.getReferenceId())
                .dueDate(transaction.getDueDate())
                .notes(transaction.getNotes())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private UtangPaymentDto mapPaymentToDto(UtangPayment payment) {
        return UtangPaymentDto.builder()
                .id(payment.getId())
                .customerId(payment.getCustomer().getId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .referenceNumber(payment.getReferenceNumber())
                .paymentDate(payment.getPaymentDate())
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private CustomerCreditScoreDto mapCreditScoreToDto(CustomerCreditScore creditScore) {
        return CustomerCreditScoreDto.builder()
                .customerId(creditScore.getCustomer().getId())
                .score(creditScore.getScore())
                .onTimePayments(creditScore.getOnTimePayments())
                .latePayments(creditScore.getLatePayments())
                .missedPayments(creditScore.getMissedPayments())
                .totalTransactions(creditScore.getTotalTransactions())
                .paymentPercentage(creditScore.getPaymentPercentage())
                .riskCategory(creditScore.getRiskCategory())
                .build();
    }
}
