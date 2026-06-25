package com.newsportal.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(30);

    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        AttemptWindow window = attempts.get(normalize(key));
        if (window == null) {
            return false;
        }
        if (window.blockedUntil != null && Instant.now().isBefore(window.blockedUntil)) {
            return true;
        }
        if (window.blockedUntil != null && Instant.now().isAfter(window.blockedUntil)) {
            attempts.remove(normalize(key));
        }
        return false;
    }

    public long getRemainingBlockSeconds(String key) {
        AttemptWindow window = attempts.get(normalize(key));
        if (window == null || window.blockedUntil == null) {
            return 0;
        }
        long seconds = Duration.between(Instant.now(), window.blockedUntil).getSeconds();
        return Math.max(seconds, 0);
    }

    public void onFailure(String key) {
        String normalizedKey = normalize(key);
        AttemptWindow window = attempts.getOrDefault(normalizedKey, new AttemptWindow(0, Instant.now(), null));
        Instant now = Instant.now();

        if (Duration.between(window.firstAttemptAt, now).compareTo(WINDOW) > 0) {
            window = new AttemptWindow(0, now, null);
        }

        window.attempts = window.attempts + 1;
        if (window.attempts >= MAX_ATTEMPTS) {
            window.blockedUntil = now.plus(BLOCK_DURATION);
        }

        attempts.put(normalizedKey, window);
    }

    public void onSuccess(String key) {
        attempts.remove(normalize(key));
    }

    private String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }

    private static class AttemptWindow {
        private int attempts;
        private Instant firstAttemptAt;
        private Instant blockedUntil;

        private AttemptWindow(int attempts, Instant firstAttemptAt, Instant blockedUntil) {
            this.attempts = attempts;
            this.firstAttemptAt = firstAttemptAt;
            this.blockedUntil = blockedUntil;
        }
    }
}
