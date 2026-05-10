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
        tracker.attempts.removeIf(timestamp -> timestamp < windowStart);

        // Add new attempt
        tracker.attempts.add(now);

        logger.debug("Recorded attempt for {}: {} attempts in window", key, tracker.attempts.size());
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
            return false;
        }

        // Count attempts within the window
        long attemptCount = tracker.attempts.stream()
            .filter(timestamp -> timestamp >= windowStart)
            .count();

        boolean isLimited = attemptCount >= maxForgotPasswordAttempts;

        if (isLimited) {
            logger.warn("Rate limit exceeded for {}: {} attempts", key, attemptCount);
        }

        return isLimited;
    }

    /**
     * Clears all attempt tracking (run periodically)
     */
    @Scheduled(fixedRateString = "${security.password.reset.attempt-cache-clear-interval:3660000}")
    public void clearAttemptTrackers() {
        logger.info("Clearing all rate limit attempt trackers");
        attemptTrackers.clear();
    }

    /**
     * Inner class to track attempts
     */
    private static class AttemptTracker {
        java.util.List<Long> attempts = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
    }
}
