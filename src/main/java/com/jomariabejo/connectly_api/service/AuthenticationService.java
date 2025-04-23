package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.dto.RegisterUserDto;
import com.jomariabejo.connectly_api.dto.LoginUserDto;
import com.jomariabejo.connectly_api.exception.UnauthorizedAccessException;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.token.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final EmailService emailService;

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private final VerificationTokenService verificationTokenService;

    public AuthenticationService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            VerificationTokenService verificationTokenService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.verificationTokenService = verificationTokenService;
    }

    public User signup(RegisterUserDto registerUserDto) {
        // Check if user already exists
        if (userRepository.existsByEmail(registerUserDto.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        User user = new User();
        user.setUsername(registerUserDto.getUsername());
        user.setEmail(registerUserDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerUserDto.getPassword()));
        user.setEnabled(false);

        // Generate verification token
        String token = UUID.randomUUID().toString();

        user.setVerificationToken(token);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 24 * 60 * 60 * 1000);
        user.setExpiryDate(expiryDate);
        user = userRepository.save(user);


        verificationTokenService.createVerificationToken(user,token);
        // Send verification email
        String verificationLink = "http://localhost:8080/auth/verify?token=" + token;
        emailService.sendVerificationEmail(user.getEmail(), verificationLink);



        return user;
    }

    public User authenticate(LoginUserDto input) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getEmail(),
                        input.getPassword()
                )
        );

        User authenticatedUser = (User) authentication.getPrincipal();

        return authenticatedUser;
    }

    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("Authenticated user: " + authentication.getPrincipal());
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            logger.error("Authentication required");
            throw new UnauthorizedAccessException("User not authenticated");
        }
        return (User) authentication.getPrincipal();
    }

    public User verifyUserByToken(String token) {
        Optional<User> userOpt = userRepository.findByVerificationToken(token);
        if (userOpt.isEmpty()) return null;

        User user = userOpt.get();
        user.setEnabled(true);
        user.setVerificationToken(null);
        return userRepository.save(user);
    }


}