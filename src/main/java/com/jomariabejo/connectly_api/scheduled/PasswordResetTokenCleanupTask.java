package com.jomariabejo.connectly_api.scheduled;

import com.jomariabejo.connectly_api.service.PasswordResetTokenService;
import com.jomariabejo.connectly_api.service.VerificationTokenService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenCleanupTask {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetTokenCleanupTask.class);

    private final PasswordResetTokenService passwordResetTokenService;
    private final VerificationTokenService verificationTokenService;

    /**
     * Scheduled task to delete expired password reset tokens
     * Runs every hour (3600000 ms)
     */
    @Scheduled(fixedRateString = "${security.password.reset.cleanup-interval:3600000}")
    @SchedulerLock(name = "cleanupExpiredTokens", lockAtLeastFor = "PT1M", lockAtMostFor = "PT10M")
    public void cleanupExpiredTokens() {
        logger.info("Starting password reset token cleanup task");
        try {
            passwordResetTokenService.deleteExpiredTokens();
            // VerificationTokenService.cleanupExpiredTokens() existed but carried no @Scheduled
            // annotation, so nothing ever called it and expired verification tokens accumulated.
            verificationTokenService.cleanupExpiredTokens();
            logger.info("Token cleanup completed successfully");
        } catch (Exception e) {
            logger.error("Error during password reset token cleanup", e);
        }
    }
}
