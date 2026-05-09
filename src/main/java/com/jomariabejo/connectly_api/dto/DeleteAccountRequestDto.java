package com.jomariabejo.connectly_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for user account deletion requests.
 * Contains optional reason for deletion and auto-reactivation preference.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAccountRequestDto {
    
    private String reason;
    
    private boolean autoReactivationEnabled = true;
}
