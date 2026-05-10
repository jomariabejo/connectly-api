package com.jomariabejo.connectly_api.exception;

public class PasswordResetAttemptsExceededException extends RuntimeException {
    public PasswordResetAttemptsExceededException(String message) {
        super(message);
    }

    public PasswordResetAttemptsExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
