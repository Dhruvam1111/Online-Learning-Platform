package com.olp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class TokenBlacklistServiceTest {

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService();
    }

    @Test
    void blacklistAndCheck_trimsTokensAndIgnoresNullOrBlank() {
        assertFalse(tokenBlacklistService.isTokenBlacklisted(null));
        assertFalse(tokenBlacklistService.isTokenBlacklisted("   "));

        tokenBlacklistService.blacklistToken(null);
        tokenBlacklistService.blacklistToken("  ");
        assertFalse(tokenBlacklistService.isTokenBlacklisted(""));

        tokenBlacklistService.blacklistToken("  jwt.token.abc  ");
        assertTrue(tokenBlacklistService.isTokenBlacklisted("jwt.token.abc"));
        assertTrue(tokenBlacklistService.isTokenBlacklisted(" jwt.token.abc "));
        assertFalse(tokenBlacklistService.isTokenBlacklisted("jwt.token.other"));
    }

    @Test
    void revokeAllUserTokens_handlesNullAndBoundaryTimestamps() {
        assertFalse(tokenBlacklistService.isUserTokenRevoked(null, new Date()));
        assertFalse(tokenBlacklistService.isUserTokenRevoked(99, new Date()));

        tokenBlacklistService.revokeAllUserTokens(null);
        tokenBlacklistService.revokeAllUserTokens(42);

        // Null issuedAt is treated as revoked once user revocation exists
        assertTrue(tokenBlacklistService.isUserTokenRevoked(42, null));

        // Token issued in the past or within revocation second is revoked
        Date pastIssuedAt = Date.from(Instant.now().minusSeconds(10));
        assertTrue(tokenBlacklistService.isUserTokenRevoked(42, pastIssuedAt));

        // Token issued well after revocation (+2s) remains valid
        Date futureIssuedAt = Date.from(Instant.now().plusSeconds(5));
        assertFalse(tokenBlacklistService.isUserTokenRevoked(42, futureIssuedAt));

        // Another user's tokens remain valid
        assertFalse(tokenBlacklistService.isUserTokenRevoked(43, pastIssuedAt));
    }

    @Test
    void clear_removesAllBlacklistedTokensAndUserRevocations() {
        tokenBlacklistService.blacklistToken("tok-1");
        tokenBlacklistService.revokeAllUserTokens(7);

        assertTrue(tokenBlacklistService.isTokenBlacklisted("tok-1"));
        assertTrue(tokenBlacklistService.isUserTokenRevoked(7, new Date()));

        tokenBlacklistService.clear();

        assertFalse(tokenBlacklistService.isTokenBlacklisted("tok-1"));
        assertFalse(tokenBlacklistService.isUserTokenRevoked(7, new Date()));
    }
}
