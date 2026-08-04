package com.jomariabejo.connectly_api.exception;

/**
 * Raised when a password fails the strength rules — on registration or on reset.
 *
 * <p>Both flows share one definition of "strong" so an account can never be created with a
 * password its owner would be unable to reset back to.
 */
public class WeakPasswordException extends RuntimeException {
    public WeakPasswordException(String message) {
        super(message);
    }
}
