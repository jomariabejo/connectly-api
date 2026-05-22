package com.jomariabejo.connectly_api.orders_api.mapper;

import com.jomariabejo.connectly_api.orders_api.dto.OrderItemDto;
import com.jomariabejo.connectly_api.orders_api.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    OrderItemDto toDto(OrderItem entity);

    OrderItem toEntity(OrderItemDto dto);

    List<OrderItemDto> toDtoList(List<OrderItem> entityList);

    List<OrderItem> toEntityList(List<OrderItemDto> dtoList);
}
