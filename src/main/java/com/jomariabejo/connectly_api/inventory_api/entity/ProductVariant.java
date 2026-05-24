package com.jomariabejo.connectly_api.inventory_api.entity;

import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_variants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_variants_tenant_sku", columnNames = {"tenant_id", "sku"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_product_id", nullable = false)
    private InventoryItem parentProduct;

    @Column(nullable = false, length = 120)
    private String sku;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "unit_type", nullable = false, length = 50)
    private String unitType; // PIECES, GLASS, PACK, BOTTLE, etc.

    @Column(name = "quantity_per_unit", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityPerUnit; // 1 glass = 0.2L, 1 piece = 1 piece

    @Column(name = "retail_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal retailPrice;

    @Column(name = "wholesale_price", precision = 15, scale = 2)
    private BigDecimal wholesalePrice;

    @Column(name = "on_hand_quantity", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal onHandQuantity = BigDecimal.ZERO;

    @Column(name = "reserved_quantity", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal reservedQuantity = BigDecimal.ZERO;

    @Column(name = "reorder_level", precision = 10, scale = 2)
    private BigDecimal reorderLevel;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, DISCONTINUED

    @Column(name = "image_key", length = 255)
    private String imageKey; // s3 key for product image

    @Column(columnDefinition = "TEXT")
    private String notes;

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

    public BigDecimal getAvailableQuantity() {
        return onHandQuantity.subtract(reservedQuantity);
    }

    public boolean isLowStock() {
        if (reorderLevel == null) {
            return false;
        }
        return getAvailableQuantity().compareTo(reorderLevel) <= 0;
    }

    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }

    public boolean isAvailable(BigDecimal quantity) {
        return getAvailableQuantity().compareTo(quantity) >= 0;
    }
}
