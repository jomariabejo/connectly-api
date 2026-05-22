package com.jomariabejo.connectly_api.inventory_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdjustInventoryRequest {
    @NotNull(message = "Quantity delta is required")
    private Integer quantityDelta;

    private String reason;
}
