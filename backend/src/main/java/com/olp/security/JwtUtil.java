package com.olp.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        // The secret string must be at least 256 bits (32 bytes) for HS256
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Create a JWT token containing the user's ID and role.
     * Person 3 and Person 4: this is what gets sent to the client on login.
     */
    public String generateToken(Integer userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validate the token signature and check if it's expired.
     * Returns true only if the token is valid and not expired.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Token is invalid, expired, or tampered with
            return false;
        }
    }

    /**
     * Extract the user ID from a valid token.
     * Call validateToken() first — this method assumes the token is valid.
     */
    public Integer getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Integer.parseInt(claims.getSubject());
    }

    /**
     * Extract the role from a valid token.
     */
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("role", String.class);
    }

    /**
     * Generate a dedicated, single-use password reset token with custom expiration.
     */
    public String generatePasswordResetToken(Integer userId, long customExpirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + customExpirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("purpose", "PASSWORD_RESET")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validate that a token is signed, not expired, and carries purpose = 'PASSWORD_RESET'.
     */
    public boolean validatePasswordResetToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return "PASSWORD_RESET".equals(claims.get("purpose", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extract the issue date (issuedAt) from a token.
     */
    public Date getIssuedAtFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getIssuedAt();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Extract purpose claim from token.
     */
    public String getPurposeFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("purpose", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
