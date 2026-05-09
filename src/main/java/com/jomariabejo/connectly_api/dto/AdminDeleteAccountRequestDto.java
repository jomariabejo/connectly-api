package com.jomariabejo.connectly_api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for admin account deletion requests.
 * Allows admins to force permanent deletion or extend grace period.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminDeleteAccountRequestDto {
    
    private boolean forceDelete = false;
    
    private String reason;
    
    private Integer extensionDays;
}
