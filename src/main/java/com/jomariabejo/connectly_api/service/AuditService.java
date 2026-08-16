package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.model.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class AuditService {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    // DateTimeFormatter is immutable and thread-safe, unlike the SimpleDateFormat it replaced.
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Logs password reset initiation (email or OTP request)
     */
    public void logPasswordResetInitiated(User user, String method) {
        logger.info(
            "PASSWORD_RESET_INITIATED | timestamp={} | userId={} | email={} | method={} | ipAddress={}",
            dateFormat.format(LocalDateTime.now()),
            user.getId(),
            user.getEmail(),
            method,
            getClientIp()
        );
    }

    /**
     * Logs password reset attempt (success or failure)
     */
    public void logPasswordReset(User user, boolean success, String method, String reason) {
        String status = success ? "SUCCESS" : "FAILURE";
        logger.info(
            "PASSWORD_RESET_ATTEMPT | timestamp={} | userId={} | email={} | method={} | status={} | reason={} | ipAddress={}",
            dateFormat.format(LocalDateTime.now()),
            user.getId(),
            user.getEmail(),
            method,
            status,
            reason,
            getClientIp()
        );
    }

    /**
     * Logs invalid password reset attempts
     */
    public void logInvalidResetAttempt(String email, String method, String reason) {
        logger.warn(
            "PASSWORD_RESET_INVALID_ATTEMPT | timestamp={} | email={} | method={} | reason={} | ipAddress={}",
            dateFormat.format(LocalDateTime.now()),
            email,
            method,
            reason,
            getClientIp()
        );
    }

    /**
     * Logs rate limit exceeded events
     */
    public void logRateLimitExceeded(String email, String action) {
        logger.warn(
            "PASSWORD_RESET_RATE_LIMIT_EXCEEDED | timestamp={} | email={} | action={} | ipAddress={}",
            dateFormat.format(LocalDateTime.now()),
            email,
            action,
            getClientIp()
        );
    }

    /**
     * Get client IP address from request context
     * (In a real implementation, you would extract this from HttpServletRequest)
     */
    private String getClientIp() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (java.net.UnknownHostException e) {
            return "UNKNOWN";
        }
    }
}
