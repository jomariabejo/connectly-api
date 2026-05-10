package com.jomariabejo.connectly_api.exception;

public class PasswordResetTokenExpiredException extends RuntimeException {
    public PasswordResetTokenExpiredException(String message) {
        super(message);
    }

    public PasswordResetTokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
