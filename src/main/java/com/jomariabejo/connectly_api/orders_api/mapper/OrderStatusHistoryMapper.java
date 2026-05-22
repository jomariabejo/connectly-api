package com.jomariabejo.connectly_api.orders_api.mapper;

import com.jomariabejo.connectly_api.orders_api.dto.OrderStatusHistoryDto;
import com.jomariabejo.connectly_api.orders_api.entity.OrderStatusHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderStatusHistoryMapper {

    @Mapping(target = "changedByUsername", source = "changedBy.username")
    @Mapping(target = "changedById", source = "changedBy.id")
    OrderStatusHistoryDto toDto(OrderStatusHistory entity);

    @Mapping(target = "changedBy", ignore = true)
    OrderStatusHistory toEntity(OrderStatusHistoryDto dto);

    List<OrderStatusHistoryDto> toDtoList(List<OrderStatusHistory> entityList);
}
