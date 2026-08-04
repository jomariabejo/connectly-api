package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.dto.LoginUserDto;
import com.jomariabejo.connectly_api.dto.RegisterUserDto;
import com.jomariabejo.connectly_api.exception.EmailAlreadyInUseException;
import com.jomariabejo.connectly_api.exception.InvalidPasswordResetTokenException;
import com.jomariabejo.connectly_api.exception.WeakPasswordException;
import com.jomariabejo.connectly_api.model.Role;
import com.jomariabejo.connectly_api.repository.RoleRepository;
import com.jomariabejo.connectly_api.model.PasswordResetToken;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthenticationService}.
 *
 * <p>The service reads three settings through {@code @Value} fields. Plain Mockito performs no
 * property injection, so {@link ReflectionTestUtils} seeds them in {@link #setUp()} -- without that
 * {@code passwordMinLength} would be 0 and {@code passwordResetRedirectUrl} null, and the tests
 * would fail for reasons unrelated to what they assert.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService")
class AuthenticationServiceTest {

    private static final String RESET_URL = "http://localhost:8080/reset-password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private PasswordResetTokenService passwordResetTokenService;

    @Mock
    private RateLimitingService rateLimitingService;

    @Mock
    private AuditService auditService;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User user;

    private static Role userRole() {
        Role role = new Role();
        role.setId(2L);
        role.setName("USER");
        return role;
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authenticationService, "passwordMinLength", 8);
        ReflectionTestUtils.setField(authenticationService, "passwordResetRedirectUrl", RESET_URL);

        user = new User("someone", "hashed", "someone@example.com");
        user.setId(1L);
    }

    @Nested
    @DisplayName("signup")
    class Signup {

        private RegisterUserDto request() {
            RegisterUserDto dto = new RegisterUserDto();
            dto.setUsername("someone");
            dto.setEmail("someone@example.com");
            dto.setPassword("PlaintextPass1!");
            return dto;
        }

        @Test
        @DisplayName("hashes the password, disables the account and issues a 24-hour token")
        void createsDisabledUserWithToken() {
            when(userRepository.existsByUsername("someone")).thenReturn(false);
            when(userRepository.existsByEmail("someone@example.com")).thenReturn(false);
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole()));
            when(passwordEncoder.encode("PlaintextPass1!")).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = authenticationService.signup(request());

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(saved.capture());
            User persisted = saved.getValue();

            assertThat(persisted.getPassword())
                    .as("the raw password must never be persisted")
                    .isEqualTo("hashed");
            assertThat(persisted.isEnabled())
                    .as("accounts stay disabled until the email is verified")
                    .isFalse();
            assertThat(persisted.getVerificationToken()).isNotBlank();

            long ttlMillis = persisted.getExpiryDate().getTime() - System.currentTimeMillis();
            assertThat(TimeUnit.MILLISECONDS.toHours(ttlMillis)).isBetween(23L, 24L);

            verify(verificationTokenService).createVerificationToken(eq(result), anyString());
            verify(emailService).sendVerificationEmail(eq("someone@example.com"), anyString());
        }

        @Test
        @DisplayName("mails a verification link carrying the generated token")
        void mailsLinkWithToken() {
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole()));
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = authenticationService.signup(request());

            ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
            verify(emailService).sendVerificationEmail(eq("someone@example.com"), link.capture());
            assertThat(link.getValue()).endsWith("/auth/verify?token=" + result.getVerificationToken());
        }

        @Test
        @DisplayName("refuses an email that is already registered")
        void rejectsDuplicateEmail() {
            when(userRepository.existsByUsername("someone")).thenReturn(false);
            when(userRepository.existsByEmail("someone@example.com")).thenReturn(true);

            // EmailAlreadyInUseException maps to 409. This used to be a bare RuntimeException,
            // which the catch-all handler reported as 500.
            assertThatThrownBy(() -> authenticationService.signup(request()))
                    .isInstanceOf(EmailAlreadyInUseException.class)
                    .hasMessageContaining("someone@example.com");

            verify(userRepository, never()).save(any());
            verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("grants the USER role, so the account has authorities from the start")
        void grantsDefaultRole() {
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole()));
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = authenticationService.signup(request());

            // Registration used to leave roles empty, which gave every account zero authorities.
            assertThat(result.getRoles()).extracting(Role::getName).containsExactly("USER");
            assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("rejects a weak password with the same rules the reset flow uses")
        void rejectsWeakPassword() {
            RegisterUserDto weak = request();
            weak.setPassword("admin123");   // no uppercase, no special character

            assertThatThrownBy(() -> authenticationService.signup(weak))
                    .isInstanceOf(WeakPasswordException.class)
                    .hasMessageContaining("at least 8 characters");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("authenticate")
    class Authenticate {

        private LoginUserDto credentials() {
            LoginUserDto dto = new LoginUserDto();
            dto.setEmail("someone@example.com");
            dto.setPassword("plaintext");
            return dto;
        }

        @Test
        @DisplayName("returns the principal from a successful authentication")
        void returnsPrincipal() {
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(user, "plaintext", user.getAuthorities());
            when(authenticationManager.authenticate(any())).thenReturn(authentication);

            assertThat(authenticationService.authenticate(credentials())).isEqualTo(user);
        }

        @Test
        @DisplayName("propagates the failure from the AuthenticationManager")
        void propagatesFailure() {
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authenticationService.authenticate(credentials()))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("verifyUserByToken")
    class VerifyUser {

        @Test
        @DisplayName("enables the account and burns the token")
        void enablesAccount() {
            user.setVerificationToken("the-token");
            when(userRepository.findByVerificationToken("the-token")).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);

            User result = authenticationService.verifyUserByToken("the-token");

            assertThat(result.isEnabled()).isTrue();
            assertThat(result.getVerificationToken())
                    .as("the token is single-use")
                    .isNull();
        }

        @Test
        @DisplayName("returns null for an unknown token")
        void returnsNullForUnknownToken() {
            when(userRepository.findByVerificationToken("nope")).thenReturn(Optional.empty());

            assertThat(authenticationService.verifyUserByToken("nope")).isNull();
        }
    }

    @Nested
    @DisplayName("initiatePasswordResetEmail")
    class ForgotPasswordEmail {

        @Test
        @DisplayName("mails a link built from the configured redirect URL")
        void mailsResetLink() {
            when(rateLimitingService.isRateLimited("someone@example.com", "forgot_password_email")).thenReturn(false);
            when(userRepository.findByEmail("someone@example.com")).thenReturn(Optional.of(user));
            when(passwordResetTokenService.createEmailResetToken(user)).thenReturn("reset-token");

            authenticationService.initiatePasswordResetEmail("someone@example.com");

            verify(rateLimitingService).recordAttempt("someone@example.com", "forgot_password_email");
            verify(emailService).sendPasswordResetEmailWithLink(
                    "someone@example.com", RESET_URL + "?token=reset-token");
            verify(auditService).logPasswordResetInitiated(user, "EMAIL_LINK");
        }

        @Test
        @DisplayName("stays silent for an unknown address so it cannot be used to enumerate accounts")
        void doesNotRevealUnknownEmail() {
            when(rateLimitingService.isRateLimited(anyString(), anyString())).thenReturn(false);
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            authenticationService.initiatePasswordResetEmail("ghost@example.com");

            verify(emailService, never()).sendPasswordResetEmailWithLink(anyString(), anyString());
            verify(auditService).logInvalidResetAttempt("ghost@example.com", "EMAIL_LINK", "Email not found");
        }

        @Test
        @DisplayName("throws once the address is rate limited")
        void throwsWhenRateLimited() {
            when(rateLimitingService.isRateLimited("someone@example.com", "forgot_password_email")).thenReturn(true);

            assertThatThrownBy(() -> authenticationService.initiatePasswordResetEmail("someone@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Too many password reset requests");

            verify(auditService).logRateLimitExceeded("someone@example.com", "forgot_password_email");
            verify(passwordResetTokenService, never()).createEmailResetToken(any());
        }
    }

    @Nested
    @DisplayName("initiatePasswordResetOtp")
    class ForgotPasswordOtp {

        @Test
        @DisplayName("mails the generated one-time code")
        void mailsOtp() {
            when(rateLimitingService.isRateLimited("someone@example.com", "forgot_password_otp")).thenReturn(false);
            when(userRepository.findByEmail("someone@example.com")).thenReturn(Optional.of(user));
            when(passwordResetTokenService.createOtpResetToken(user)).thenReturn("123456");

            authenticationService.initiatePasswordResetOtp("someone@example.com");

            verify(rateLimitingService).recordAttempt("someone@example.com", "forgot_password_otp");
            verify(emailService).sendPasswordResetOtp("someone@example.com", "123456");
            verify(auditService).logPasswordResetInitiated(user, "OTP");
        }

        @Test
        @DisplayName("throws once the address is rate limited")
        void throwsWhenRateLimited() {
            when(rateLimitingService.isRateLimited("someone@example.com", "forgot_password_otp")).thenReturn(true);

            assertThatThrownBy(() -> authenticationService.initiatePasswordResetOtp("someone@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Too many password reset requests");
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        private static final String STRONG = "NewSecurePassword123!";

        private PasswordResetToken tokenFor(User owner) {
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(owner);
            return token;
        }

        @Test
        @DisplayName("re-hashes the password and burns the token in the email-link flow")
        void resetsViaToken() {
            PasswordResetToken resetToken = tokenFor(user);
            when(passwordResetTokenService.validateToken("reset-token")).thenReturn(resetToken);
            when(passwordEncoder.encode(STRONG)).thenReturn("new-hash");

            authenticationService.resetPassword("reset-token", null, STRONG);

            assertThat(user.getPassword()).isEqualTo("new-hash");
            verify(userRepository).save(user);
            verify(passwordResetTokenService).markTokenAsUsed(resetToken);
            verify(auditService).logPasswordReset(eq(user), eq(true), eq("EMAIL_LINK"), anyString());
        }

        @Test
        @DisplayName("re-hashes the password and burns the code in the OTP flow")
        void resetsViaOtp() {
            PasswordResetToken resetToken = tokenFor(user);
            when(passwordResetTokenService.validateOtp("123456")).thenReturn(resetToken);
            when(passwordEncoder.encode(STRONG)).thenReturn("new-hash");

            authenticationService.resetPassword(null, "123456", STRONG);

            assertThat(user.getPassword()).isEqualTo("new-hash");
            verify(passwordResetTokenService).markTokenAsUsed(resetToken);
            verify(auditService).logPasswordReset(eq(user), eq(true), eq("OTP"), anyString());
        }

        @Test
        @DisplayName("requires either a token or an OTP")
        void requiresOneCredential() {
            assertThatThrownBy(() -> authenticationService.resetPassword(null, null, STRONG))
                    .isInstanceOf(InvalidPasswordResetTokenException.class)
                    .hasMessage("Either token or OTP is required");

            assertThatThrownBy(() -> authenticationService.resetPassword("  ", "  ", STRONG))
                    .isInstanceOf(InvalidPasswordResetTokenException.class);
        }

        @Test
        @DisplayName("rejects a password that fails the strength rules before touching the token")
        void rejectsWeakPassword() {
            // Too short, no uppercase, no digit and no special character respectively.
            for (String weak : new String[]{"Ab1!", "newsecurepassword123!", "NewSecurePassword!", "NewSecurePassword123"}) {
                assertThatThrownBy(() -> authenticationService.resetPassword("reset-token", null, weak))
                        .isInstanceOf(WeakPasswordException.class)
                        .hasMessageContaining("Password must be at least 8 characters long");
            }

            verify(passwordResetTokenService, never()).validateToken(anyString());
            verify(userRepository, never()).save(any());
        }
    }
}
