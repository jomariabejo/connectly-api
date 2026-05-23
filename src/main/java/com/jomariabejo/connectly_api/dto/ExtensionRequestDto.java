package com.jomariabejo.connectly_api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExtensionRequestDto {

    @NotNull(message = "Extension days are required")
    @Positive(message = "Extension days must be positive")
    private Integer extensionDays;
}
