package com.jomariabejo.connectly_api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for account reactivation requests.
 * Contains the reactivation token sent to user's email.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReactivateAccountRequestDto {
    
    @NotBlank(message = "Reactivation token is required")
    private String reactivationToken;
}
