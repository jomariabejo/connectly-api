package com.jomariabejo.connectly_api.inventory_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
        name = "inventory_reservations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inventory_reservations_order_sku", columnNames = {"order_id", "sku"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 120)
    private String sku;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InventoryReservationStatus status;

    @Column(name = "release_reason", length = 255)
    private String releaseReason;

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
