package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.model.VerificationToken;
import com.jomariabejo.connectly_api.repository.UserRepository;
import com.jomariabejo.connectly_api.repository.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link VerificationTokenService}.
 *
 * <p>Both repositories are plain Mockito mocks; the service has no {@code @Value} fields, so no
 * {@link org.springframework.test.util.ReflectionTestUtils} seeding is needed. A few choices worth
 * calling out:
 *
 * <ul>
 *   <li>{@link VerificationTokenRepository#findByUser} returns a bare (possibly {@code null})
 *       {@link VerificationToken}, not an {@link Optional} -- the "no existing token" tests lean on
 *       Mockito's default {@code null} return to exercise the service's null-safety.</li>
 *   <li>Expired and valid tokens are built through the no-arg constructor plus setters, because the
 *       {@code (token, user)} convenience constructor always stamps an expiry 24 hours in the
 *       future and would make the expiry branches untestable.</li>
 *   <li>Time-sensitive assertions bracket the call with {@code System.currentTimeMillis()} and
 *       assert a tolerant range instead of an exact instant.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationTokenService")
class VerificationTokenServiceTest {

    @Mock
    private VerificationTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VerificationTokenService verificationTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("someone", "hashed", "someone@example.com");
        user.setId(1L);
    }

    private VerificationToken tokenExpiringAt(String token, Date expiryDate) {
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setId(10L);
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(expiryDate);
        return verificationToken;
    }

    @Nested
    @DisplayName("createVerificationToken")
    class CreateVerificationToken {

        @Test
        @DisplayName("deletes the user's pre-existing token before saving the replacement")
        void replacesExistingToken() {
            VerificationToken existing = tokenExpiringAt("stale-token", new Date());
            when(tokenRepository.findByUser(user)).thenReturn(existing);
            when(tokenRepository.save(any(VerificationToken.class))).thenAnswer(inv -> inv.getArgument(0));

            verificationTokenService.createVerificationToken(user, "fresh-token");

            // A user may only hold one token, so the old row must be gone before the new insert.
            InOrder order = inOrder(tokenRepository);
            order.verify(tokenRepository).delete(existing);
            order.verify(tokenRepository).save(any(VerificationToken.class));
        }

        @Test
        @DisplayName("just saves when the user has no existing token (findByUser returns null)")
        void savesWhenNoExistingToken() {
            when(tokenRepository.findByUser(user)).thenReturn(null);
            when(tokenRepository.save(any(VerificationToken.class))).thenAnswer(inv -> inv.getArgument(0));

            long before = System.currentTimeMillis();
            VerificationToken result = verificationTokenService.createVerificationToken(user, "fresh-token");
            long after = System.currentTimeMillis();

            verify(tokenRepository, never()).delete(any(VerificationToken.class));

            ArgumentCaptor<VerificationToken> saved = ArgumentCaptor.forClass(VerificationToken.class);
            verify(tokenRepository).save(saved.capture());
            assertThat(result).isSameAs(saved.getValue());
            assertThat(result.getToken()).isEqualTo("fresh-token");
            assertThat(result.getUser()).isEqualTo(user);

            // The entity constructor stamps a 24-hour expiry; allow for the call's elapsed time.
            long dayMillis = TimeUnit.HOURS.toMillis(24);
            assertThat(result.getExpiryDate().getTime())
                    .isBetween(before + dayMillis, after + dayMillis);
        }
    }

    @Nested
    @DisplayName("getVerificationToken")
    class GetVerificationToken {

        @Test
        @DisplayName("delegates to the repository's findByToken")
        void delegatesToFindByToken() {
            VerificationToken token = tokenExpiringAt("some-token", new Date());
            when(tokenRepository.findByToken("some-token")).thenReturn(Optional.of(token));

            assertThat(verificationTokenService.getVerificationToken("some-token")).contains(token);
        }
    }

    @Nested
    @DisplayName("getTokenByUser")
    class GetTokenByUser {

        @Test
        @DisplayName("delegates to the repository's findByUser")
        void delegatesToFindByUser() {
            VerificationToken token = tokenExpiringAt("some-token", new Date());
            when(tokenRepository.findByUser(user)).thenReturn(token);

            assertThat(verificationTokenService.getTokenByUser(user)).isSameAs(token);
        }
    }

    @Nested
    @DisplayName("deleteToken")
    class DeleteToken {

        @Test
        @DisplayName("delegates to the repository's delete")
        void delegatesToDelete() {
            VerificationToken token = tokenExpiringAt("some-token", new Date());

            verificationTokenService.deleteToken(token);

            verify(tokenRepository).delete(token);
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("throws RuntimeException(\"Invalid token\") for an unknown token")
        void rejectsUnknownToken() {
            when(tokenRepository.findByToken("nope")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> verificationTokenService.validateToken("nope"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Invalid token");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("deletes an expired token, returns false and leaves the user disabled")
        void expiredTokenIsBurnedWithoutEnablingUser() {
            Date anHourAgo = new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1));
            VerificationToken expired = tokenExpiringAt("expired-token", anHourAgo);
            when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

            boolean result = verificationTokenService.validateToken("expired-token");

            assertThat(result).isFalse();
            verify(tokenRepository).delete(expired);
            assertThat(user.isEnabled())
                    .as("an expired token must never activate the account")
                    .isFalse();
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("enables the user, saves them and burns the token when it is still valid")
        void validTokenEnablesUser() {
            Date inAnHour = new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));
            VerificationToken valid = tokenExpiringAt("valid-token", inAnHour);
            when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(valid));

            boolean result = verificationTokenService.validateToken("valid-token");

            assertThat(result).isTrue();
            assertThat(user.isEnabled()).isTrue();
            verify(userRepository).save(user);
            verify(tokenRepository).delete(valid);
        }
    }

    @Nested
    @DisplayName("cleanupExpiredTokens")
    class CleanupExpiredTokens {

        @Test
        @DisplayName("looks up tokens expired as of now and deletes them all")
        void deletesAllExpiredTokens() {
            List<VerificationToken> expired = List.of(
                    tokenExpiringAt("expired-1", new Date(System.currentTimeMillis() - 1_000L)),
                    tokenExpiringAt("expired-2", new Date(System.currentTimeMillis() - 2_000L)));
            when(tokenRepository.findAllExpiredTokens(any(Date.class))).thenReturn(expired);

            long before = System.currentTimeMillis();
            verificationTokenService.cleanupExpiredTokens();
            long after = System.currentTimeMillis();

            ArgumentCaptor<Date> cutoff = ArgumentCaptor.forClass(Date.class);
            verify(tokenRepository).findAllExpiredTokens(cutoff.capture());
            assertThat(cutoff.getValue().getTime()).isBetween(before, after);

            verify(tokenRepository).deleteAll(expired);
        }
    }
}
