package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.common.FrontendUrlBuilder;
import com.jomariabejo.connectly_api.exception.InvalidVerificationException;
import com.jomariabejo.connectly_api.model.VerificationToken;
import com.jomariabejo.connectly_api.dto.RegisterUserDto;
import com.jomariabejo.connectly_api.dto.LoginUserDto;
import com.jomariabejo.connectly_api.exception.EmailAlreadyInUseException;
import com.jomariabejo.connectly_api.exception.UnauthorizedAccessException;
import com.jomariabejo.connectly_api.exception.InvalidPasswordResetTokenException;
import com.jomariabejo.connectly_api.exception.PasswordResetTokenExpiredException;
import com.jomariabejo.connectly_api.exception.UserAlreadyExistsException;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.model.PasswordResetToken;
import com.jomariabejo.connectly_api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final PasswordResetTokenService passwordResetTokenService;
    private final RateLimitingService rateLimitingService;
    private final AuditService auditService;
    private final FrontendUrlBuilder frontendUrlBuilder;

    @Value("${security.password.validation.min-length:8}")
    private int passwordMinLength;

    public AuthenticationService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            VerificationTokenService verificationTokenService,
            PasswordResetTokenService passwordResetTokenService,
            RateLimitingService rateLimitingService,
            AuditService auditService,
            FrontendUrlBuilder frontendUrlBuilder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.verificationTokenService = verificationTokenService;
        this.passwordResetTokenService = passwordResetTokenService;
        this.rateLimitingService = rateLimitingService;
        this.auditService = auditService;
        this.frontendUrlBuilder = frontendUrlBuilder;
    }

    public User signup(RegisterUserDto registerUserDto) {
        // Check if user already exists
        if (userRepository.existsAnyByUsername(registerUserDto.getUsername())) {
            throw new UserAlreadyExistsException("Username is already taken");
        }

        if (userRepository.existsAnyByEmail(registerUserDto.getEmail())) {
            throw new EmailAlreadyInUseException("Email is already registered");
        }

        User user = new User();
        user.setUsername(registerUserDto.getUsername());
        user.setEmail(registerUserDto.getEmail().trim().toLowerCase());
        user.setFirstName(registerUserDto.getFirstName());
        user.setLastName(registerUserDto.getLastName());
        user.setPassword(passwordEncoder.encode(registerUserDto.getPassword()));
        user.setEnabled(false);

        // Generate verification token
        String token = UUID.randomUUID().toString();

        user.setVerificationToken(token);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 24 * 60 * 60 * 1000);
        user.setExpiryDate(expiryDate);
        user = userRepository.save(user);


        sendVerificationEmailForUser(user, token);
        return user;
    }

    public void sendVerificationEmailForUser(User user, String token) {
        VerificationToken verificationToken = verificationTokenService.createVerificationToken(user, token);
        String verificationLink = frontendUrlBuilder.verifyEmailUrl(token);
        emailService.sendVerificationEmail(
                user.getEmail(),
                verificationLink,
                verificationToken.getOtp(),
                frontendUrlBuilder.checkEmailUrl()
        );
    }

    @Transactional
    public void verifyByOtp(String email, String otp) {
        String normalizedEmail = email.trim().toLowerCase();
        if (rateLimitingService.isRateLimited(normalizedEmail, "verify_otp")) {
            throw new InvalidVerificationException("Too many verification attempts. Please try again later.");
        }
        rateLimitingService.recordAttempt(normalizedEmail, "verify_otp");
        verificationTokenService.verifyByOtp(normalizedEmail, otp);
    }

    public void resendVerificationEmail(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        if (rateLimitingService.isRateLimited(normalizedEmail, "verify_resend")) {
            throw new InvalidVerificationException("Too many resend requests. Please try again later.");
        }
        rateLimitingService.recordAttempt(normalizedEmail, "verify_resend");

        Optional<User> userOpt = userRepository.findByEmailNormalized(normalizedEmail);
        if (userOpt.isEmpty() || userOpt.get().isEnabled()) {
            return;
        }

        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setExpiryDate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L));
        userRepository.save(user);
        sendVerificationEmailForUser(user, token);
        logger.info("Verification email resent to: {}", normalizedEmail);
    }

    public User authenticate(LoginUserDto input) {
        String email = input.getEmail() == null ? "" : input.getEmail().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
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

    @Transactional
    public User verifyUserByToken(String token) {
        Optional<VerificationToken> tokenOpt = verificationTokenService.getVerificationToken(token);
        if (tokenOpt.isPresent()) {
            try {
                if (!verificationTokenService.validateToken(token)) {
                    return null;
                }
                return tokenOpt.get().getUser();
            } catch (InvalidVerificationException e) {
                logger.warn("Token verification failed: {}", e.getMessage());
                return null;
            }
        }

        Optional<User> userOpt = userRepository.findByVerificationToken(token);
        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();
        if (user.getExpiryDate() != null && user.getExpiryDate().before(new Date())) {
            user.setVerificationToken(null);
            userRepository.save(user);
            return null;
        }

        user.setEnabled(true);
        user.setVerificationToken(null);
        userRepository.save(user);
        verificationTokenService.revokeForUser(user);
        return user;
    }

    /**
     * Initiates password reset flow via email link
     */
    public void initiatePasswordResetEmail(String email) {
        logger.debug("Initiating password reset email flow for: {}", email);

        // Check rate limiting
        if (rateLimitingService.isRateLimited(email, "forgot_password_email")) {
            auditService.logRateLimitExceeded(email, "forgot_password_email");
            throw new RuntimeException("Too many password reset requests. Please try again later.");
        }

        // Record the attempt
        rateLimitingService.recordAttempt(email, "forgot_password_email");

        // Check if user exists
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            auditService.logPasswordResetInitiated(user, "EMAIL_LINK");

            // Create reset token
            String resetToken = passwordResetTokenService.createEmailResetToken(user);

            // Send email with reset link
            String resetLink = frontendUrlBuilder.resetPasswordUrl(resetToken);
            emailService.sendPasswordResetEmailWithLink(email, resetLink);

            logger.info("Password reset email sent to: {}", email);
        } else {
            // Log invalid attempt but don't reveal if email exists
            auditService.logInvalidResetAttempt(email, "EMAIL_LINK", "Email not found");
            logger.debug("Password reset requested for non-existent email: {}", email);
        }

        // Always return success to prevent email enumeration
    }

    /**
     * Initiates password reset flow via OTP
     */
    public void initiatePasswordResetOtp(String email) {
        logger.debug("Initiating password reset OTP flow for: {}", email);

        // Check rate limiting
        if (rateLimitingService.isRateLimited(email, "forgot_password_otp")) {
            auditService.logRateLimitExceeded(email, "forgot_password_otp");
            throw new RuntimeException("Too many password reset requests. Please try again later.");
        }

        // Record the attempt
        rateLimitingService.recordAttempt(email, "forgot_password_otp");

        // Check if user exists
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            auditService.logPasswordResetInitiated(user, "OTP");

            // Create reset OTP
            String otp = passwordResetTokenService.createOtpResetToken(user);

            // Send email with OTP
            emailService.sendPasswordResetOtp(email, otp);

            logger.info("Password reset OTP sent to: {}", email);
        } else {
            // Log invalid attempt but don't reveal if email exists
            auditService.logInvalidResetAttempt(email, "OTP", "Email not found");
            logger.debug("Password reset requested for non-existent email: {}", email);
        }

        // Always return success to prevent email enumeration
    }

    /**
     * Resets user password using either token or OTP
     */
    public void resetPassword(String token, String otp, String newPassword) {
        logger.debug("Processing password reset request");

        // Validate password strength
        if (!isPasswordStrong(newPassword)) {
            throw new RuntimeException(
                "Password must be at least " + passwordMinLength + 
                " characters long and contain uppercase, number, and special character"
            );
        }

        PasswordResetToken resetToken = null;
        String resetMethod = null;

        // Validate either token or OTP
        if (token != null && !token.trim().isEmpty()) {
            logger.debug("Validating reset token");
            resetToken = passwordResetTokenService.validateToken(token);
            resetMethod = "EMAIL_LINK";
        } else if (otp != null && !otp.trim().isEmpty()) {
            logger.debug("Validating reset OTP");
            resetToken = passwordResetTokenService.validateOtp(otp);
            resetMethod = "OTP";
        } else {
            logger.warn("No valid token or OTP provided for password reset");
            throw new InvalidPasswordResetTokenException("Either token or OTP is required");
        }

        User user = resetToken.getUser();

        try {
            // Update password
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            // Mark token as used
            passwordResetTokenService.markTokenAsUsed(resetToken);

            auditService.logPasswordReset(user, true, resetMethod, "Password successfully reset");
            logger.info("Password reset successful for user: {}", user.getEmail());
        } catch (Exception e) {
            auditService.logPasswordReset(user, false, resetMethod, "Error during reset: " + e.getMessage());
            logger.error("Error resetting password for user: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to reset password", e);
        }
    }

    /**
     * Validates password strength
     */
    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < passwordMinLength) {
            return false;
        }

        // Check for uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            return false;
        }

        // Check for number
        if (!password.matches(".*[0-9].*")) {
            return false;
        }

        // Check for special character
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            return false;
        }

        return true;
    }

}
