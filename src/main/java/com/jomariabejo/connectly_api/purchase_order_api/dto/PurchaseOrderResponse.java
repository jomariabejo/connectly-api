package com.jomariabejo.connectly_api.purchase_order_api.dto;

import com.jomariabejo.connectly_api.purchase_order_api.entity.PurchaseOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderResponse {
    private Long id;
    private String poNumber;
    private String supplierName;
    private PurchaseOrderStatus status;
    private String notes;
    private LocalDate expectedDeliveryDate;
    private BigDecimal totalAmount;
    private Long createdByUserId;
    private String createdByName;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private List<PurchaseOrderLineResponse> lines;
}
