package com.jomariabejo.connectly_api.inventory_api.dto;

import com.jomariabejo.connectly_api.orders_api.dto.OrderItemDto;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
public class PricedOrderItem {
    List<OrderItemDto> items;
    BigDecimal totalAmount;
}
