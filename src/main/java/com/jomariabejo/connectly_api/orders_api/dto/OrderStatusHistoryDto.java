package com.jomariabejo.connectly_api.orders_api.dto;

import com.jomariabejo.connectly_api.orders_api.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistoryDto {
    private Long id;
    private OrderStatus oldStatus;
    private OrderStatus newStatus;
    private LocalDateTime changedAt;
    private String changedByUsername;
    private Long changedById;
}
