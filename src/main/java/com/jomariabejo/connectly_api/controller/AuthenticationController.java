package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.dto.LoginResponse;
import com.jomariabejo.connectly_api.dto.RegisterUserDto;
import com.jomariabejo.connectly_api.dto.LoginUserDto;
import com.jomariabejo.connectly_api.dto.ForgotPasswordRequest;
import com.jomariabejo.connectly_api.dto.ResetPasswordRequest;
import com.jomariabejo.connectly_api.dto.GenericResponse;
import com.jomariabejo.connectly_api.dto.user.UserResponseDto;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.model.VerificationToken;
import com.jomariabejo.connectly_api.repository.UserRepository;
import com.jomariabejo.connectly_api.repository.VerificationTokenRepository;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.JwtService;
import com.jomariabejo.connectly_api.service.UserService;
import com.jomariabejo.connectly_api.user.event.OnRegistrationCompleteEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Locale;
import java.util.Optional;

@RequestMapping("/auth")
@RestController
@Tag(name = "Authentication", description = "Registration, email verification, login and password reset. Every endpoint here is public.")
public class AuthenticationController {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

    private final JwtService jwtService;

    private final VerificationTokenRepository tokenRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final AuthenticationService authenticationService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    public AuthenticationController(JwtService jwtService, VerificationTokenRepository tokenRepository, AuthenticationService authenticationService, ApplicationEventPublisher eventPublisher) {
        this.jwtService = jwtService;
        this.tokenRepository = tokenRepository;
        this.authenticationService = authenticationService;
        this.eventPublisher = eventPublisher;
    }

    @Operation(
            summary = "Register a new account",
            description = "Creates a disabled account and publishes OnRegistrationCompleteEvent, which mails a "
                    + "verification token valid for 24 hours. The account cannot log in until it is verified.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account created; verification email dispatched"),
            @ApiResponse(responseCode = "400", description = "Validation failed on the request body"),
            @ApiResponse(responseCode = "409", description = "Username or email already registered")
    })
    @PostMapping("/registration")
    public ResponseEntity<UserResponseDto> registerUserAccount(@Valid @RequestBody RegisterUserDto registerUserDto) {
        log.info("Starting registration");
        User registeredUser = authenticationService.signup(registerUserDto);
        log.info("Registered user: {}", registeredUser);
        String appUrl = request.getRequestURL().toString().replace(request.getRequestURI(), request.getContextPath());
        log.info("App URL: {}", appUrl);
        eventPublisher.publishEvent(
                new OnRegistrationCompleteEvent(
                        registeredUser,
                        Locale.ENGLISH,
                        appUrl));
        log.info("Return Registered user: {}", registeredUser);
        return ResponseEntity.ok(UserResponseDto.from(registeredUser));
    }

    @Operation(
            summary = "Log in and obtain a JWT",
            description = "Returns a bearer token plus its lifetime in milliseconds (security.jwt.expiration, 1h by default). "
                    + "Send it as `Authorization: Bearer <token>` on every non-/auth endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated; token returned"),
            @ApiResponse(responseCode = "401", description = "Bad credentials, or the account is not yet verified")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginUserDto loginUserDto) {
        User authenticatedUser = authenticationService.authenticate(loginUserDto);

        String jwtToken = jwtService.generateToken(authenticatedUser);

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(jwtToken);
        loginResponse.setExpiresIn(jwtService.getExpirationTime());

        return ResponseEntity.ok(loginResponse);
    }

    @Operation(
            summary = "Confirm registration from the emailed link",
            description = "Enables the account and consumes the verification token. This is the link target used by the "
                    + "registration email; see /auth/verify for the equivalent that validates expiry.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account activated"),
            @ApiResponse(responseCode = "400", description = "Unknown verification token")
    })
    @GetMapping("/registrationConfirm")
    public ResponseEntity<?> confirmRegistration(
            @Parameter(description = "Verification token from the registration email", required = true)
            @RequestParam("token") String token) {

        Optional<VerificationToken> verificationTokenOptional = tokenRepository.findByToken(token);
        if (!verificationTokenOptional.isPresent()) {
            return ResponseEntity.badRequest().body("Invalid verification token");
        }

        VerificationToken verificationToken = verificationTokenOptional.get();

        // This endpoint used to enable the account without looking at the expiry date, so a token
        // from months ago still worked. /auth/verify has always checked; now both do.
        if (verificationToken.getExpiryDate() != null
                && verificationToken.getExpiryDate().before(new Date())) {
            return ResponseEntity.badRequest().body("Verification token has expired");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        user.setVerificationToken(null);
        userRepository.save(user);

        tokenRepository.delete(verificationToken);

        return ResponseEntity.ok("Your account has been successfully activated. You can now login.");
    }

    @Operation(
            summary = "Verify an email address",
            description = "Validates the token and its expiry date, then enables the account.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email verified"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired verification token")
    })
    @GetMapping("/verify")
    public ResponseEntity<String> verifyAccount(
            @Parameter(description = "Verification token from the registration email", required = true)
            @RequestParam String token) {
        User user = authenticationService.verifyUserByToken(token);
        if (user != null) {
            return ResponseEntity.ok("Email verified successfully. You can now login.");
        } else {
            return ResponseEntity.badRequest().body("Invalid or expired verification token");
        }
    }

    @Operation(
            summary = "Request a password reset link by email",
            description = "Always answers with the same neutral message so the endpoint cannot be used to enumerate "
                    + "registered addresses. Rate limited by RateLimitingService.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request accepted (sent only if the account exists)"),
            @ApiResponse(responseCode = "400", description = "Malformed email address"),
            @ApiResponse(responseCode = "429", description = "Too many reset attempts for this address")
    })
    @PostMapping("/forgot-password/email")
    public ResponseEntity<GenericResponse<String>> forgotPasswordEmail(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Password reset requested via email for: {}", request.getEmail());
        try {
            authenticationService.initiatePasswordResetEmail(request.getEmail());
            return ResponseEntity.ok(new GenericResponse<>(
                "If an account exists with this email, you will receive a password reset link shortly.", null));
        } catch (Exception e) {
            log.error("Error processing email password reset", e);
            return ResponseEntity.status(429).body(new GenericResponse<>(
                "Too many requests. Please try again later.", null));
        }
    }

    @Operation(
            summary = "Request a password reset one-time code",
            description = "Same neutral response and rate limiting as the email variant, but mails a short OTP instead "
                    + "of a link. The OTP allows security.password.reset.max-otp-attempts tries before it is burned.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request accepted (sent only if the account exists)"),
            @ApiResponse(responseCode = "400", description = "Malformed email address"),
            @ApiResponse(responseCode = "429", description = "Too many reset attempts for this address")
    })
    @PostMapping("/forgot-password/otp")
    public ResponseEntity<GenericResponse<String>> forgotPasswordOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Password reset requested via OTP for: {}", request.getEmail());
        try {
            authenticationService.initiatePasswordResetOtp(request.getEmail());
            return ResponseEntity.ok(new GenericResponse<>(
                "If an account exists with this email, you will receive a verification code shortly.", null));
        } catch (Exception e) {
            log.error("Error processing OTP password reset", e);
            return ResponseEntity.status(429).body(new GenericResponse<>(
                "Too many requests. Please try again later.", null));
        }
    }

    @Operation(
            summary = "Complete a password reset",
            description = "Supply either the emailed token or the OTP, together with the new password. Tokens expire "
                    + "after security.password.reset.expiry.minutes and are single-use.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "Token invalid, expired, already used, or the new password is too weak"),
            @ApiResponse(responseCode = "500", description = "Unexpected failure while resetting")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<GenericResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Password reset attempt initiated");
        try {
            authenticationService.resetPassword(request.getToken(), request.getOtp(), request.getNewPassword());
            return ResponseEntity.ok(new GenericResponse<>(
                "Your password has been successfully reset. You can now login with your new password.", null));
        } catch (RuntimeException e) {
            log.error("Error resetting password: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new GenericResponse<>(e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error during password reset", e);
            return ResponseEntity.status(500).body(new GenericResponse<>(
                "An error occurred while resetting your password. Please try again later.", null));
        }
    }
}