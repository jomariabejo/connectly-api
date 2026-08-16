package com.jomariabejo.connectly_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jomariabejo.connectly_api.dto.LoginUserDto;
import com.jomariabejo.connectly_api.dto.RegisterUserDto;
import com.jomariabejo.connectly_api.exception.AccountDeletionScheduledException;
import com.jomariabejo.connectly_api.exception.InvalidCredentialsException;
import com.jomariabejo.connectly_api.exception.UserAlreadyExistsException;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.UserRepository;
import com.jomariabejo.connectly_api.repository.VerificationTokenRepository;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.JwtService;
import com.jomariabejo.connectly_api.service.UserService;
import com.jomariabejo.connectly_api.user.event.OnRegistrationCompleteEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller slice tests for {@link AuthenticationController}: routing, validation, status codes
 * and JSON shape across the registration, login, verification and password-reset flows.
 *
 * <p>See {@link ControllerSliceTest} for what the slice does and does not cover.
 */
@ControllerSliceTest(AuthenticationController.class)
@RecordApplicationEvents
@DisplayName("AuthenticationController")
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationEvents applicationEvents;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private VerificationTokenRepository verificationTokenRepository;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    private User user() {
        User user = new User("someone", "hashed", "someone@example.com");
        user.setId(1L);
        return user;
    }

    @Test
    @DisplayName("POST /auth/registration returns the created account and fires the registration event")
    void registersAccount() throws Exception {
        RegisterUserDto request = new RegisterUserDto();
        request.setUsername("someone");
        request.setEmail("someone@example.com");
        request.setPassword("plaintext");

        when(authenticationService.signup(any(RegisterUserDto.class))).thenReturn(user());

        mockMvc.perform(post("/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("someone@example.com"));

        // Asserted against the container's real publisher rather than a mock: the controller is
        // handed the ApplicationContext itself for ApplicationEventPublisher, so a @MockitoBean
        // of that type would never be the instance it calls.
        assertThat(applicationEvents.stream(OnRegistrationCompleteEvent.class))
                .singleElement()
                .satisfies(event -> assertThat(event.getUser().getEmail()).isEqualTo("someone@example.com"));
    }

    @Test
    @DisplayName("POST /auth/registration rejects a body that fails @Valid with 400")
    void rejectsInvalidRegistration() throws Exception {
        // Blank username and a malformed email both violate the constraints on RegisterUserDto.
        String invalid = objectMapper.writeValueAsString(Map.of(
                "username", "",
                "email", "not-an-email",
                "password", "plaintext"));

        mockMvc.perform(post("/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).signup(any());
    }

    @Test
    @DisplayName("POST /auth/login returns the token and its lifetime")
    void logsIn() throws Exception {
        LoginUserDto request = new LoginUserDto();
        request.setEmail("someone@example.com");
        request.setPassword("plaintext");

        when(authenticationService.authenticate(any(LoginUserDto.class))).thenReturn(user());
        when(jwtService.generateToken(any(User.class))).thenReturn("a.jwt.token");
        when(jwtService.getExpirationTime()).thenReturn(3_600_000L);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("a.jwt.token"))
                .andExpect(jsonPath("$.expiresIn").value(3_600_000L));
    }

    @Test
    @DisplayName("GET /auth/verify confirms a valid token")
    void verifiesAccount() throws Exception {
        when(authenticationService.verifyUserByToken("good-token")).thenReturn(user());

        mockMvc.perform(get("/auth/verify").param("token", "good-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("Email verified successfully. You can now login."));
    }

    @Test
    @DisplayName("GET /auth/verify answers 400 for an unknown token")
    void rejectsUnknownVerificationToken() throws Exception {
        when(authenticationService.verifyUserByToken("bad-token")).thenReturn(null);

        mockMvc.perform(get("/auth/verify").param("token", "bad-token"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid or expired verification token"));
    }

    @Test
    @DisplayName("GET /auth/registrationConfirm answers 400 when the token is unknown")
    void rejectsUnknownConfirmationToken() throws Exception {
        when(verificationTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        mockMvc.perform(get("/auth/registrationConfirm").param("token", "bad-token"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid verification token"));
    }

    @Test
    @DisplayName("POST /auth/forgot-password/email answers with a neutral message")
    void requestsPasswordResetByEmail() throws Exception {
        mockMvc.perform(post("/auth/forgot-password/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"someone@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("If an account exists with this email, you will receive a password reset link shortly."));

        verify(authenticationService).initiatePasswordResetEmail("someone@example.com");
    }

    @Test
    @DisplayName("POST /auth/forgot-password/email answers 429 once the address is rate limited")
    void reportsRateLimit() throws Exception {
        // The controller catches every exception from the service and reports 429.
        org.mockito.Mockito.doThrow(new RuntimeException("Too many password reset requests"))
                .when(authenticationService).initiatePasswordResetEmail(anyString());

        mockMvc.perform(post("/auth/forgot-password/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"someone@example.com\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many requests. Please try again later."));
    }

    @Test
    @DisplayName("POST /auth/forgot-password/otp answers with a neutral message")
    void requestsPasswordResetByOtp() throws Exception {
        mockMvc.perform(post("/auth/forgot-password/otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"someone@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("If an account exists with this email, you will receive a verification code shortly."));

        verify(authenticationService).initiatePasswordResetOtp("someone@example.com");
    }

    @Test
    @DisplayName("POST /auth/reset-password confirms a successful reset")
    void resetsPassword() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"reset-token\",\"otp\":null,\"newPassword\":\"NewSecurePassword123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Your password has been successfully reset. You can now login with your new password."));

        verify(authenticationService).resetPassword("reset-token", null, "NewSecurePassword123!");
    }

    @Test
    @DisplayName("POST /auth/reset-password surfaces the service's reason with 400")
    void reportsResetFailure() throws Exception {
        org.mockito.Mockito.doThrow(new RuntimeException("Token has expired"))
                .when(authenticationService).resetPassword(anyString(), any(), anyString());

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"stale\",\"otp\":null,\"newPassword\":\"NewSecurePassword123!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token has expired"));
    }

    @Test
    @DisplayName("POST /auth/login rejects a blank email with 400 before the service runs")
    void rejectsLoginWithBlankEmail() throws Exception {
        String invalid = objectMapper.writeValueAsString(Map.of(
                "email", "",
                "password", "plaintext"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).authenticate(any());
    }

    @Test
    @DisplayName("POST /auth/login rejects a malformed email with 400 before the service runs")
    void rejectsLoginWithMalformedEmail() throws Exception {
        String invalid = objectMapper.writeValueAsString(Map.of(
                "email", "not-an-email",
                "password", "plaintext"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).authenticate(any());
    }

    @Test
    @DisplayName("POST /auth/forgot-password/email answers 400 Validation failed for a malformed address")
    void rejectsMalformedForgotPasswordEmail() throws Exception {
        // Bean validation on ForgotPasswordRequest fails before the handler body runs, so this is
        // a 400 from the advice -- never the controller's catch-all 429, which only sees
        // exceptions thrown by the service (see reportsRateLimit above).
        mockMvc.perform(post("/auth/forgot-password/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.message").value("email: Malformed email address"));

        verify(authenticationService, never()).initiatePasswordResetEmail(anyString());
    }

    @Test
    @DisplayName("POST /auth/registration answers 409 with an ErrorResponse when the account already exists")
    void reportsDuplicateRegistration() throws Exception {
        RegisterUserDto request = new RegisterUserDto();
        request.setUsername("someone");
        request.setEmail("someone@example.com");
        request.setPassword("plaintext");

        when(authenticationService.signup(any(RegisterUserDto.class)))
                .thenThrow(new UserAlreadyExistsException("Username already exists: someone"));

        mockMvc.perform(post("/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("User already exists"))
                .andExpect(jsonPath("$.message").value("Username already exists: someone"))
                .andExpect(jsonPath("$.timestamp").isNumber());

        // signup threw before the controller reached the event publisher.
        assertThat(applicationEvents.stream(OnRegistrationCompleteEvent.class)).isEmpty();
    }

    @Test
    @DisplayName("POST /auth/login answers 401 with an ErrorResponse for invalid credentials")
    void reportsInvalidCredentials() throws Exception {
        LoginUserDto request = new LoginUserDto();
        request.setEmail("someone@example.com");
        request.setPassword("wrong-password");

        when(authenticationService.authenticate(any(LoginUserDto.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Invalid credentials"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /auth/login answers 410 GONE when the account is scheduled for deletion")
    void reportsScheduledDeletion() throws Exception {
        LoginUserDto request = new LoginUserDto();
        request.setEmail("someone@example.com");
        request.setPassword("plaintext");

        LocalDateTime deletedAt = LocalDateTime.of(2026, Month.JANUARY, 1, 12, 0);
        when(authenticationService.authenticate(any(LoginUserDto.class)))
                .thenThrow(new AccountDeletionScheduledException(
                        "This account is scheduled for deletion", deletedAt, deletedAt.plusDays(30)));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410))
                .andExpect(jsonPath("$.error").value("Account scheduled for deletion"))
                .andExpect(jsonPath("$.message").value("This account is scheduled for deletion"));
    }
}
