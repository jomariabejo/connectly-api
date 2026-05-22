package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.dto.LoginResponse;
import com.jomariabejo.connectly_api.dto.RegisterUserDto;
import com.jomariabejo.connectly_api.dto.LoginUserDto;
import com.jomariabejo.connectly_api.dto.ForgotPasswordRequest;
import com.jomariabejo.connectly_api.dto.ResendVerificationRequest;
import com.jomariabejo.connectly_api.dto.VerifyOtpRequest;
import com.jomariabejo.connectly_api.dto.ResetPasswordRequest;
import com.jomariabejo.connectly_api.dto.GenericResponse;
import com.jomariabejo.connectly_api.dto.user.UserResponseDto;
import com.jomariabejo.connectly_api.mapper.UserMapper;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.model.VerificationToken;
import com.jomariabejo.connectly_api.repository.UserRepository;
import com.jomariabejo.connectly_api.repository.VerificationTokenRepository;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.JwtService;
import com.jomariabejo.connectly_api.tenant_api.dto.InvitePreviewDto;
import com.jomariabejo.connectly_api.tenant_api.dto.RegisterCustomerRequest;
import com.jomariabejo.connectly_api.tenant_api.dto.RegisterInviteRequest;
import com.jomariabejo.connectly_api.tenant_api.service.ActorRegistrationService;
import com.jomariabejo.connectly_api.tenant_api.service.LoginRedirectService;
import com.jomariabejo.connectly_api.tenant_api.service.TenantInvitationService;
import com.jomariabejo.connectly_api.tenant_api.service.TenantService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RequestMapping("/v1/auth")
@RestController
public class AuthenticationController {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

    private final JwtService jwtService;

    private final VerificationTokenRepository tokenRepository;

    private final AuthenticationService authenticationService;

    private final UserMapper userMapper;
    private final TenantService tenantService;
    private final ActorRegistrationService actorRegistrationService;
    private final TenantInvitationService tenantInvitationService;
    private final LoginRedirectService loginRedirectService;

    @Autowired
    private UserRepository userRepository;

    public AuthenticationController(
            JwtService jwtService,
            VerificationTokenRepository tokenRepository,
            AuthenticationService authenticationService,
            UserMapper userMapper,
            TenantService tenantService,
            ActorRegistrationService actorRegistrationService,
            TenantInvitationService tenantInvitationService,
            LoginRedirectService loginRedirectService
    ) {
        this.jwtService = jwtService;
        this.tokenRepository = tokenRepository;
        this.authenticationService = authenticationService;
        this.userMapper = userMapper;
        this.tenantService = tenantService;
        this.actorRegistrationService = actorRegistrationService;
        this.tenantInvitationService = tenantInvitationService;
        this.loginRedirectService = loginRedirectService;
    }

    @PostMapping("/registration")
    public ResponseEntity<UserResponseDto> registerUserAccount(@Valid @RequestBody RegisterUserDto registerUserDto) {
        log.info("Starting registration");
        User registeredUser = authenticationService.signup(registerUserDto);
        log.info("Registered user: {}", registeredUser);
        log.info("Return Registered user: {}", registeredUser);
        return ResponseEntity.ok(userMapper.toResponseDto(registeredUser));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginUserDto loginUserDto) {
        User authenticatedUser = authenticationService.authenticate(loginUserDto);

        String jwtToken = jwtService.generateToken(authenticatedUser);

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(jwtToken);
        loginResponse.setExpiresIn(jwtService.getExpirationTime());
        var tenants = tenantService.getTenantsForUser(authenticatedUser);
        loginResponse.setTenants(tenants);
        loginResponse.setSuggestedRedirect(loginRedirectService.resolveRedirect(authenticatedUser, tenants));

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/register/customer")
    public ResponseEntity<UserResponseDto> registerCustomer(@Valid @RequestBody RegisterCustomerRequest request) {
        User user = actorRegistrationService.registerCustomer(request);
        return ResponseEntity.ok(userMapper.toResponseDto(user));
    }

    @GetMapping("/invites/{token}")
    public ResponseEntity<InvitePreviewDto> previewInvite(@org.springframework.web.bind.annotation.PathVariable String token) {
        return ResponseEntity.ok(tenantInvitationService.previewInvite(token));
    }

    @PostMapping("/register/invite")
    public ResponseEntity<UserResponseDto> registerViaInvite(@Valid @RequestBody RegisterInviteRequest request) {
        User user = actorRegistrationService.registerViaInvite(request);
        return ResponseEntity.ok(userMapper.toResponseDto(user));
    }

    @GetMapping("/registrationConfirm")
    public ResponseEntity<?> confirmRegistration(@RequestParam("token") String token) {

        Optional<VerificationToken> verificationTokenOptional = tokenRepository.findByToken(token);
        if (!verificationTokenOptional.isPresent()) {
            return ResponseEntity.badRequest().body("Invalid verification token");
        }

        VerificationToken verificationToken = verificationTokenOptional.get();
        
        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        tokenRepository.delete(verificationToken);

        return ResponseEntity.ok("Your account has been successfully activated. You can now login.");
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyAccount(@RequestParam String token) {
        User user = authenticationService.verifyUserByToken(token);
        if (user != null) {
            return ResponseEntity.ok("Email verified successfully. You can now login.");
        } else {
            return ResponseEntity.badRequest().body("Invalid or expired verification token");
        }
    }

    @PostMapping("/verify/otp")
    public ResponseEntity<GenericResponse<String>> verifyByOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            authenticationService.verifyByOtp(request.getEmail(), request.getOtp());
            return ResponseEntity.ok(new GenericResponse<>(
                    "Email verified successfully. You can now sign in.", null));
        } catch (RuntimeException e) {
            log.warn("OTP verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new GenericResponse<>(e.getMessage(), null));
        }
    }

    @PostMapping("/verify/resend")
    public ResponseEntity<GenericResponse<String>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        try {
            authenticationService.resendVerificationEmail(request.getEmail());
            return ResponseEntity.ok(new GenericResponse<>(
                    "If an unverified account exists for this email, a new verification message has been sent.", null));
        } catch (RuntimeException e) {
            log.warn("Verification resend failed: {}", e.getMessage());
            return ResponseEntity.status(429).body(new GenericResponse<>(e.getMessage(), null));
        }
    }

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
