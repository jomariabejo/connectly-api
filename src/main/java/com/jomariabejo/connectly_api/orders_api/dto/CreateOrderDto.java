package com.jomariabejo.connectly_api.orders_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderDto {
    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Items are required")
    @Size(min = 1, message = "Order must contain at least one item")
    private List<OrderItemDto> items;

    @NotNull(message = "Total amount is required")
    @Min(value = 0, message = "Total amount must be greater than 0")
    private BigDecimal totalAmount;

    @NotNull(message = "Marketplace source is required")
    private String marketplaceSource;
}
