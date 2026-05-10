package com.jomariabejo.connectly_api.exception;

public class InvalidPasswordResetTokenException extends RuntimeException {
    public InvalidPasswordResetTokenException(String message) {
        super(message);
    }

    public InvalidPasswordResetTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
