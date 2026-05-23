package com.jomariabejo.connectly_api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for user account deletion requests.
 * Auto-reactivation is managed through user settings, not at deletion time.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAccountRequestDto {
    
    private String reason;
}
