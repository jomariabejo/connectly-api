package com.jomariabejo.connectly_api.inventory_api.mapper;

import com.jomariabejo.connectly_api.inventory_api.dto.InventoryItemDto;
import com.jomariabejo.connectly_api.inventory_api.entity.InventoryItem;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {
    public InventoryItemDto toDto(InventoryItem item) {
        if (item == null) {
            return null;
        }
        return InventoryItemDto.builder()
                .sku(item.getSku())
                .name(item.getName())
                .description(item.getDescription())
                .unitPrice(item.getUnitPrice())
                .currency(item.getCurrency())
                .onHandQuantity(item.getOnHandQuantity())
                .reservedQuantity(item.getReservedQuantity())
                .availableQuantity(item.getAvailableQuantity())
                .active(item.getActive())
                .createdDate(item.getCreatedDate())
                .updatedDate(item.getUpdatedDate())
                .build();
    }
}
