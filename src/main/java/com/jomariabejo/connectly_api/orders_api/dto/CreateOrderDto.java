package com.jomariabejo.connectly_api.orders_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
    @Valid
    private List<OrderItemDto> items;

    private BigDecimal totalAmount;

    @NotNull(message = "Marketplace source is required")
    private String marketplaceSource;
}
