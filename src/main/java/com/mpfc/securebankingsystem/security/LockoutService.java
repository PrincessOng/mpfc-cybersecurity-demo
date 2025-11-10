package com.mpfc.securebankingsystem.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LockoutService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(2);

    private static class AttemptInfo {
        int attempts = 0;
        Instant lockedUntil = null;
    }

    private final Map<String, AttemptInfo> store = new ConcurrentHashMap<>();

    public boolean recordFailure(String username) {
        AttemptInfo info = store.computeIfAbsent(username.toLowerCase(), k -> new AttemptInfo());
        if (isLocked(username)) return true;
        info.attempts++;
        if (info.attempts >= MAX_ATTEMPTS) {
            info.lockedUntil = Instant.now().plus(LOCK_DURATION);
            info.attempts = 0; // reset counter after locking
            return true;
        }
        return false;
    }

    public void reset(String username) {
        AttemptInfo info = store.get(username.toLowerCase());
        if (info != null) {
            info.attempts = 0;
            info.lockedUntil = null;
        }
    }

    public boolean isLocked(String username) {
        AttemptInfo info = store.get(username.toLowerCase());
        if (info == null || info.lockedUntil == null) return false;
        if (Instant.now().isAfter(info.lockedUntil)) {
            info.lockedUntil = null;
            return false;
        }
        return true;
    }

    public Instant lockedUntil(String username) {
        AttemptInfo info = store.get(username.toLowerCase());
        return (info == null) ? null : info.lockedUntil;
    }
}