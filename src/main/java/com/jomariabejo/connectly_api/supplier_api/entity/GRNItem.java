package com.jomariabejo.connectly_api.supplier_api.entity;

import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "grn_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GRNItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id", nullable = false)
    private GoodsReceiptNote grn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_item_id", nullable = false)
    private PurchaseOrderItem poItem;

    @Column(name = "quantity_received", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityReceived;

    @Column(name = "quantity_accepted", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal quantityAccepted = BigDecimal.ZERO;

    @Column(name = "quantity_rejected", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal quantityRejected = BigDecimal.ZERO;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

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

    public boolean isFullyAccepted() {
        return quantityAccepted != null && quantityAccepted.compareTo(quantityReceived) >= 0;
    }
}
