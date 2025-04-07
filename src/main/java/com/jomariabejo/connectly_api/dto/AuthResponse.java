package com.jomariabejo.connectly_api.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AuthResponse {
    private String message;
    private boolean success;

    public AuthResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }

}
