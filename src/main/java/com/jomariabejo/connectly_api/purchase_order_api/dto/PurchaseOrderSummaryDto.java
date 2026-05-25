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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderSummaryDto {
    private Long id;
    private String poNumber;
    private String supplierName;
    private PurchaseOrderStatus status;
    private LocalDate expectedDeliveryDate;
    private BigDecimal totalAmount;
    private int lineCount;
    private LocalDateTime createdDate;
}
