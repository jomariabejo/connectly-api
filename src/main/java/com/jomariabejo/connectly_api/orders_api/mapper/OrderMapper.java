package com.jomariabejo.connectly_api.orders_api.mapper;

import com.jomariabejo.connectly_api.orders_api.dto.OrderListItemDto;
import com.jomariabejo.connectly_api.orders_api.dto.OrderResponseDto;
import com.jomariabejo.connectly_api.orders_api.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class, OrderStatusHistoryMapper.class})
public interface OrderMapper {

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.username")
    OrderResponseDto toResponseDto(Order entity);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.username")
    @Mapping(target = "itemCount", expression = "java(entity.getItems().size())")
    OrderListItemDto toListItemDto(Order entity);

    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "statusHistory", ignore = true)
    Order toEntity(OrderResponseDto dto);

    List<OrderResponseDto> toResponseDtoList(List<Order> entityList);

    List<OrderListItemDto> toListItemDtoList(List<Order> entityList);
}
