package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.exception.InvalidVerificationException;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.model.VerificationToken;
import com.jomariabejo.connectly_api.repository.UserRepository;
import com.jomariabejo.connectly_api.repository.VerificationTokenRepository;
import com.jomariabejo.connectly_api.util.OtpGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class VerificationTokenService {

    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(VerificationTokenService.class);

    @Value("${security.verification.max-otp-attempts:5}")
    private int maxOtpAttempts;

    public VerificationTokenService(VerificationTokenRepository tokenRepository, UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public VerificationToken createVerificationToken(User user, String token) {
        VerificationToken existingToken = tokenRepository.findByUser(user);
        if (existingToken != null) {
            logger.info("Removing existing token for user: {}", user.getEmail());
            tokenRepository.delete(existingToken);
        }

        VerificationToken verificationToken = new VerificationToken(token, user);
        verificationToken.setOtp(OtpGenerator.generateOtp());
        verificationToken.setAttemptCount(0);
        logger.info("Creating new verification token for user: {}", user.getEmail());
        return tokenRepository.save(verificationToken);
    }

    public Optional<VerificationToken> getVerificationToken(String token) {
        return tokenRepository.findByToken(token);
    }

    @Transactional
    public User verifyByOtp(String email, String otp) {
        String normalizedEmail = email.trim().toLowerCase();
        String normalizedOtp = otp.trim();

        User user = userRepository.findByEmailNormalized(normalizedEmail)
                .orElseThrow(() -> new InvalidVerificationException("Invalid verification code"));

        VerificationToken verificationToken = tokenRepository.findByUser(user);
        if (verificationToken == null) {
            throw new InvalidVerificationException("Invalid verification code");
        }

        if (verificationToken.getExpiryDate().before(new Date())) {
            tokenRepository.delete(verificationToken);
            throw new InvalidVerificationException("Verification code has expired");
        }

        if (verificationToken.getAttemptCount() >= maxOtpAttempts) {
            throw new InvalidVerificationException("Too many attempts. Request a new verification code.");
        }

        if (user.isEnabled()) {
            tokenRepository.delete(verificationToken);
            throw new InvalidVerificationException("Account is already verified");
        }

        if (verificationToken.getOtp() == null
                || !verificationToken.getOtp().equals(normalizedOtp)) {
            verificationToken.setAttemptCount(verificationToken.getAttemptCount() + 1);
            tokenRepository.save(verificationToken);
            throw new InvalidVerificationException("Invalid verification code");
        }

        user.setEnabled(true);
        user.setVerificationToken(null);
        userRepository.save(user);
        tokenRepository.delete(verificationToken);
        return user;
    }

    @Transactional
    public void revokeForUser(User user) {
        VerificationToken existingToken = tokenRepository.findByUser(user);
        if (existingToken != null) {
            tokenRepository.delete(existingToken);
        }
    }

    @Transactional
    public boolean validateToken(String token) {
        VerificationToken verificationToken = getVerificationToken(token)
                .orElseThrow(() -> {
                    logger.warn("No token found: {}", token);
                    return new InvalidVerificationException("Invalid verification token");
                });

        if (verificationToken.getExpiryDate().before(new Date())) {
            logger.warn("Token expired: {}", token);
            tokenRepository.delete(verificationToken);
            return false;
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        user.setVerificationToken(null);
        userRepository.save(user);
        tokenRepository.delete(verificationToken);
        return true;
    }

    @Scheduled(cron = "0 0 */12 * * *")
    public void cleanupExpiredTokens() {
        Calendar cal = Calendar.getInstance();
        List<VerificationToken> expiredTokens = tokenRepository.findAllExpiredTokens(cal.getTime());
        logger.info("Found {} expired tokens for cleanup", expiredTokens.size());
        tokenRepository.deleteAll(expiredTokens);
    }
}
