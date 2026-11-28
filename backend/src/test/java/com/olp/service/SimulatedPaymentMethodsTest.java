package com.olp.service;

import com.olp.dto.PaymentSimulationDtos.SavedMethodRequest;
import com.olp.entity.User;
import com.olp.entity.WebhookEvent;
import com.olp.repository.PaymentRepository;
import com.olp.repository.WalletRepository;
import com.olp.repository.WebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SimulatedPaymentMethodsTest {

    private WebhookEventRepository events;
    private PaymentRepository payments;
    private WalletRepository locks;
    private PaymentService settlement;
    private SimulatedPaymentMethods service;
    private User student;

    @BeforeEach
    void setUp() {
        events = mock(WebhookEventRepository.class);
        payments = mock(PaymentRepository.class);
        locks = mock(WalletRepository.class);
        settlement = mock(PaymentService.class);
        service = new SimulatedPaymentMethods(events, payments, locks, settlement);

        student = new User("Dhruvam Learner", "dhruvam@example.com", "hash", "student");
        student.setId(55);
    }

    @Test
    void create_cardWithRecognizableDetails_storesInEventAndReturnsRecognizableData() {
        SavedMethodRequest request = new SavedMethodRequest(
                "card",
                true,
                "Mastercard",
                "8821",
                "09",
                "29",
                "Dhruvam Learner",
                null,
                null,
                null,
                null
        );

        Map<String, Object> result = service.create(student, request);
        assertNotNull(result);
        assertEquals("card", result.get("method"));
        assertEquals("Mastercard", result.get("cardBrand"));
        assertEquals("8821", result.get("last4"));
        assertEquals("09/29", result.get("expiry"));
        assertTrue(result.get("displayName").toString().contains("Mastercard •••• 8821"));

        verify(events).save(any(WebhookEvent.class));
    }

    @Test
    void create_upiWithRecognizableDetails_storesUpiIdAndDisplayName() {
        SavedMethodRequest request = new SavedMethodRequest(
                "upi",
                true,
                null,
                null,
                null,
                null,
                null,
                "dhruvam@okaxis",
                null,
                null,
                null
        );

        Map<String, Object> result = service.create(student, request);
        assertNotNull(result);
        assertEquals("upi", result.get("method"));
        assertEquals("dhruvam@okaxis", result.get("upiId"));
        assertEquals("UPI: dhruvam@okaxis", result.get("displayName"));
    }

    @Test
    void list_enrichesMissingDetailsWithSensibleDefaults() {
        String jsonPayload = """
                {
                    "token": "simtok_12345678901234567890123456781234",
                    "owner": 55,
                    "method": "card",
                    "consent": true,
                    "expiresAt": "%s"
                }
                """.formatted(Instant.now().plusSeconds(3600).toString());

        WebhookEvent event = new WebhookEvent("simtok_12345678901234567890123456781234", "method.created", jsonPayload);
        when(events.findByEventType("method.created")).thenReturn(List.of(event));
        when(events.existsByEventId(startsWith("revoked_"))).thenReturn(false);

        List<Map<String, Object>> methods = service.list(student);
        assertEquals(1, methods.size());
        Map<String, Object> m = methods.get(0);
        assertEquals("card", m.get("method"));
        assertEquals("Visa", m.get("cardBrand"));
        assertEquals("1234", m.get("last4"));
        assertNotNull(m.get("displayName"));
        assertTrue(m.get("displayName").toString().contains("Visa •••• 1234"));
    }

    @Test
    void create_withoutConsentThrowsException() {
        SavedMethodRequest req = new SavedMethodRequest("card", false);
        assertThrows(IllegalArgumentException.class, () -> service.create(student, req));
    }

    @Test
    void list_handlesRecordsWithMissingOrDifferentOwnerSafely() {
        String missingOwnerJson = """
                {
                    "token": "simtok_11111111111111111111111111111111",
                    "method": "upi",
                    "consent": true,
                    "expiresAt": "%s"
                }
                """.formatted(Instant.now().plusSeconds(3600).toString());
        String otherOwnerJson = """
                {
                    "token": "simtok_22222222222222222222222222222222",
                    "owner": 999,
                    "method": "upi",
                    "consent": true,
                    "expiresAt": "%s"
                }
                """.formatted(Instant.now().plusSeconds(3600).toString());

        when(events.findByEventType("method.created")).thenReturn(List.of(
                new WebhookEvent("simtok_1", "method.created", missingOwnerJson),
                new WebhookEvent("simtok_2", "method.created", otherOwnerJson)));

        List<Map<String, Object>> result = service.list(student);
        assertTrue(result.isEmpty());
    }

    @Test
    void revoke_rejectsMalformedTokenFormat() {
        assertThrows(IllegalArgumentException.class, () -> service.revoke(student, "invalid-token"));
    }
}

