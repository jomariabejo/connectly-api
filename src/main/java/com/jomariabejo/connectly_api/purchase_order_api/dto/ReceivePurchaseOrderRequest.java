package com.jomariabejo.connectly_api.purchase_order_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceivePurchaseOrderRequest {
    @NotEmpty
    @Valid
    private List<ReceivePurchaseOrderLineRequest> lines;
}
