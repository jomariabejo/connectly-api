package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.model.VerificationToken;
import com.jomariabejo.connectly_api.repository.UserRepository;
import com.jomariabejo.connectly_api.repository.VerificationTokenRepository;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class VerificationTokenService {

    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(VerificationTokenService.class);

    @Autowired
    public VerificationTokenService(VerificationTokenRepository tokenRepository, UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    public VerificationToken createVerificationToken(User user, String token) {
        // Check if a token already exists for this user
        VerificationToken existingToken = tokenRepository.findByUser(user);
        if (existingToken != null) {
            logger.info("Removing existing token for user: " + user.getEmail());
            tokenRepository.delete(existingToken);
        }

        // Create new token
        VerificationToken verificationToken = new VerificationToken(token, user);
        logger.info("Creating new verification token for user: " + user.getEmail());
        return tokenRepository.save(verificationToken);
    }

    public Optional<VerificationToken> getVerificationToken(String token) {
        return (tokenRepository.findByToken(token));
    }

    public VerificationToken getTokenByUser(User user) {
        return tokenRepository.findByUser(user);
    }

    public void deleteToken(VerificationToken token) {
        tokenRepository.delete(token);
    }

    @Transactional
    public boolean validateToken(String token) {

        VerificationToken verificationToken = getVerificationToken(token)
            .orElseThrow(() -> {
                logger.warn("No token found: {}", token);
                return new RuntimeException("Invalid token");
            });

        if (verificationToken.getExpiryDate().before(new Date())) {
            logger.warn("Token expired: {}", token);
            tokenRepository.delete(verificationToken);
            return false;
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        tokenRepository.delete(verificationToken);

        return true;
    }

    @Scheduled(cron = "0 0 */12 * * *") // Run every 12 hours
    public void cleanupExpiredTokens() {
        Calendar cal = Calendar.getInstance();
        List<VerificationToken> expiredTokens = tokenRepository.findAllExpiredTokens(cal.getTime());
        logger.info("Found " + expiredTokens.size() + " expired tokens for cleanup");
        tokenRepository.deleteAll(expiredTokens);
    }
}
