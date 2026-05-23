package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.config.AccountDeletionConfig;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.model.VerificationToken;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class DeleteAccountResult {

    private final User user;
    private final VerificationToken reactivationToken;

    public DeleteAccountResult(User user, VerificationToken reactivationToken) {
        this.user = user;
        this.reactivationToken = reactivationToken;
    }

    public User getUser() {
        return user;
    }

    public VerificationToken getReactivationToken() {
        return reactivationToken;
    }

    public int getGracePeriodDays() {
        return AccountDeletionConfig.GRACE_PERIOD_DAYS;
    }

    public LocalDateTime getDeletedAt() {
        return LocalDateTime.ofInstant(user.getDeletedAt().toInstant(), ZoneId.systemDefault());
    }

    public LocalDateTime getScheduledDeletionAt() {
        return LocalDateTime.ofInstant(user.getScheduledDeletionAt().toInstant(), ZoneId.systemDefault());
    }
}
