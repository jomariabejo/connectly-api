package com.jomariabejo.connectly_api.inventory_api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateInventoryItemRequest {
    private String name;
    private String description;

    @DecimalMin(value = "0.00", inclusive = true, message = "Unit price must be non-negative")
    private BigDecimal unitPrice;

    private String currency;

    @Min(value = 0, message = "On-hand quantity must be non-negative")
    private Integer onHandQuantity;

    private Boolean active;
}
