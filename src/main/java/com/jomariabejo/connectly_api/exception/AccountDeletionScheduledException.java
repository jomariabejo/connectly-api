package com.jomariabejo.connectly_api.exception;

import java.time.LocalDateTime;

/**
 * Exception thrown when attempting to access or interact with an account that has been scheduled for deletion.
 * This occurs when a user tries to authenticate or perform actions on an account within the 30-day grace period.
 */
public class AccountDeletionScheduledException extends RuntimeException {
    private final LocalDateTime scheduledDeletionAt;
    private final LocalDateTime deletedAt;

    public AccountDeletionScheduledException(String message, LocalDateTime deletedAt, LocalDateTime scheduledDeletionAt) {
        super(message);
        this.deletedAt = deletedAt;
        this.scheduledDeletionAt = scheduledDeletionAt;
    }

    public AccountDeletionScheduledException(LocalDateTime deletedAt, LocalDateTime scheduledDeletionAt) {
        super("Account has been scheduled for deletion at: " + scheduledDeletionAt + ". Deleted at: " + deletedAt);
        this.deletedAt = deletedAt;
        this.scheduledDeletionAt = scheduledDeletionAt;
    }

    public LocalDateTime getScheduledDeletionAt() {
        return scheduledDeletionAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
