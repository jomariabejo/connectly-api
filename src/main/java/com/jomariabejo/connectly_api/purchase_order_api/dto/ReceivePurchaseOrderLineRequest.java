package com.jomariabejo.connectly_api.purchase_order_api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceivePurchaseOrderLineRequest {
    @NotBlank
    private String sku;

    @NotNull
    @Min(1)
    private Integer quantityReceived;
}
