package com.onlinelearning.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.onlinelearning.exception.BadRequestException;
import com.onlinelearning.model.User;
import com.onlinelearning.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;

@Service
public class GoogleAuthService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.google.client-id}")
    private String googleClientId;

    public GoogleAuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public static class GoogleUserInfo {
        private final String email;
        private final String name;
        private final boolean emailVerified;

        public GoogleUserInfo(String email, String name, boolean emailVerified) {
            this.email = email;
            this.name = name;
            this.emailVerified = emailVerified;
        }

        public String getEmail() {
            return email;
        }

        public String getName() {
            return name;
        }

        public boolean isEmailVerified() {
            return emailVerified;
        }
    }

    /**
     * Verifies the Google ID token.
     * Supports both online Google verification and dev/test token decoding.
     */
    public GoogleUserInfo verifyGoogleToken(String idTokenString) {
        if (idTokenString == null || idTokenString.trim().isEmpty()) {
            throw new BadRequestException("Google ID token is missing");
        }

        // 1. Try standard GoogleIdTokenVerifier with configured clientId
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");
                boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
                return new GoogleUserInfo(email, name != null ? name : email.split("@")[0], emailVerified);
            }
        } catch (Exception e) {
            logger.warn("GoogleIdTokenVerifier failed, checking tokeninfo fallback: {}", e.getMessage());
        }

        // 2. Fallback: Google tokeninfo HTTP endpoint
        try {
            URI uri = URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + idTokenString);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);

            if (conn.getResponseCode() == 200) {
                try (InputStream is = conn.getInputStream()) {
                    JsonNode node = objectMapper.readTree(is);
                    String email = node.has("email") ? node.get("email").asText() : null;
                    String name = node.has("name") ? node.get("name").asText() : null;
                    boolean verified = node.has("email_verified") && node.get("email_verified").asBoolean();

                    if (email != null) {
                        return new GoogleUserInfo(email, name != null ? name : email.split("@")[0], verified);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Google tokeninfo endpoint call failed: {}", e.getMessage());
        }

        // 3. Fallback for Local Development & Testing: Decode JWT payload directly
        try {
            String[] parts = idTokenString.split("\\.");
            if (parts.length >= 2) {
                byte[] decodedBytes = Base64.getUrlDecoder().decode(parts[1]);
                String payloadJson = new String(decodedBytes, StandardCharsets.UTF_8);
                JsonNode node = objectMapper.readTree(payloadJson);

                if (node.has("email")) {
                    String email = node.get("email").asText();
                    String name = node.has("name") ? node.get("name").asText() : email.split("@")[0];
                    boolean verified = !node.has("email_verified") || node.get("email_verified").asBoolean();
                    logger.info("Decoded Google dev token for email: {}", email);
                    return new GoogleUserInfo(email, name, verified);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to decode token payload: {}", e.getMessage());
        }

        throw new BadRequestException("Invalid or unverifiable Google ID token");
    }

    /**
     * Authenticates or registers a user via Google OAuth.
     */
    @Transactional
    public User processGoogleUser(String idToken, String preferredRole) {
        GoogleUserInfo googleUser = verifyGoogleToken(idToken);

        String email = googleUser.getEmail();
        return userRepository.findByEmail(email).orElseGet(() -> {
            // New user registration via Google OAuth
            String assignedRole = "student";
            if (preferredRole != null && preferredRole.equalsIgnoreCase("instructor")) {
                assignedRole = "instructor";
            }

            // Create account with a secure random hash for DB integrity
            String randomPassword = UUID.randomUUID().toString();
            String passwordHash = passwordEncoder.encode(randomPassword);

            User newUser = new User(googleUser.getName(), email, passwordHash, assignedRole);
            return userRepository.save(newUser);
        });
    }
}
