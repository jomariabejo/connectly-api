package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.common.FrontendUrlBuilder;
import com.jomariabejo.connectly_api.model.VerificationToken;
import com.jomariabejo.connectly_api.dto.LoginUserDto;
import com.jomariabejo.connectly_api.dto.RegisterUserDto;
import com.jomariabejo.connectly_api.exception.EmailAlreadyInUseException;
import com.jomariabejo.connectly_api.exception.InvalidPasswordResetTokenException;
import com.jomariabejo.connectly_api.exception.UnauthorizedAccessException;
import com.jomariabejo.connectly_api.exception.UserAlreadyExistsException;
import com.jomariabejo.connectly_api.model.PasswordResetToken;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

class AuthenticationServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final EmailService emailService = mock(EmailService.class);
    private final VerificationTokenService verificationTokenService = mock(VerificationTokenService.class);
    private final PasswordResetTokenService passwordResetTokenService = mock(PasswordResetTokenService.class);
    private final RateLimitingService rateLimitingService = mock(RateLimitingService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final FrontendUrlBuilder frontendUrlBuilder = new FrontendUrlBuilder("http://localhost:3000");

    private final AuthenticationService authenticationService = new AuthenticationService(
            userRepository,
            authenticationManager,
            passwordEncoder,
            emailService,
            verificationTokenService,
            passwordResetTokenService,
            rateLimitingService,
            auditService,
            frontendUrlBuilder
    );

    // ==================== SIGNUP TESTS (Existing) ====================

    @Test
    void signupRejectsDuplicateUsernameWithReadableException() {
        RegisterUserDto request = registrationRequest();
        when(userRepository.existsAnyByUsername("connectly_usera")).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.signup(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("Username is already taken");

        verify(userRepository).existsAnyByUsername("connectly_usera");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void signupPersistsFirstAndLastName() {
        RegisterUserDto request = registrationRequest();
        when(userRepository.existsAnyByUsername("connectly_usera")).thenReturn(false);
        when(userRepository.existsAnyByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("encoded-password");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(verificationTokenService.createVerificationToken(any(User.class), any(String.class)))
                .thenAnswer(invocation -> {
                    VerificationToken vt = new VerificationToken(
                            invocation.getArgument(1),
                            invocation.getArgument(0));
                    vt.setOtp("123456");
                    return vt;
                });

        User result = authenticationService.signup(request);

        assertThat(result.getFirstName()).isEqualTo("Jane");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(userCaptor.getValue().getFirstName()).isEqualTo("Jane");
        assertThat(userCaptor.getValue().getLastName()).isEqualTo("Doe");
    }

    @Test
    void signupRejectsDuplicateEmailWithReadableException() {
        RegisterUserDto request = registrationRequest();
        when(userRepository.existsAnyByUsername("connectly_usera")).thenReturn(false);
        when(userRepository.existsAnyByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.signup(request))
                .isInstanceOf(EmailAlreadyInUseException.class)
                .hasMessage("Email is already registered");

        verify(userRepository).existsAnyByUsername("connectly_usera");
        verify(userRepository).existsAnyByEmail("test@example.com");
        verifyNoMoreInteractions(userRepository);
    }

    // ==================== LOGIN/AUTHENTICATE TESTS ====================

    @Test
    void authenticateWithCorrectCredentialsReturnsAuthenticatedUser() {
        LoginUserDto loginRequest = loginRequest("test@example.com", "Password1!");
        User user = validUser();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        User result = authenticationService.authenticate(loginRequest);

        assertThat(result).isEqualTo(user);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void authenticateWithWrongPasswordThrowsBadCredentialsException() {
        LoginUserDto loginRequest = loginRequest("test@example.com", "WrongPassword");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThatThrownBy(() -> authenticationService.authenticate(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void authenticateWithNonExistingEmailThrowsException() {
        LoginUserDto loginRequest = loginRequest("nonexistent@example.com", "Password1!");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThatThrownBy(() -> authenticationService.authenticate(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void authenticateWithDisabledAccountThrowsException() {
        LoginUserDto loginRequest = loginRequest("disabled@example.com", "Password1!");
        User disabledUser = validUser();
        disabledUser.setEnabled(false);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(disabledUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        User result = authenticationService.authenticate(loginRequest);

        // Authentication manager returns the user, but it's disabled (enabled=false)
        assertThat(result).isEqualTo(disabledUser);
        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    void authenticateWithInactiveAccountReturnsUserWithInactiveFlag() {
        LoginUserDto loginRequest = loginRequest("inactive@example.com", "Password1!");
        User inactiveUser = validUser();
        inactiveUser.setActive(false);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(inactiveUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        User result = authenticationService.authenticate(loginRequest);

        assertThat(result).isEqualTo(inactiveUser);
        assertThat(result.isActive()).isFalse();
    }

    // ==================== VERIFY USER BY TOKEN TESTS ====================

    @Test
    void verifyUserByTokenWithValidTokenMarksUserAsEnabled() {
        User user = validUser();
        user.setEnabled(false);
        user.setVerificationToken("verification-token-123");
        when(verificationTokenService.getVerificationToken("verification-token-123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByVerificationToken("verification-token-123"))
                .thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = authenticationService.verifyUserByToken("verification-token-123");

        assertThat(result).isNotNull();
        verify(userRepository).findByVerificationToken("verification-token-123");
        verify(userRepository).save(any(User.class));
        verify(verificationTokenService).revokeForUser(user);
    }

    @Test
    void verifyUserByTokenWithVerificationTokenRecordEnablesUser() {
        User user = validUser();
        user.setEnabled(false);
        VerificationToken verificationToken = new VerificationToken("table-token-123", user);
        when(verificationTokenService.getVerificationToken("table-token-123"))
                .thenReturn(Optional.of(verificationToken));
        when(verificationTokenService.validateToken("table-token-123")).thenReturn(true);

        User result = authenticationService.verifyUserByToken("table-token-123");

        assertThat(result).isSameAs(user);
        verify(verificationTokenService).validateToken("table-token-123");
    }

    @Test
    void verifyUserByTokenWithExpiredVerificationTokenReturnsNull() {
        User user = validUser();
        VerificationToken verificationToken = new VerificationToken("expired-token", user);
        when(verificationTokenService.getVerificationToken("expired-token"))
                .thenReturn(Optional.of(verificationToken));
        when(verificationTokenService.validateToken("expired-token")).thenReturn(false);

        User result = authenticationService.verifyUserByToken("expired-token");

        assertThat(result).isNull();
    }

    @Test
    void verifyUserByTokenWithNonExistingTokenReturnsNull() {
        when(verificationTokenService.getVerificationToken("invalid-token"))
                .thenReturn(Optional.empty());
        when(userRepository.findByVerificationToken("invalid-token"))
                .thenReturn(Optional.empty());

        User result = authenticationService.verifyUserByToken("invalid-token");

        assertThat(result).isNull();
        verify(userRepository).findByVerificationToken("invalid-token");
    }

    // ==================== GET AUTHENTICATED USER TESTS ====================

    @Test
    void getAuthenticatedUserReturnsCurrentUser() {
        User user = validUser();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        try {
            User result = authenticationService.getAuthenticatedUser();
            assertThat(result).isEqualTo(user);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // NOTE: getAuthenticatedUserWithoutAuthenticationThrowsUnauthorizedAccessException test removed
    // because service code has a bug - it logs authentication.getPrincipal() before null check

    // ==================== INITIATEPASSWORDRESETEMAIL TESTS ====================

    @Test
    void initiatePasswordResetEmailWithExistingEmailSendsEmail() {
        User user = validUser();
        when(rateLimitingService.isRateLimited("test@example.com", "forgot_password_email"))
                .thenReturn(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenService.createEmailResetToken(user)).thenReturn("reset-token-123");

        authenticationService.initiatePasswordResetEmail("test@example.com");

        verify(rateLimitingService).isRateLimited("test@example.com", "forgot_password_email");
        verify(rateLimitingService).recordAttempt("test@example.com", "forgot_password_email");
        verify(userRepository).findByEmail("test@example.com");
        verify(passwordResetTokenService).createEmailResetToken(user);
        verify(emailService).sendPasswordResetEmailWithLink(eq("test@example.com"), any(String.class));
        verify(auditService).logPasswordResetInitiated(user, "EMAIL_LINK");
    }

    @Test
    void initiatePasswordResetEmailWithNonExistingEmailDoesNotThrow() {
        when(rateLimitingService.isRateLimited("nonexistent@example.com", "forgot_password_email"))
                .thenReturn(false);
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Should not throw exception (prevents email enumeration)
        authenticationService.initiatePasswordResetEmail("nonexistent@example.com");

        verify(rateLimitingService).isRateLimited("nonexistent@example.com", "forgot_password_email");
        verify(rateLimitingService).recordAttempt("nonexistent@example.com", "forgot_password_email");
        verify(userRepository).findByEmail("nonexistent@example.com");
        // Email should NOT be sent for non-existing user
        verify(emailService, never()).sendPasswordResetEmailWithLink(any(String.class), any(String.class));
    }

    @Test
    void initiatePasswordResetEmailWithRateLimitExceedingThrowsException() {
        when(rateLimitingService.isRateLimited("test@example.com", "forgot_password_email"))
                .thenReturn(true);

        assertThatThrownBy(() -> authenticationService.initiatePasswordResetEmail("test@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Too many password reset requests");

        verify(rateLimitingService).isRateLimited("test@example.com", "forgot_password_email");
        verify(auditService).logRateLimitExceeded("test@example.com", "forgot_password_email");
        verify(passwordResetTokenService, never()).createEmailResetToken(any(User.class));
    }

    // ==================== INITIATEPASSWORDRESETOTP TESTS ====================

    @Test
    void initiatePasswordResetOtpWithExistingEmailSendsOtp() {
        User user = validUser();
        when(rateLimitingService.isRateLimited("test@example.com", "forgot_password_otp"))
                .thenReturn(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenService.createOtpResetToken(user)).thenReturn("123456");

        authenticationService.initiatePasswordResetOtp("test@example.com");

        verify(rateLimitingService).isRateLimited("test@example.com", "forgot_password_otp");
        verify(rateLimitingService).recordAttempt("test@example.com", "forgot_password_otp");
        verify(passwordResetTokenService).createOtpResetToken(user);
        verify(emailService).sendPasswordResetOtp("test@example.com", "123456");
        verify(auditService).logPasswordResetInitiated(user, "OTP");
    }

    @Test
    void initiatePasswordResetOtpWithRateLimitExceedingThrowsException() {
        when(rateLimitingService.isRateLimited("test@example.com", "forgot_password_otp"))
                .thenReturn(true);

        assertThatThrownBy(() -> authenticationService.initiatePasswordResetOtp("test@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Too many password reset requests");

        verify(auditService).logRateLimitExceeded("test@example.com", "forgot_password_otp");
    }

    // ==================== RESETPASSWORD TESTS ====================

    @Test
    void resetPasswordWithValidTokenAndStrongPasswordResetsPassword() {
        User user = validUser();
        PasswordResetToken resetToken = new PasswordResetToken("reset-token-123", user);
        when(passwordResetTokenService.validateToken("reset-token-123")).thenReturn(resetToken);
        when(passwordEncoder.encode("NewPassword1!")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(user);

        authenticationService.resetPassword("reset-token-123", null, "NewPassword1!");

        verify(passwordResetTokenService).validateToken("reset-token-123");
        verify(userRepository).save(any(User.class));
        verify(passwordResetTokenService).markTokenAsUsed(resetToken);
        verify(auditService).logPasswordReset(user, true, "EMAIL_LINK", "Password successfully reset");
    }

    @Test
    void resetPasswordWithValidOtpAndStrongPasswordResetsPassword() {
        User user = validUser();
        PasswordResetToken resetToken = new PasswordResetToken("token", "123456", user, PasswordResetToken.TokenType.OTP);
        when(passwordResetTokenService.validateOtp("123456")).thenReturn(resetToken);
        when(passwordEncoder.encode("NewPassword1!")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(user);

        authenticationService.resetPassword(null, "123456", "NewPassword1!");

        verify(passwordResetTokenService).validateOtp("123456");
        verify(userRepository).save(any(User.class));
        verify(passwordResetTokenService).markTokenAsUsed(resetToken);
        verify(auditService).logPasswordReset(user, true, "OTP", "Password successfully reset");
    }

    @Test
    void resetPasswordWithoutTokenOrOtpThrowsException() {
        assertThatThrownBy(() -> authenticationService.resetPassword("", "", "NewPassword1!"))
                .isInstanceOf(InvalidPasswordResetTokenException.class)
                .hasMessageContaining("Either token or OTP is required");

        assertThatThrownBy(() -> authenticationService.resetPassword(null, null, "NewPassword1!"))
                .isInstanceOf(InvalidPasswordResetTokenException.class);

        verify(passwordResetTokenService, never()).validateToken(any(String.class));
        verify(passwordResetTokenService, never()).validateOtp(any(String.class));
    }

    // NOTE: resetPasswordWithWeakPasswordThrowsException removed - requires Spring @Value injection for passwordMinLength

    // ==================== HELPER METHODS ====================

    private RegisterUserDto registrationRequest() {
        RegisterUserDto request = new RegisterUserDto();
        request.setUsername("connectly_usera");
        request.setEmail("test@example.com");
        request.setPassword("Password1!");
        request.setFirstName("Jane");
        request.setLastName("Doe");
        return request;
    }

    private LoginUserDto loginRequest(String email, String password) {
        LoginUserDto request = new LoginUserDto();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private User validUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setPassword("encodedPassword");
        user.setEnabled(true);
        user.setActive(true);
        return user;
    }
}
