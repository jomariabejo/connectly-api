package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.exception.InvalidPasswordResetTokenException;
import com.jomariabejo.connectly_api.exception.PasswordResetAttemptsExceededException;
import com.jomariabejo.connectly_api.exception.PasswordResetTokenExpiredException;
import com.jomariabejo.connectly_api.model.PasswordResetToken;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.PasswordResetTokenRepository;
import com.jomariabejo.connectly_api.util.OtpGenerator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetTokenService.class);

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${security.password.reset.max-otp-attempts:3}")
    private int maxOtpAttempts;

    /**
     * Creates a password reset token for email delivery
     */
    @Transactional
    public String createEmailResetToken(User user) {
        logger.debug("Creating email reset token for user: {}", user.getEmail());
        
        // Invalidate any previous unused tokens for this user
        invalidateUserTokens(user);

        String token = OtpGenerator.generateSecureToken();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        resetToken.setTokenType(PasswordResetToken.TokenType.LINK);

        passwordResetTokenRepository.save(resetToken);
        logger.info("Email reset token created for user: {}", user.getEmail());
        
        return token;
    }

    /**
     * Creates a password reset token with OTP for OTP delivery
     */
    @Transactional
    public String createOtpResetToken(User user) {
        logger.debug("Creating OTP reset token for user: {}", user.getEmail());
        
        // Invalidate any previous unused tokens for this user
        invalidateUserTokens(user);

        String token = OtpGenerator.generateSecureToken();
        String otp = OtpGenerator.generateOtp();
        PasswordResetToken resetToken = new PasswordResetToken(token, otp, user, PasswordResetToken.TokenType.OTP);

        passwordResetTokenRepository.save(resetToken);
        logger.info("OTP reset token created for user: {}", user.getEmail());
        
        return otp; // Return OTP for sending to user
    }

    /**
     * Invalidates all unused tokens for a user
     */
    @Transactional
    public void invalidateUserTokens(User user) {
        logger.debug("Invalidating all unused tokens for user: {}", user.getEmail());
        
        List<PasswordResetToken> unusedTokens = passwordResetTokenRepository.findAllUnusedByUser(user);
        for (PasswordResetToken token : unusedTokens) {
            token.setUsed(true);
            passwordResetTokenRepository.save(token);
        }
        
        logger.debug("Invalidated {} tokens for user: {}", unusedTokens.size(), user.getEmail());
    }

    /**
     * Validates a reset token (for email-based reset)
     */
    @Transactional
    public PasswordResetToken validateToken(String token) {
        logger.debug("Validating password reset token: {}", token);
        
        Optional<PasswordResetToken> resetToken = passwordResetTokenRepository.findByToken(token);
        
        if (resetToken.isEmpty()) {
            logger.warn("Invalid password reset token: {}", token);
            throw new InvalidPasswordResetTokenException("Invalid password reset token");
        }

        PasswordResetToken prt = resetToken.get();

        if (prt.isExpired()) {
            logger.warn("Password reset token expired for user: {}", prt.getUser().getEmail());
            throw new PasswordResetTokenExpiredException("Password reset token has expired");
        }

        if (prt.isUsed()) {
            logger.warn("Password reset token already used for user: {}", prt.getUser().getEmail());
            throw new InvalidPasswordResetTokenException("Password reset token has already been used");
        }

        logger.info("Password reset token validated successfully for user: {}", prt.getUser().getEmail());
        return prt;
    }

    /**
     * Validates an OTP (for OTP-based reset)
     */
    @Transactional
    public PasswordResetToken validateOtp(String otp) {
        logger.debug("Validating OTP");
        
        Optional<PasswordResetToken> resetToken = passwordResetTokenRepository.findByOtp(otp);
        
        if (resetToken.isEmpty()) {
            logger.warn("Invalid OTP");
            throw new InvalidPasswordResetTokenException("Invalid OTP");
        }

        PasswordResetToken prt = resetToken.get();

        if (prt.isExpired()) {
            logger.warn("OTP expired for user: {}", prt.getUser().getEmail());
            throw new PasswordResetTokenExpiredException("OTP has expired");
        }

        if (prt.isUsed()) {
            logger.warn("OTP already used for user: {}", prt.getUser().getEmail());
            throw new InvalidPasswordResetTokenException("OTP has already been used");
        }

        // Check attempt count
        if (prt.getAttemptCount() >= maxOtpAttempts) {
            logger.warn("Max OTP attempts exceeded for user: {}", prt.getUser().getEmail());
            prt.setUsed(true);
            passwordResetTokenRepository.save(prt);
            throw new PasswordResetAttemptsExceededException(
                "Maximum OTP validation attempts exceeded. Please request a new OTP."
            );
        }

        // Increment attempt count
        prt.setAttemptCount(prt.getAttemptCount() + 1);
        passwordResetTokenRepository.save(prt);

        logger.info("OTP validated successfully for user: {}", prt.getUser().getEmail());
        return prt;
    }

    /**
     * Marks a token as used after successful password reset
     */
    @Transactional
    public void markTokenAsUsed(PasswordResetToken token) {
        logger.debug("Marking token as used for user: {}", token.getUser().getEmail());
        
        token.setUsed(true);
        passwordResetTokenRepository.save(token);
        
        logger.info("Token marked as used for user: {}", token.getUser().getEmail());
    }

    /**
     * Deletes expired tokens
     */
    @Transactional
    public void deleteExpiredTokens() {
        logger.debug("Deleting expired password reset tokens");
        
        List<PasswordResetToken> expiredTokens = passwordResetTokenRepository.findExpiredTokens();
        if (!expiredTokens.isEmpty()) {
            passwordResetTokenRepository.deleteAll(expiredTokens);
            logger.info("Deleted {} expired password reset tokens", expiredTokens.size());
        } else {
            logger.debug("No expired password reset tokens found");
        }
    }
}
