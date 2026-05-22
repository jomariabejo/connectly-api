package com.jomariabejo.connectly_api.orders_api.dto;

import com.jomariabejo.connectly_api.orders_api.entity.OrderStatus;
import com.jomariabejo.connectly_api.payments_api.entity.PaymentStatus;
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
public class OrderListItemDto {
    private Long id;
    private Long customerId;
    private String customerName;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private String marketplaceSource;
    private LocalDateTime createdDate;
    private Integer itemCount;
}
