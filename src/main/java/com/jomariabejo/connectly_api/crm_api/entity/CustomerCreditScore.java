package com.jomariabejo.connectly_api.crm_api.entity;

import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_credit_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerCreditScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(precision = 5, scale = 2)
    private BigDecimal score; // 0-100 scale

    @Column(name = "on_time_payments")
    @Builder.Default
    private Integer onTimePayments = 0;

    @Column(name = "late_payments")
    @Builder.Default
    private Integer latePayments = 0;

    @Column(name = "missed_payments")
    @Builder.Default
    private Integer missedPayments = 0;

    @Column(name = "total_transactions")
    @Builder.Default
    private Integer totalTransactions = 0;

    @Column(name = "payment_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal paymentPercentage = BigDecimal.ZERO; // % of utang paid on time

    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;

    @Column(name = "calculated_by", length = 50)
    @Builder.Default
    private String calculatedBy = "SYSTEM";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Classify credit score into risk category
     * 0-40: HIGH_RISK, 41-60: MEDIUM_RISK, 61-80: ACCEPTABLE, 81-100: EXCELLENT
     */
    public String getRiskCategory() {
        if (score == null) return "UNKNOWN";
        int scoreInt = score.intValue();
        if (scoreInt <= 40) return "HIGH_RISK";
        if (scoreInt <= 60) return "MEDIUM_RISK";
        if (scoreInt <= 80) return "ACCEPTABLE";
        return "EXCELLENT";
    }
}
