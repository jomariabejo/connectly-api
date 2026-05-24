package com.jomariabejo.connectly_api.supplier_api.service;

import com.jomariabejo.connectly_api.supplier_api.dto.*;
import com.jomariabejo.connectly_api.supplier_api.entity.*;
import com.jomariabejo.connectly_api.supplier_api.repository.*;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import com.jomariabejo.connectly_api.tenant_api.service.AuditLoggingService;
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
public class PayablesService {

    private final PayablesLedgerRepository payablesLedgerRepository;
    private final PurchasePaymentRepository paymentRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final PurchaseInvoiceRepository invoiceRepository;
    private final SupplierRepository supplierRepository;
    private final TenantContextService tenantContextService;
    private final AuditLoggingService auditService;

    /**
     * Record payment for invoices (FIFO allocation)
     */
    @Transactional
    public PurchasePaymentDto recordPayment(RecordPaymentRequest request, Long userId) {
        Long tenantId = tenantContextService.requireTenantId();

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new IllegalStateException("Supplier not found"));

        if (!supplier.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Supplier does not belong to this tenant");
        }

        // Get unpaid invoices (FIFO)
        List<PayablesLedger> unpaidInvoices = payablesLedgerRepository
                .findUnpaidInvoicesBySupplier(request.getSupplierId());

        if (unpaidInvoices.isEmpty()) {
            throw new IllegalStateException("No unpaid invoices found for supplier");
        }

        PurchasePayment payment = PurchasePayment.builder()
                .tenant(supplier.getTenant())
                .supplier(supplier)
                .paymentAmount(request.getPaymentAmount())
                .paymentDate(LocalDateTime.now())
                .paymentMethod(request.getPaymentMethod())
                .referenceNumber(request.getReferenceNumber())
                .notes(request.getNotes())
                .build();

        payment = paymentRepository.save(payment);

        // FIFO allocation
        BigDecimal remainingAmount = request.getPaymentAmount();
        int allocationOrder = 0;

        for (PayablesLedger ledger : unpaidInvoices) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal allocatedAmount = remainingAmount.min(ledger.getBalance());

            PaymentAllocation allocation = PaymentAllocation.builder()
                    .tenant(supplier.getTenant())
                    .payment(payment)
                    .payablesLedger(ledger)
                    .invoiceId(ledger.getInvoice().getId())
                    .allocatedAmount(allocatedAmount)
                    .allocationOrder(allocationOrder++)
                    .build();

            allocationRepository.save(allocation);

            // Update ledger balance
            ledger.setBalance(ledger.getBalance().subtract(allocatedAmount));
            payablesLedgerRepository.save(ledger);

            // Update payment status if fully paid
            if (ledger.getBalance().compareTo(BigDecimal.ZERO) == 0) {
                PurchaseInvoice invoice = ledger.getInvoice();
                invoice.setPaymentStatus("PAID");
                invoiceRepository.save(invoice);
            }

            remainingAmount = remainingAmount.subtract(allocatedAmount);
        }

        // Audit log
        auditService.logEvent(tenantId, userId, "CREATE", "PurchasePayment", payment.getId(), null,
                Map.of("amount", request.getPaymentAmount(), "supplier", request.getSupplierId()),
                null, null);

        log.info("Recorded payment: {} for supplier: {}", payment.getId(), request.getSupplierId());

        return mapPaymentToDto(payment);
    }

    /**
     * Get payables ledger for supplier
     */
    public List<PayablesLedgerDto> getSupplierPayablesLedger(Long supplierId) {
        Long tenantId = tenantContextService.requireTenantId();

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalStateException("Supplier not found"));

        if (!supplier.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Supplier does not belong to this tenant");
        }

        List<PayablesLedger> ledgers = payablesLedgerRepository
                .findBySupplierIdOrderByTransactionDateDesc(supplierId);

        return ledgers.stream()
                .map(this::mapLedgerToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get unpaid amount by supplier
     */
    public BigDecimal getSupplierBalance(Long supplierId) {
        Long tenantId = tenantContextService.requireTenantId();

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalStateException("Supplier not found"));

        if (!supplier.getTenant().getId().equals(tenantId)) {
            throw new IllegalStateException("Supplier does not belong to this tenant");
        }

        return payablesLedgerRepository.getSupplierTotalBalance(supplierId);
    }

    /**
     * Get aging report
     */
    public List<SupplierAgingReportDto> getAgingReport() {
        Long tenantId = tenantContextService.requireTenantId();

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime sixtyDaysAgo = LocalDateTime.now().minusDays(60);
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);

        List<Supplier> suppliers = supplierRepository.findByTenantOrderByNameAsc(tenantId);

        return suppliers.stream()
                .map(supplier -> {
                    List<PayablesLedger> ledgers = payablesLedgerRepository
                            .findUnpaidInvoicesBySupplier(supplier.getId());

                    BigDecimal current = ledgers.stream()
                            .filter(l -> l.getTransactionDate().isAfter(thirtyDaysAgo))
                            .map(PayablesLedger::getBalance)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal thirtyPlus = ledgers.stream()
                            .filter(l -> l.getTransactionDate().isBefore(thirtyDaysAgo) &&
                                    l.getTransactionDate().isAfter(sixtyDaysAgo))
                            .map(PayablesLedger::getBalance)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal sixtyPlus = ledgers.stream()
                            .filter(l -> l.getTransactionDate().isBefore(sixtyDaysAgo) &&
                                    l.getTransactionDate().isAfter(ninetyDaysAgo))
                            .map(PayablesLedger::getBalance)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal ninetyPlus = ledgers.stream()
                            .filter(l -> l.getTransactionDate().isBefore(ninetyDaysAgo))
                            .map(PayablesLedger::getBalance)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return SupplierAgingReportDto.builder()
                            .supplierId(supplier.getId())
                            .supplierName(supplier.getName())
                            .currentAmount(current)
                            .thirtyDaysAmount(thirtyPlus)
                            .sixtyDaysAmount(sixtyPlus)
                            .ninetyDaysAmount(ninetyPlus)
                            .totalAmount(current.add(thirtyPlus).add(sixtyPlus).add(ninetyPlus))
                            .build();
                })
                .filter(report -> report.getTotalAmount().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }

    // Mapping methods
    private PurchasePaymentDto mapPaymentToDto(PurchasePayment payment) {
        return PurchasePaymentDto.builder()
                .id(payment.getId())
                .supplierId(payment.getSupplier().getId())
                .supplierName(payment.getSupplier().getName())
                .paymentAmount(payment.getPaymentAmount())
                .paymentDate(payment.getPaymentDate())
                .paymentMethod(payment.getPaymentMethod())
                .referenceNumber(payment.getReferenceNumber())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private PayablesLedgerDto mapLedgerToDto(PayablesLedger ledger) {
        return PayablesLedgerDto.builder()
                .id(ledger.getId())
                .supplierId(ledger.getSupplier().getId())
                .supplierName(ledger.getSupplier().getName())
                .invoiceId(ledger.getInvoice().getId())
                .invoiceNumber(ledger.getInvoice().getInvoiceNumber())
                .transactionType(ledger.getTransactionType())
                .amount(ledger.getAmount())
                .balance(ledger.getBalance())
                .transactionDate(ledger.getTransactionDate())
                .createdAt(ledger.getCreatedAt())
                .build();
    }
}
