package com.jomariabejo.connectly_api.orders_api.dto;

import com.jomariabejo.connectly_api.orders_api.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderStatusDto {
    @NotNull(message = "New status is required")
    private OrderStatus newStatus;

    private String reason;

    @Builder.Default
    private String idempotencyKey = UUID.randomUUID().toString();
}
