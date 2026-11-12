package com.olp.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for tracking blacklisted JWT tokens (e.g., on logout or single-use consumption)
 * and revoking all active sessions for a user (e.g., following a password reset).
 */
@Service
public class TokenBlacklistService {

    // Store blacklisted tokens with the instant they were revoked
    private final Map<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

    // Store user revocation timestamp: all tokens issued before this instant are invalid
    private final Map<Integer, Instant> userRevocationMap = new ConcurrentHashMap<>();

    /**
     * Blacklist an individual token so it can no longer be used.
     */
    public void blacklistToken(String token) {
        if (token != null && !token.isBlank()) {
            blacklistedTokens.put(token.trim(), Instant.now());
        }
    }

    /**
     * Check if a specific token has been blacklisted.
     */
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return blacklistedTokens.containsKey(token.trim());
    }

    /**
     * Revoke all active sessions for a user (called after password reset).
     */
    public void revokeAllUserTokens(Integer userId) {
        if (userId != null) {
            userRevocationMap.put(userId, Instant.now());
        }
    }

    /**
     * Check if a token for the user was issued prior to a password reset / session revocation.
     */
    public boolean isUserTokenRevoked(Integer userId, Date issuedAt) {
        if (userId == null) {
            return false;
        }
        Instant revokedAt = userRevocationMap.get(userId);
        if (revokedAt == null) {
            return false;
        }
        if (issuedAt == null) {
            return true;
        }
        // If the token was issued before the revocation timestamp, it is invalid
        return issuedAt.toInstant().isBefore(revokedAt.plusSeconds(1));
    }

    /**
     * Clear all records (useful for testing).
     */
    public void clear() {
        blacklistedTokens.clear();
        userRevocationMap.clear();
    }
}

