package com.jomariabejo.connectly_api.purchase_order_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePurchaseOrderRequest {
    private String supplierName;
    private String notes;
    private LocalDate expectedDeliveryDate;

    @NotEmpty
    @Valid
    private List<CreatePurchaseOrderLineRequest> lines;
}
