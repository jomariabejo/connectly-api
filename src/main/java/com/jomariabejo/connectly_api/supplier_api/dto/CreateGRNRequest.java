package com.jomariabejo.connectly_api.supplier_api.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGRNRequest {
    private Long purchaseOrderId;
    private String notes;
}
