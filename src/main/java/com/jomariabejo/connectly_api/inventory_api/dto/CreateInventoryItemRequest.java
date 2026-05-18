package com.jomariabejo.connectly_api.inventory_api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInventoryItemRequest {
    @NotBlank(message = "SKU is required")
    private String sku;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Unit price must be non-negative")
    private BigDecimal unitPrice;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotNull(message = "On-hand quantity is required")
    @Min(value = 0, message = "On-hand quantity must be non-negative")
    private Integer onHandQuantity;

    private Boolean active;
}
