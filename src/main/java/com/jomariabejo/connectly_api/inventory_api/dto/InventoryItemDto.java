package com.jomariabejo.connectly_api.inventory_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItemDto {
    private String sku;
    private String name;
    private String description;
    private BigDecimal unitPrice;
    private String currency;
    private Integer onHandQuantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private Boolean active;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
