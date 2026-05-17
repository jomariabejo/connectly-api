package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.common.ApiUrlBuilder;
import com.jomariabejo.connectly_api.dto.RegisterUserDto;
import com.jomariabejo.connectly_api.exception.EmailAlreadyInUseException;
import com.jomariabejo.connectly_api.exception.UserAlreadyExistsException;
import com.jomariabejo.connectly_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final EmailService emailService = mock(EmailService.class);
    private final VerificationTokenService verificationTokenService = mock(VerificationTokenService.class);
    private final PasswordResetTokenService passwordResetTokenService = mock(PasswordResetTokenService.class);
    private final RateLimitingService rateLimitingService = mock(RateLimitingService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final ApiUrlBuilder apiUrlBuilder = new ApiUrlBuilder("http://localhost:8080/api");

    private final AuthenticationService authenticationService = new AuthenticationService(
            userRepository,
            authenticationManager,
            passwordEncoder,
            emailService,
            verificationTokenService,
            passwordResetTokenService,
            rateLimitingService,
            auditService,
            apiUrlBuilder
    );

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

    private RegisterUserDto registrationRequest() {
        RegisterUserDto request = new RegisterUserDto();
        request.setUsername("connectly_usera");
        request.setEmail("test@example.com");
        request.setPassword("Password1!");
        return request;
    }
}
