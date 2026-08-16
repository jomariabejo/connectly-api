package com.jomariabejo.connectly_api.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jomariabejo.connectly_api.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AuditService}.
 *
 * <p>The service's only observable output is what it writes to its SLF4J logger, so instead of
 * mocking anything (it has no dependencies to mock) each test attaches a Logback
 * {@link ListAppender} to the {@code AuditService} logger, invokes one audit method and inspects
 * the captured {@link ILoggingEvent}. The appender is detached again in {@link #detachAppender()}
 * so no state leaks between tests or into other test classes sharing the JVM.
 *
 * <p>Every assertion checks two things: the event marker that log scrapers key on, and a
 * {@code yyyy-MM-dd HH:mm:ss} timestamp inside the message. The timestamp regex pins the
 * thread-safe {@link java.time.format.DateTimeFormatter} replacement for the old
 * {@code SimpleDateFormat} -- the format must have survived the swap unchanged.
 */
@DisplayName("AuditService")
class AuditServiceTest {

    private static final String TIMESTAMP_PATTERN = "timestamp=\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";

    private final AuditService auditService = new AuditService();

    private Logger auditLogger;
    private ListAppender<ILoggingEvent> appender;

    private User user;

    @BeforeEach
    void attachAppender() {
        auditLogger = (Logger) LoggerFactory.getLogger(AuditService.class);
        appender = new ListAppender<>();
        appender.start();
        auditLogger.addAppender(appender);

        user = new User("someone", "hashed", "someone@example.com");
        user.setId(42L);
    }

    @AfterEach
    void detachAppender() {
        auditLogger.detachAppender(appender);
        appender.stop();
    }

    private String onlyLoggedMessage() {
        assertThat(appender.list).hasSize(1);
        return appender.list.get(0).getFormattedMessage();
    }

    @Nested
    @DisplayName("logPasswordResetInitiated")
    class LogPasswordResetInitiated {

        @Test
        @DisplayName("writes a PASSWORD_RESET_INITIATED entry with a formatted timestamp")
        void logsInitiatedEvent() {
            auditService.logPasswordResetInitiated(user, "EMAIL_LINK");

            String message = onlyLoggedMessage();
            assertThat(message)
                    .contains("PASSWORD_RESET_INITIATED")
                    .contains("userId=42")
                    .contains("email=someone@example.com")
                    .contains("method=EMAIL_LINK")
                    .containsPattern(TIMESTAMP_PATTERN);
        }
    }

    @Nested
    @DisplayName("logPasswordReset")
    class LogPasswordReset {

        @Test
        @DisplayName("writes a PASSWORD_RESET_ATTEMPT entry with status=SUCCESS")
        void logsSuccessfulAttempt() {
            auditService.logPasswordReset(user, true, "OTP", "Password reset completed");

            String message = onlyLoggedMessage();
            assertThat(message)
                    .contains("PASSWORD_RESET_ATTEMPT")
                    .contains("status=SUCCESS")
                    .contains("method=OTP")
                    .contains("reason=Password reset completed")
                    .containsPattern(TIMESTAMP_PATTERN);
        }

        @Test
        @DisplayName("writes a PASSWORD_RESET_ATTEMPT entry with status=FAILURE")
        void logsFailedAttempt() {
            auditService.logPasswordReset(user, false, "EMAIL_LINK", "Token expired");

            String message = onlyLoggedMessage();
            assertThat(message)
                    .contains("PASSWORD_RESET_ATTEMPT")
                    .contains("status=FAILURE")
                    .contains("reason=Token expired")
                    .containsPattern(TIMESTAMP_PATTERN);
        }
    }

    @Nested
    @DisplayName("logInvalidResetAttempt")
    class LogInvalidResetAttempt {

        @Test
        @DisplayName("writes a PASSWORD_RESET_INVALID_ATTEMPT entry with a formatted timestamp")
        void logsInvalidAttempt() {
            auditService.logInvalidResetAttempt("ghost@example.com", "EMAIL_LINK", "Email not found");

            String message = onlyLoggedMessage();
            assertThat(message)
                    .contains("PASSWORD_RESET_INVALID_ATTEMPT")
                    .contains("email=ghost@example.com")
                    .contains("reason=Email not found")
                    .containsPattern(TIMESTAMP_PATTERN);
        }
    }

    @Nested
    @DisplayName("logRateLimitExceeded")
    class LogRateLimitExceeded {

        @Test
        @DisplayName("writes a PASSWORD_RESET_RATE_LIMIT_EXCEEDED entry with a formatted timestamp")
        void logsRateLimitEvent() {
            auditService.logRateLimitExceeded("someone@example.com", "forgot_password_email");

            String message = onlyLoggedMessage();
            assertThat(message)
                    .contains("PASSWORD_RESET_RATE_LIMIT_EXCEEDED")
                    .contains("email=someone@example.com")
                    .contains("action=forgot_password_email")
                    .containsPattern(TIMESTAMP_PATTERN);
        }
    }
}
