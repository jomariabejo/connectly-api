package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.exception.InvalidPasswordResetTokenException;
import com.jomariabejo.connectly_api.exception.PasswordResetAttemptsExceededException;
import com.jomariabejo.connectly_api.exception.PasswordResetTokenExpiredException;
import com.jomariabejo.connectly_api.model.PasswordResetToken;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PasswordResetTokenService}.
 *
 * <p>The service reads {@code security.password.reset.max-otp-attempts} through a {@code @Value}
 * field. Plain Mockito performs no property injection, so {@code maxOtpAttempts} would stay at the
 * primitive default of 0 and every OTP validation would immediately trip the attempts guard.
 * {@link ReflectionTestUtils} therefore seeds it to 3 (the production default) in {@link #setUp()}.
 *
 * <p>Token fixtures are built through the entity's own constructors, which stamp a 15-minute
 * expiry. Expired fixtures overwrite {@code expiryDate} with a fixed instant one minute in the
 * past, so the expiry checks are deterministic and no test ever sleeps.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetTokenService")
class PasswordResetTokenServiceTest {

    private static final int MAX_OTP_ATTEMPTS = 3;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private PasswordResetTokenService passwordResetTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetTokenService, "maxOtpAttempts", MAX_OTP_ATTEMPTS);

        user = new User("someone", "hashed", "someone@example.com");
        user.setId(1L);
    }

    private PasswordResetToken linkToken(String token) {
        return new PasswordResetToken(token, user);
    }

    private PasswordResetToken otpToken(String otp) {
        return new PasswordResetToken("token-" + otp, otp, user, PasswordResetToken.TokenType.OTP);
    }

    private static void expire(PasswordResetToken token) {
        token.setExpiryDate(new Date(System.currentTimeMillis() - 60_000L));
    }

    @Nested
    @DisplayName("createEmailResetToken")
    class CreateEmailResetToken {

        @Test
        @DisplayName("invalidates prior unused tokens, then saves a LINK token carrying the returned value")
        void invalidatesPriorTokensThenSavesLinkToken() {
            PasswordResetToken stale1 = linkToken("stale-1");
            PasswordResetToken stale2 = linkToken("stale-2");
            when(passwordResetTokenRepository.findAllUnusedByUser(user))
                    .thenReturn(List.of(stale1, stale2));

            String token = passwordResetTokenService.createEmailResetToken(user);

            ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(passwordResetTokenRepository, times(3)).save(saved.capture());

            // The two invalidation saves come first, the freshly minted token last.
            assertThat(saved.getAllValues().subList(0, 2)).containsExactly(stale1, stale2);
            assertThat(stale1.isUsed()).as("prior token must be burned").isTrue();
            assertThat(stale2.isUsed()).as("prior token must be burned").isTrue();

            PasswordResetToken fresh = saved.getAllValues().get(2);
            assertThat(fresh.getTokenType()).isEqualTo(PasswordResetToken.TokenType.LINK);
            assertThat(fresh.getToken()).isEqualTo(token);
            assertThat(fresh.getUser()).isEqualTo(user);
            assertThat(fresh.isUsed()).isFalse();
            assertThat(fresh.getAttemptCount()).isZero();
        }

        @Test
        @DisplayName("returns a UUID-shaped token")
        void returnsUuidShapedToken() {
            when(passwordResetTokenRepository.findAllUnusedByUser(user)).thenReturn(List.of());

            String token = passwordResetTokenService.createEmailResetToken(user);

            // The value is random, so assert the shape: canonical UUIDs are 36 characters
            // and survive a UUID.fromString round-trip unchanged.
            assertThat(token).hasSize(36);
            assertThat(UUID.fromString(token)).hasToString(token);
        }
    }

    @Nested
    @DisplayName("createOtpResetToken")
    class CreateOtpResetToken {

        @Test
        @DisplayName("returns a six-digit one-time code")
        void returnsSixDigitOtp() {
            when(passwordResetTokenRepository.findAllUnusedByUser(user)).thenReturn(List.of());

            String otp = passwordResetTokenService.createOtpResetToken(user);

            // The code is random, so assert the format rather than a value.
            assertThat(otp).matches("\\d{6}");
        }

        @Test
        @DisplayName("saves an OTP-typed entity carrying the returned code")
        void savesOtpEntityCarryingReturnedCode() {
            when(passwordResetTokenRepository.findAllUnusedByUser(user)).thenReturn(List.of());

            String otp = passwordResetTokenService.createOtpResetToken(user);

            ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(passwordResetTokenRepository).save(saved.capture());

            PasswordResetToken persisted = saved.getValue();
            assertThat(persisted.getTokenType()).isEqualTo(PasswordResetToken.TokenType.OTP);
            assertThat(persisted.getOtp())
                    .as("the persisted entity must carry the code that was handed to the caller")
                    .isEqualTo(otp);
            assertThat(persisted.getToken()).isNotBlank();
            assertThat(persisted.getUser()).isEqualTo(user);
            assertThat(persisted.isUsed()).isFalse();
            assertThat(persisted.getAttemptCount()).isZero();
        }
    }

    @Nested
    @DisplayName("invalidateUserTokens")
    class InvalidateUserTokens {

        @Test
        @DisplayName("marks every unused token as used and persists each one")
        void marksEveryUnusedTokenUsed() {
            PasswordResetToken first = linkToken("first");
            PasswordResetToken second = linkToken("second");
            when(passwordResetTokenRepository.findAllUnusedByUser(user))
                    .thenReturn(List.of(first, second));

            passwordResetTokenService.invalidateUserTokens(user);

            assertThat(first.isUsed()).isTrue();
            assertThat(second.isUsed()).isTrue();
            verify(passwordResetTokenRepository).save(first);
            verify(passwordResetTokenRepository).save(second);
        }

        @Test
        @DisplayName("saves nothing when the user has no unused tokens")
        void savesNothingWhenNoUnusedTokens() {
            when(passwordResetTokenRepository.findAllUnusedByUser(user)).thenReturn(List.of());

            passwordResetTokenService.invalidateUserTokens(user);

            verify(passwordResetTokenRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("throws InvalidPasswordResetTokenException for an unknown token")
        void throwsForUnknownToken() {
            when(passwordResetTokenRepository.findByToken("nope")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetTokenService.validateToken("nope"))
                    .isInstanceOf(InvalidPasswordResetTokenException.class)
                    .hasMessage("Invalid password reset token");
        }

        @Test
        @DisplayName("throws PasswordResetTokenExpiredException for an expired token")
        void throwsForExpiredToken() {
            PasswordResetToken token = linkToken("expired");
            expire(token);
            when(passwordResetTokenRepository.findByToken("expired")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> passwordResetTokenService.validateToken("expired"))
                    .isInstanceOf(PasswordResetTokenExpiredException.class)
                    .hasMessage("Password reset token has expired");
        }

        @Test
        @DisplayName("throws InvalidPasswordResetTokenException for an already-used token")
        void throwsForUsedToken() {
            PasswordResetToken token = linkToken("burned");
            token.setUsed(true);
            when(passwordResetTokenRepository.findByToken("burned")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> passwordResetTokenService.validateToken("burned"))
                    .isInstanceOf(InvalidPasswordResetTokenException.class)
                    .hasMessage("Password reset token has already been used");
        }

        @Test
        @DisplayName("returns the token when it is known, unexpired and unused")
        void returnsValidToken() {
            PasswordResetToken token = linkToken("valid");
            when(passwordResetTokenRepository.findByToken("valid")).thenReturn(Optional.of(token));

            assertThat(passwordResetTokenService.validateToken("valid")).isSameAs(token);
        }
    }

    @Nested
    @DisplayName("validateOtp")
    class ValidateOtp {

        @Test
        @DisplayName("throws InvalidPasswordResetTokenException for an unknown code")
        void throwsForUnknownOtp() {
            when(passwordResetTokenRepository.findByOtp("000000")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetTokenService.validateOtp("000000"))
                    .isInstanceOf(InvalidPasswordResetTokenException.class)
                    .hasMessage("Invalid OTP");
        }

        @Test
        @DisplayName("reports expiry before the attempts guard when a token is both expired and at max attempts")
        void expiryIsCheckedBeforeAttempts() {
            PasswordResetToken token = otpToken("123456");
            expire(token);
            token.setAttemptCount(MAX_OTP_ATTEMPTS);
            when(passwordResetTokenRepository.findByOtp("123456")).thenReturn(Optional.of(token));

            // Were attempts checked first, this would be PasswordResetAttemptsExceededException
            // and the token would be burned and saved. The expiry path does neither.
            assertThatThrownBy(() -> passwordResetTokenService.validateOtp("123456"))
                    .isInstanceOf(PasswordResetTokenExpiredException.class)
                    .hasMessage("OTP has expired");

            verify(passwordResetTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws InvalidPasswordResetTokenException for an already-used code")
        void throwsForUsedOtp() {
            PasswordResetToken token = otpToken("123456");
            token.setUsed(true);
            when(passwordResetTokenRepository.findByOtp("123456")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> passwordResetTokenService.validateOtp("123456"))
                    .isInstanceOf(InvalidPasswordResetTokenException.class)
                    .hasMessage("OTP has already been used");
        }

        @Test
        @DisplayName("burns the token and throws once the attempt count reaches the maximum")
        void burnsTokenAtMaxAttempts() {
            PasswordResetToken token = otpToken("123456");
            token.setAttemptCount(MAX_OTP_ATTEMPTS);
            when(passwordResetTokenRepository.findByOtp("123456")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> passwordResetTokenService.validateOtp("123456"))
                    .isInstanceOf(PasswordResetAttemptsExceededException.class)
                    .hasMessageContaining("Maximum OTP validation attempts exceeded");

            assertThat(token.isUsed())
                    .as("an exhausted OTP must be unusable afterwards")
                    .isTrue();
            verify(passwordResetTokenRepository).save(token);
        }

        @Test
        @DisplayName("increments and persists the attempt count on a successful validation")
        void incrementsAttemptCountOnSuccess() {
            PasswordResetToken token = otpToken("123456");
            when(passwordResetTokenRepository.findByOtp("123456")).thenReturn(Optional.of(token));

            PasswordResetToken result = passwordResetTokenService.validateOtp("123456");

            assertThat(result).isSameAs(token);

            ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(passwordResetTokenRepository).save(saved.capture());
            assertThat(saved.getValue().getAttemptCount()).isEqualTo(1);
            assertThat(saved.getValue().isUsed()).isFalse();
        }

        @Test
        @DisplayName("still succeeds at attemptCount = max - 1, consuming the final attempt")
        void succeedsAtBoundaryOneBelowMax() {
            PasswordResetToken token = otpToken("123456");
            token.setAttemptCount(MAX_OTP_ATTEMPTS - 1);
            when(passwordResetTokenRepository.findByOtp("123456")).thenReturn(Optional.of(token));

            PasswordResetToken result = passwordResetTokenService.validateOtp("123456");

            assertThat(result).isSameAs(token);
            assertThat(token.getAttemptCount()).isEqualTo(MAX_OTP_ATTEMPTS);
            assertThat(token.isUsed()).isFalse();
            verify(passwordResetTokenRepository).save(token);
        }
    }

    @Nested
    @DisplayName("markTokenAsUsed")
    class MarkTokenAsUsed {

        @Test
        @DisplayName("flips the used flag and persists the token")
        void persistsUsedFlag() {
            PasswordResetToken token = linkToken("valid");

            passwordResetTokenService.markTokenAsUsed(token);

            assertThat(token.isUsed()).isTrue();
            verify(passwordResetTokenRepository).save(token);
        }
    }

    @Nested
    @DisplayName("deleteExpiredTokens")
    class DeleteExpiredTokens {

        @Test
        @DisplayName("deletes every expired token the repository reports")
        void deletesFoundTokens() {
            PasswordResetToken first = linkToken("expired-1");
            PasswordResetToken second = linkToken("expired-2");
            expire(first);
            expire(second);
            List<PasswordResetToken> expired = List.of(first, second);
            when(passwordResetTokenRepository.findExpiredTokens()).thenReturn(expired);

            passwordResetTokenService.deleteExpiredTokens();

            verify(passwordResetTokenRepository).deleteAll(expired);
        }

        @Test
        @DisplayName("skips the delete entirely when nothing has expired")
        void skipsDeleteWhenNothingExpired() {
            when(passwordResetTokenRepository.findExpiredTokens()).thenReturn(List.of());

            passwordResetTokenService.deleteExpiredTokens();

            verify(passwordResetTokenRepository, never()).deleteAll(anyIterable());
            verify(passwordResetTokenRepository, never()).deleteAll();
        }
    }
}
