package com.jomariabejo.connectly_api.config;

import java.time.Duration;

public final class AccountDeletionConfig {

    public static final int GRACE_PERIOD_DAYS = 30;
    public static final Duration GRACE_PERIOD = Duration.ofDays(GRACE_PERIOD_DAYS);
    public static final String REACTIVATION_RATE_LIMIT_ACTION = "account-reactivation";

    private AccountDeletionConfig() {
    }
}
