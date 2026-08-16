package com.jomariabejo.connectly_api.registration.listener;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.service.VerificationTokenService;
import com.jomariabejo.connectly_api.user.event.OnRegistrationCompleteEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link RegistrationListener}.
 *
 * <p>The listener persists the verification-token row backing
 * {@code GET /auth/registrationConfirm}, reusing the token already stored on
 * {@code User.verificationToken} during signup (it used to mint a second, unrelated UUID that no
 * one was ever emailed). These tests pin the fixed behavior: the user's own token -- not a fresh
 * one -- is handed to {@link VerificationTokenService#createVerificationToken}, and a user without
 * a token produces no token row at all instead of an accidental orphan.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationListener")
class RegistrationListenerTest {

    private static final String APP_URL = "http://localhost:8080";

    @Mock
    private VerificationTokenService tokenService;

    @InjectMocks
    private RegistrationListener listener;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("someone", "hashed", "someone@example.com");
        user.setId(1L);
    }

    @Nested
    @DisplayName("onApplicationEvent")
    class OnApplicationEvent {

        @Test
        @DisplayName("stores the token already assigned to the user at signup")
        void reusesTheUsersExistingToken() {
            user.setVerificationToken("signup-token-123");

            listener.onApplicationEvent(
                    new OnRegistrationCompleteEvent(user, Locale.ENGLISH, APP_URL));

            verify(tokenService).createVerificationToken(user, "signup-token-123");
        }

        @Test
        @DisplayName("skips the token row entirely when the user carries no verification token")
        void skipsUserWithoutToken() {
            user.setVerificationToken(null);

            listener.onApplicationEvent(
                    new OnRegistrationCompleteEvent(user, Locale.ENGLISH, APP_URL));

            verifyNoInteractions(tokenService);
        }
    }
}
