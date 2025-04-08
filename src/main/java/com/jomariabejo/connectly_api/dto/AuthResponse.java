package com.jomariabejo.connectly_api.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AuthResponse {
    private String message;
    private boolean success;
    private String token;

    public AuthResponse(boolean success,String message) {
        this.message = message;
        this.success = success;
    }

    public AuthResponse(String message, boolean success, String token) {
        this.message = message;
        this.success = success;
        this.token = token;
    }

}
