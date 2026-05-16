package com.jomariabejo.connectly_api.orders_api.dto;

import com.jomariabejo.connectly_api.orders_api.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderFilterDto {
    private OrderStatus status;
    private String marketplaceSource;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private Long customerId;

    @Builder.Default
    @Min(value = 0, message = "Page must be >= 0")
    private Integer page = 0;

    @Builder.Default
    @Min(value = 1, message = "Page size must be >= 1")
    @Max(value = 100, message = "Page size must be <= 100")
    private Integer pageSize = 20;

    @Builder.Default
    private String sortBy = "createdDate";

    @Builder.Default
    private String sortOrder = "DESC";
}
