package com.olp.service;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

/** Local simulator signs exactly the bytes sent, with a five-minute replay window. */
@Component
public class PaymentWebhookSignature {
    private final byte[] key;
    public PaymentWebhookSignature(@Value("${payments.webhook-secret:}") String secret) {
        if (secret == null || secret.isBlank()) { key = new byte[32]; new SecureRandom().nextBytes(key); }
        else { key = secret.getBytes(StandardCharsets.UTF_8); if (key.length < 32) throw new IllegalArgumentException("Webhook secret needs at least 32 bytes."); }
    }
    public String sign(String payload) {
        long timestamp = Instant.now().getEpochSecond();
        return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(digest(timestamp + "." + payload));
    }
    public boolean verify(String payload, String header) {
        if (payload == null || payload.isBlank() || payload.length() > 16384 || header == null) return false;
        try {
            if (!header.matches("t=[0-9]{1,12},v1=[a-fA-F0-9]{64}")) return false;
            String[] parts = header.split(",v1=");
            long timestamp = Long.parseLong(parts[0].substring(2));
            if (Math.abs(Instant.now().getEpochSecond() - timestamp) > 300) return false;
            return MessageDigest.isEqual(digest(timestamp + "." + payload), HexFormat.of().parseHex(parts[1]));
        } catch (RuntimeException ex) { return false; }
    }
    private byte[] digest(String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException e) { throw new IllegalStateException(e); }
    }
}
