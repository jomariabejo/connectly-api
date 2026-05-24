package com.jomariabejo.connectly_api.supplier_api.entity;

import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAllocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private PurchasePayment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payables_ledger_id", nullable = false)
    private PayablesLedger payablesLedger;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "allocated_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal allocatedAmount;

    @Column(name = "allocation_order")
    private Integer allocationOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
