package com.jomariabejo.connectly_api.exception;

/**
 * Exception thrown when account reactivation fails due to:
 * - Invalid or expired reactivation token
 * - Grace period has passed (30 days)
 * - User is not in a deletable state
 * - Reactivation token is missing or invalid
 */
public class AccountReactivationFailedException extends RuntimeException {
    public AccountReactivationFailedException(String message) {
        super(message);
    }

    public AccountReactivationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
