package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.dto.LoginResponse;
import com.jomariabejo.connectly_api.dto.RegisterUserDto;
import com.jomariabejo.connectly_api.dto.LoginUserDto;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.model.VerificationToken;
import com.jomariabejo.connectly_api.repository.UserRepository;
import com.jomariabejo.connectly_api.repository.VerificationTokenRepository;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.JwtService;
import com.jomariabejo.connectly_api.service.UserService;
import com.jomariabejo.connectly_api.user.event.OnRegistrationCompleteEvent;
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

import java.util.Calendar;
import java.util.Locale;

@RequestMapping("/auth")
@RestController
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

    @PostMapping("/registration")
    public ResponseEntity<User> registerUserAccount(@Valid @RequestBody RegisterUserDto registerUserDto) {
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
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginUserDto loginUserDto) {
        User authenticatedUser = authenticationService.authenticate(loginUserDto);

        String jwtToken = jwtService.generateToken(authenticatedUser);

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(jwtToken);
        loginResponse.setExpiresIn(jwtService.getExpirationTime());

        return ResponseEntity.ok(loginResponse);
    }

    @GetMapping("/registrationConfirm")
    public ResponseEntity<?> confirmRegistration(@RequestParam("token") String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token);
        if (verificationToken == null) {
            return ResponseEntity.badRequest().body("Invalid token");
        }

        User user = verificationToken.getUser();
        Calendar cal = Calendar.getInstance();
        if ((verificationToken.getExpiryDate().getTime() - cal.getTime().getTime()) <= 0) {
            return ResponseEntity.badRequest().body("Expired token");
        }

        user.setEnabled(true);
        userRepository.save(user);

        // Delete the token after successful verification
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
}