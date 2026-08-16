package com.jomariabejo.connectly_api.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OtpGenerator}.
 *
 * <p>Both generators are static and backed by {@link java.security.SecureRandom} /
 * {@link UUID#randomUUID()}, so the tests assert format properties rather than exact values. The
 * OTP check runs a few hundred iterations: {@code String.format("%06d", ...)} must left-pad small
 * draws, and over ~200 samples of a uniform six-digit space a sub-six-digit raw value is
 * overwhelmingly likely to appear, exercising the padding without any seeding tricks -- while a
 * regression to an unpadded format fails deterministically on the first short draw.
 */
@DisplayName("OtpGenerator")
class OtpGeneratorTest {

    @Nested
    @DisplayName("generateOtp")
    class GenerateOtp {

        @Test
        @DisplayName("always returns exactly six digits, zero-padded")
        void alwaysSixDigits() {
            for (int i = 0; i < 200; i++) {
                assertThat(OtpGenerator.generateOtp()).matches("\\d{6}");
            }
        }
    }

    @Nested
    @DisplayName("generateSecureToken")
    class GenerateSecureToken {

        @Test
        @DisplayName("returns a canonical 36-character UUID string")
        void isCanonicalUuid() {
            String token = OtpGenerator.generateSecureToken();

            assertThat(token).hasSize(36);
            assertThat(UUID.fromString(token)).hasToString(token);
        }

        @Test
        @DisplayName("returns a different token on each call")
        void successiveTokensDiffer() {
            String first = OtpGenerator.generateSecureToken();
            String second = OtpGenerator.generateSecureToken();

            assertThat(first).isNotEqualTo(second);
        }
    }
}
