package com.jomariabejo.connectly_api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RateLimitingService}.
 *
 * <p>No mocks -- the tracker is a plain in-memory map. Its two {@code @Value} fields are seeded with
 * {@link ReflectionTestUtils}; without that {@code maxForgotPasswordAttempts} would be 0 and the
 * very first call would already look rate limited.
 */
@DisplayName("RateLimitingService")
class RateLimitingServiceTest {

    private static final String EMAIL = "someone@example.com";
    private static final String ACTION = "forgot_password_email";
    private static final int MAX_ATTEMPTS = 5;

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService();
        ReflectionTestUtils.setField(rateLimitingService, "maxForgotPasswordAttempts", MAX_ATTEMPTS);
        ReflectionTestUtils.setField(rateLimitingService, "attemptWindowMinutes", 60);
    }

    @Test
    @DisplayName("an address with no history is not limited")
    void unknownAddressIsNotLimited() {
        assertThat(rateLimitingService.isRateLimited(EMAIL, ACTION)).isFalse();
    }

    @Test
    @DisplayName("stays open until the attempt count reaches the maximum")
    void allowsUpToTheLimit() {
        for (int i = 1; i < MAX_ATTEMPTS; i++) {
            rateLimitingService.recordAttempt(EMAIL, ACTION);
            assertThat(rateLimitingService.isRateLimited(EMAIL, ACTION))
                    .as("after %d of %d attempts", i, MAX_ATTEMPTS)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("closes on the maximum attempt and stays closed beyond it")
    void limitsAtAndAboveTheMaximum() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            rateLimitingService.recordAttempt(EMAIL, ACTION);
        }
        assertThat(rateLimitingService.isRateLimited(EMAIL, ACTION)).isTrue();

        rateLimitingService.recordAttempt(EMAIL, ACTION);
        assertThat(rateLimitingService.isRateLimited(EMAIL, ACTION)).isTrue();
    }

    @Test
    @DisplayName("counts each action separately for the same address")
    void tracksActionsIndependently() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            rateLimitingService.recordAttempt(EMAIL, ACTION);
        }

        assertThat(rateLimitingService.isRateLimited(EMAIL, ACTION)).isTrue();
        assertThat(rateLimitingService.isRateLimited(EMAIL, "forgot_password_otp"))
                .as("the OTP flow has its own budget")
                .isFalse();
    }

    @Test
    @DisplayName("counts each address separately for the same action")
    void tracksAddressesIndependently() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            rateLimitingService.recordAttempt(EMAIL, ACTION);
        }

        assertThat(rateLimitingService.isRateLimited(EMAIL, ACTION)).isTrue();
        assertThat(rateLimitingService.isRateLimited("other@example.com", ACTION)).isFalse();
    }

    @Test
    @DisplayName("attempts older than the window stop counting")
    void expiresAttemptsOutsideTheWindow() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            rateLimitingService.recordAttempt(EMAIL, ACTION);
        }
        assertThat(rateLimitingService.isRateLimited(EMAIL, ACTION)).isTrue();

        // The service derives windowStart as `now - windowMinutes`, so a negative window puts
        // windowStart in the future and strands every recorded timestamp behind it. That is the
        // same arithmetic as a real 60-minute window rolling past, without sleeping for an hour
        // or depending on how many milliseconds the loop above happened to take.
        ReflectionTestUtils.setField(rateLimitingService, "attemptWindowMinutes", -1);

        assertThat(rateLimitingService.isRateLimited(EMAIL, ACTION)).isFalse();
    }

    @Test
    @DisplayName("clearAttemptTrackers wipes all history")
    void clearResetsEverything() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            rateLimitingService.recordAttempt(EMAIL, ACTION);
        }
        assertThat(rateLimitingService.isRateLimited(EMAIL, ACTION)).isTrue();

        rateLimitingService.clearAttemptTrackers();

        assertThat(rateLimitingService.isRateLimited(EMAIL, ACTION)).isFalse();
    }
}
