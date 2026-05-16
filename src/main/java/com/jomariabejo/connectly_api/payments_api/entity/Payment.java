package com.jomariabejo.connectly_api.payments_api.entity;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.orders_api.entity.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payments_idempotency_key", columnNames = "idempotency_key"),
                @UniqueConstraint(name = "uk_payments_provider_payment_id", columnNames = {"provider", "provider_payment_id"}),
                @UniqueConstraint(name = "uk_payments_provider_checkout_id", columnNames = {"provider", "provider_checkout_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentProvider provider;

    @Column(name = "provider_payment_id", length = 120)
    private String providerPaymentId;

    @Column(name = "provider_checkout_id", length = 120)
    private String providerCheckoutId;

    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "intent_type", nullable = false, length = 30)
    private PaymentIntentType intentType;

    @Column(name = "checkout_url", length = 2048)
    private String checkoutUrl;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(name = "failure_code", length = 120)
    private String failureCode;

    @Column(name = "failure_message", length = 1024)
    private String failureMessage;

    @Column(name = "created_date", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "updated_date", nullable = false)
    @Builder.Default
    private LocalDateTime updatedDate = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
