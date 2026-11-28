package com.olp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olp.dto.PaymentSimulationDtos.SavedMethodRequest;
import com.olp.dto.PaymentTransactionResponse;
import com.olp.entity.Payment;
import com.olp.entity.User;
import com.olp.entity.WebhookEvent;
import com.olp.repository.PaymentRepository;
import com.olp.repository.WalletRepository;
import com.olp.repository.WebhookEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/** Opaque simulated provider tokens. Durable consent/revocation events contain no card data. */
@Service
public class SimulatedPaymentMethods {
    @Autowired(required = false)
    private PaymentGateway gateway;
    private final WebhookEventRepository events;
    private final PaymentRepository payments;
    private final WalletRepository locks;
    private final PaymentService settlement;
    private final ObjectMapper json = new ObjectMapper();

    public SimulatedPaymentMethods(WebhookEventRepository events, PaymentRepository payments,
                                   WalletRepository locks, PaymentService settlement) {
        this.events = events;
        this.payments = payments;
        this.locks = locks;
        this.settlement = settlement;
    }

    @Transactional
    public Map<String, Object> create(User user, String method, boolean consent) {
        return create(user, new SavedMethodRequest(method, consent));
    }

    @Transactional
    public Map<String, Object> create(User user, SavedMethodRequest request) {
        requireUser(user);
        if (request == null || !request.consent()) {
            throw new IllegalArgumentException("Explicit consent is required to save a simulated payment method.");
        }
        String method = request.method();
        if (method == null || !Set.of("card", "upi", "netbanking", "wallet").contains(method)) {
            throw new IllegalArgumentException("Unsupported payment method.");
        }
        String token = gateway == null
                ? "simtok_" + UUID.randomUUID().toString().replace("-", "")
                : gateway.createMethodToken(method);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("owner", user.getId());
        data.put("method", method);
        data.put("consent", true);
        data.put("createdAt", Instant.now().toString());
        data.put("expiresAt", Instant.now().plusSeconds(31536000).toString());

        if (request.last4() != null && !request.last4().isBlank()) {
            data.put("last4", request.last4().trim());
        }
        if (request.cardBrand() != null && !request.cardBrand().isBlank()) {
            data.put("cardBrand", request.cardBrand());
        }
        if (request.expiryMonth() != null && request.expiryYear() != null) {
            data.put("expiry", request.expiryMonth() + "/" + request.expiryYear());
        }
        if (request.cardholderName() != null && !request.cardholderName().isBlank()) {
            data.put("cardholderName", request.cardholderName());
        } else if ("card".equalsIgnoreCase(method)) {
            data.put("cardholderName", user.getName() != null ? user.getName() : "Student Learner");
        }
        if (request.upiId() != null && !request.upiId().isBlank()) {
            data.put("upiId", request.upiId());
        } else if ("upi".equalsIgnoreCase(method) && user.getEmail() != null && !user.getEmail().isBlank()) {
            data.put("upiId", user.getEmail().split("@")[0] + "@okhdfcbank");
        }
        if (request.bankName() != null && !request.bankName().isBlank()) {
            data.put("bankName", request.bankName());
        }
        if (request.walletProvider() != null && !request.walletProvider().isBlank()) {
            data.put("walletProvider", request.walletProvider());
        }

        enrichRecognizableDetailsInto(data);
        if (request.displayName() != null && !request.displayName().isBlank()) {
            data.put("displayName", request.displayName());
        }

        events.save(new WebhookEvent(token, "method.created", encode(data)));
        return data;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(User user) {
        requireUser(user);
        return events.findByEventType("method.created").stream()
                .map(e -> decode(e.getPayload()))
                .filter(d -> isOwnedByUser(d, user))
                .filter(d -> !events.existsByEventId("revoked_" + d.get("token")))
                .filter(d -> d.get("expiresAt") instanceof String exp && Instant.parse(exp).isAfter(Instant.now()))
                .map(this::enrichRecognizableDetails)
                .toList();
    }

    private Map<String, Object> enrichRecognizableDetails(Map<String, Object> orig) {
        Map<String, Object> res = new LinkedHashMap<>(orig);
        enrichRecognizableDetailsInto(res);
        return res;
    }

    private void enrichRecognizableDetailsInto(Map<String, Object> res) {
        String method = String.valueOf(res.get("method"));
        String token = res.get("token") != null ? String.valueOf(res.get("token")) : "";
        String last4 = res.get("last4") != null
                ? String.valueOf(res.get("last4"))
                : (token.length() >= 4 ? token.substring(token.length() - 4) : "4242");
        res.put("last4", last4);

        if ("card".equalsIgnoreCase(method)) {
            String brand = nonBlankOrDefault(res.get("cardBrand"), "Visa");
            String expiry = nonBlankOrDefault(res.get("expiry"), "12/28");
            res.put("cardBrand", brand);
            res.put("expiry", expiry);
            putIfMissingDisplayName(res, brand + " •••• " + last4 + " (Exp " + expiry + ")");
        } else if ("upi".equalsIgnoreCase(method)) {
            String upiId = nonBlankOrDefault(res.get("upiId"), "student" + last4 + "@okhdfcbank");
            res.put("upiId", upiId);
            putIfMissingDisplayName(res, "UPI: " + upiId);
        } else if ("netbanking".equalsIgnoreCase(method)) {
            String bank = nonBlankOrDefault(res.get("bankName"), "HDFC Bank NetBanking");
            res.put("bankName", bank);
            putIfMissingDisplayName(res, bank + " (A/C •••• " + last4 + ")");
        } else if ("wallet".equalsIgnoreCase(method)) {
            String wallet = nonBlankOrDefault(res.get("walletProvider"), "OLP Learner Wallet");
            res.put("walletProvider", wallet);
            putIfMissingDisplayName(res, wallet + " (Linked •••• " + last4 + ")");
        }
    }

    private static String nonBlankOrDefault(Object val, String fallback) {
        return (val != null && !String.valueOf(val).isBlank()) ? String.valueOf(val) : fallback;
    }

    private static void putIfMissingDisplayName(Map<String, Object> res, String defaultDisplayName) {
        if (res.get("displayName") == null) {
            res.put("displayName", defaultDisplayName);
        }
    }

    private static boolean isOwnedByUser(Map<String, Object> data, User user) {
        return data != null
                && data.get("owner") instanceof Number ownerId
                && user.getId() != null
                && ownerId.intValue() == user.getId();
    }

    @Transactional
    public void revoke(User user, String token) {
        requireUser(user);
        locks.lockStudent(user.getId());
        validate(user, token);
        if (!events.existsByEventId("revoked_" + token)) {
            events.save(new WebhookEvent("revoked_" + token, "method.revoked",
                    encode(Map.of("owner", user.getId(), "token", token))));
        }
    }

    /** A repeat/subscription renewal pays an existing order; retrying that order is idempotent. */
    @Transactional(noRollbackFor = com.olp.exception.PaymentOrderExpiredException.class)
    public PaymentTransactionResponse pay(User user, String token, String orderId) {
        requireUser(user);
        Payment p = payments.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found."));
        if (p.getStudent() == null || user.getId() == null || !p.getStudent().getId().equals(user.getId())) {
            throw new AccessDeniedException("This order belongs to another account.");
        }
        locks.lockStudent(user.getId());
        Map<String, Object> methodData = validate(user, token);
        if (events.existsByEventId("revoked_" + token)) {
            throw new IllegalArgumentException("This saved method was revoked.");
        }
        String savedMethod = String.valueOf(methodData.get("method"));
        String label = "card".equalsIgnoreCase(savedMethod) ? "Card" :
                ("upi".equalsIgnoreCase(savedMethod) ? "UPI" :
                ("netbanking".equalsIgnoreCase(savedMethod) ? "Net Banking" :
                ("wallet".equalsIgnoreCase(savedMethod) ? "Wallet" : savedMethod)));
        return settlement.confirmPayment(orderId, label);
    }

    private Map<String, Object> validate(User user, String token) {
        if (token == null || !token.matches("simtok_[a-f0-9]{32}")) {
            throw new IllegalArgumentException("Invalid saved method.");
        }
        Map<String, Object> d = decode(events.findByEventId(token)
                .filter(e -> "method.created".equals(e.getEventType()))
                .orElseThrow(() -> new IllegalArgumentException("Saved method not found."))
                .getPayload());
        if (!isOwnedByUser(d, user)) {
            throw new AccessDeniedException("Saved method belongs to another account.");
        }
        if (!(d.get("expiresAt") instanceof String exp) || !Instant.parse(exp).isAfter(Instant.now())) {
            throw new IllegalArgumentException("Saved method expired.");
        }
        return d;
    }

    private void requireUser(User u) {
        if (u == null || !Set.of("student", "admin").contains(u.getRole())) {
            throw new AccessDeniedException("Sign in as a student.");
        }
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> decode(String value) {
        try {
            return json.readValue(value, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid saved method record.", e);
        }
    }
}
