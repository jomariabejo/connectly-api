package com.jomariabejo.connectly_api.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RateLimitingService {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);

    // In-memory tracking of attempts (for simplicity; use Redis in production)
    private final Map<String, AttemptTracker> attemptTrackers = new ConcurrentHashMap<>();

    @Value("${security.password.reset.max-attempts:5}")
    private int maxForgotPasswordAttempts;

    @Value("${security.password.reset.attempt-window-minutes:60}")
    private int attemptWindowMinutes;

    /**
     * Records an attempt for the given email and action
     */
    public void recordAttempt(String email, String action) {
        String key = email + ":" + action;
        long now = System.currentTimeMillis();
        long windowStart = now - (attemptWindowMinutes * 60 * 1000L);

        attemptTrackers.putIfAbsent(key, new AttemptTracker());
        AttemptTracker tracker = attemptTrackers.get(key);

        // Remove old attempts outside the window
        int beforeSize = tracker.attempts.size();
        tracker.attempts.removeIf(timestamp -> timestamp < windowStart);
        int afterSize = tracker.attempts.size();
        
        if (beforeSize != afterSize) {
            logger.debug("RATE_LIMIT_CLEANUP | action={} | email={} | removed_expired_attempts={}", 
                        action, email, beforeSize - afterSize);
        }

        // Add new attempt
        tracker.attempts.add(now);

        logger.debug("RATE_LIMIT_RECORD | action={} | email={} | attempts_in_window={} | max_allowed={}", 
                     action, email, tracker.attempts.size(), maxForgotPasswordAttempts);
    }

    /**
     * Checks if the provided email and action is rate limited
     */
    public boolean isRateLimited(String email, String action) {
        String key = email + ":" + action;
        long now = System.currentTimeMillis();
        long windowStart = now - (attemptWindowMinutes * 60 * 1000L);

        AttemptTracker tracker = attemptTrackers.get(key);

        if (tracker == null) {
            logger.trace("RATE_LIMIT_CHECK | action={} | email={} | no_prior_attempts | status=ALLOWED", 
                        action, email);
            return false;
        }

        // Count attempts within the window
        long attemptCount = tracker.attempts.stream()
            .filter(timestamp -> timestamp >= windowStart)
            .count();

        boolean isLimited = attemptCount >= maxForgotPasswordAttempts;

        String status = isLimited ? "LIMITED" : "ALLOWED";
        logger.info("RATE_LIMIT_CHECK | action={} | email={} | attempts_in_window={} | max_allowed={} | status={}", 
                    action, email, attemptCount, maxForgotPasswordAttempts, status);

        if (isLimited) {
            logger.warn("RATE_LIMIT_EXCEEDED | action={} | email={} | attempts={} | window_minutes={}", 
                       action, email, attemptCount, attemptWindowMinutes);
        }

        return isLimited;
    }

    /**
     * Clears all attempt tracking (run periodically)
     */
    @Scheduled(fixedRateString = "${security.password.reset.attempt-cache-clear-interval:3660000}")
    public void clearAttemptTrackers() {
        int trackerCount = attemptTrackers.size();
        logger.info("RATE_LIMIT_CACHE_CLEAR_START | total_trackers_to_clear={}", trackerCount);
        
        attemptTrackers.clear();
        
        logger.info("RATE_LIMIT_CACHE_CLEAR_COMPLETE | trackers_cleared={}", trackerCount);
    }

    /**
     * Inner class to track attempts
     */
    private static class AttemptTracker {
        java.util.List<Long> attempts = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
    }
}
