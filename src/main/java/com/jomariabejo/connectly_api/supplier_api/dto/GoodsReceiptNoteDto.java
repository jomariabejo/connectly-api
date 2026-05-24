package com.jomariabejo.connectly_api.supplier_api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptNoteDto {
    private Long id;
    private String grnNumber;
    private Long purchaseOrderId;
    private String poNumber;
    private Long supplierId;
    private String supplierName;
    private LocalDateTime receiptDate;
    private String status;
    private BigDecimal totalQuantityReceived;
    private LocalDateTime createdAt;
}
