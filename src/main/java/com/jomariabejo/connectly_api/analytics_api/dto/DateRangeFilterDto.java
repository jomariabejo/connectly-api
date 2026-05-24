package com.jomariabejo.connectly_api.analytics_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DateRangeFilterDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private String periodType; // DAILY, WEEKLY, MONTHLY, YEARLY
}
