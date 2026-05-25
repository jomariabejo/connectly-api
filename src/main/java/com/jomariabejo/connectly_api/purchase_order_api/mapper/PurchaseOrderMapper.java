package com.jomariabejo.connectly_api.purchase_order_api.mapper;

import com.jomariabejo.connectly_api.purchase_order_api.dto.PurchaseOrderLineResponse;
import com.jomariabejo.connectly_api.purchase_order_api.dto.PurchaseOrderResponse;
import com.jomariabejo.connectly_api.purchase_order_api.dto.PurchaseOrderSummaryDto;
import com.jomariabejo.connectly_api.purchase_order_api.entity.PurchaseOrder;
import com.jomariabejo.connectly_api.purchase_order_api.entity.PurchaseOrderLine;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PurchaseOrderMapper {

    public PurchaseOrderResponse toResponse(PurchaseOrder order) {
        String createdByName = null;
        if (order.getCreatedBy() != null) {
            String first = order.getCreatedBy().getFirstName() != null ? order.getCreatedBy().getFirstName() : "";
            String last = order.getCreatedBy().getLastName() != null ? order.getCreatedBy().getLastName() : "";
            createdByName = (first + " " + last).trim();
            if (createdByName.isEmpty()) {
                createdByName = order.getCreatedBy().getUsername();
            }
        }
        return PurchaseOrderResponse.builder()
                .id(order.getId())
                .poNumber(order.getPoNumber())
                .supplierName(order.getSupplierName())
                .status(order.getStatus())
                .notes(order.getNotes())
                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                .totalAmount(order.getTotalAmount())
                .createdByUserId(order.getCreatedBy() != null ? order.getCreatedBy().getId() : null)
                .createdByName(createdByName != null ? createdByName.trim() : null)
                .createdDate(order.getCreatedDate())
                .updatedDate(order.getUpdatedDate())
                .lines(order.getLines().stream().map(this::toLineResponse).collect(Collectors.toList()))
                .build();
    }

    public PurchaseOrderLineResponse toLineResponse(PurchaseOrderLine line) {
        return PurchaseOrderLineResponse.builder()
                .id(line.getId())
                .sku(line.getSku())
                .productName(line.getProductName())
                .quantityOrdered(line.getQuantityOrdered())
                .quantityReceived(line.getQuantityReceived())
                .unitCost(line.getUnitCost())
                .lineTotal(line.getLineTotal())
                .build();
    }

    public PurchaseOrderSummaryDto toSummary(PurchaseOrder order) {
        return PurchaseOrderSummaryDto.builder()
                .id(order.getId())
                .poNumber(order.getPoNumber())
                .supplierName(order.getSupplierName())
                .status(order.getStatus())
                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                .totalAmount(order.getTotalAmount())
                .lineCount(order.getLines() != null ? order.getLines().size() : 0)
                .createdDate(order.getCreatedDate())
                .build();
    }

    public List<PurchaseOrderSummaryDto> toSummaries(List<PurchaseOrder> orders) {
        return orders.stream().map(this::toSummary).collect(Collectors.toList());
    }
}
