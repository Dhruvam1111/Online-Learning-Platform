package com.olp.service;

import com.olp.dto.RefundRequest;
import com.olp.dto.RefundResponse;
import com.olp.entity.Course;
import com.olp.entity.Payment;
import com.olp.entity.User;
import com.olp.repository.PaymentRepository;
import com.olp.repository.RefundRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExceptionalRefundServiceTest {

    @Mock
    private PaymentRepository payments;
    @Mock
    private RefundRepository refunds;
    @Mock
    private PaymentAuditService audit;
    @Mock
    private RefundService settlement;

    @InjectMocks
    private ExceptionalRefundService service;

    private User student(Integer id) {
        User u = new User("Student", "s" + id + "@olp.test", "hash", "student");
        u.setId(id);
        return u;
    }

    private User admin() {
        User u = new User("Admin", "admin@olp.test", "hash", "admin");
        u.setId(1);
        return u;
    }

    private Payment payment(User owner, LocalDateTime createdAt) {
        Course c = new Course();
        c.setId(5);
        c.setTitle("Algorithms");
        Payment p = new Payment();
        p.setId(22);
        p.setOrderId("ORD-22");
        p.setPaymentId("PAY-22");
        p.setStudent(owner);
        p.setCourse(c);
        p.setAmount(new BigDecimal("400.00"));
        p.setStatus("success");
        p.setCreatedAt(createdAt);
        return p;
    }

    @Test
    void submit_routesWithinWindowDirectlyAndLatePurchasesToPendingReview() {
        User s = student(10);
        Payment recent = payment(s, LocalDateTime.now().minusDays(5));
        RefundRequest req = new RefundRequest("PAY-22", null, "Need refund");

        when(payments.findByPaymentIdForUpdate("PAY-22")).thenReturn(Optional.of(recent));
        RefundResponse immediate = new RefundResponse("RFD-1", "PAY-22", "ORD-22", 5, "Algorithms",
                new BigDecimal("400.00"), "processed", "Need refund", true, LocalDateTime.now());
        when(settlement.processRefund(s, req)).thenReturn(immediate);

        RefundResponse directResult = service.submit(s, req);
        assertEquals("processed", directResult.getStatus());
        verify(settlement).processRefund(s, req);

        // Late purchase (>30 days) creates pending exceptional review request
        Payment old = payment(s, LocalDateTime.now().minusDays(45));
        when(payments.findByPaymentIdForUpdate("ORD-22")).thenReturn(Optional.empty());
        when(payments.findByOrderIdForUpdate("ORD-22")).thenReturn(Optional.of(old));
        when(refunds.existsByPaymentId(22)).thenReturn(false);
        when(audit.exists("exception-ORD-22")).thenReturn(false);
        when(audit.get("exception-ORD-22")).thenReturn(Map.of("eventId", "exception-ORD-22"));

        RefundResponse pendingResult = service.submit(s, new RefundRequest("ORD-22", new BigDecimal("250.00"), "Hospitalized"));
        assertEquals("pending_review", pendingResult.getStatus());
        assertEquals(new BigDecimal("250.00"), pendingResult.getAmount());
        assertFalse(pendingResult.isEnrollmentRevoked());
    }

    @Test
    void request_preventsDuplicateExceptionsNonOwnersAndUnrefundableOrders() {
        User owner = student(10);
        User other = student(11);
        Payment p = payment(owner, LocalDateTime.now().minusDays(40));

        assertThrows(AccessDeniedException.class,
                () -> service.request(null, new RefundRequest("ORD-22", null, "Reason")));

        when(payments.findByOrderIdForUpdate("ORD-22")).thenReturn(Optional.of(p));
        assertThrows(AccessDeniedException.class,
                () -> service.request(other, new RefundRequest("ORD-22", null, "Reason")));

        p.setStatus("failed");
        assertThrows(IllegalStateException.class,
                () -> service.request(owner, new RefundRequest("ORD-22", null, "Reason")));

        p.setStatus("success");
        when(refunds.existsByPaymentId(22)).thenReturn(false);
        when(audit.exists("exception-ORD-22")).thenReturn(true);
        assertThrows(IllegalStateException.class,
                () -> service.request(owner, new RefundRequest("ORD-22", null, "Reason")));
    }

    @Test
    void queueAndList_validateParametersAndFilterVisibility() {
        User s = student(10);
        User a = admin();

        assertThrows(AccessDeniedException.class, () -> service.queue(s, "pending", "", 0, 10));
        assertThrows(IllegalArgumentException.class, () -> service.queue(a, "unknown", "", 0, 10));
        assertThrows(IllegalArgumentException.class, () -> service.queue(a, "pending", "", -1, 10));
        assertThrows(IllegalArgumentException.class, () -> service.queue(a, "pending", "", 0, 101));

        service.queue(a, "pending", "  alice  ", 0, 20);
        verify(audit).refundQueue("pending", "alice", 0, 20);

        Map<String, Object> ownRow = new HashMap<>(Map.of("eventId", "exception-ORD-1", "requesterId", 10));
        Map<String, Object> otherRow = new HashMap<>(Map.of("eventId", "exception-ORD-2", "requesterId", 99));
        when(audit.list("refund.exception.request")).thenReturn(List.of(ownRow, otherRow));
        when(audit.exists("decision-exception-ORD-1")).thenReturn(true);
        when(audit.get("decision-exception-ORD-1")).thenReturn(Map.of("decision", "approved"));
        when(audit.exists("decision-exception-ORD-2")).thenReturn(false);

        List<Map<String, Object>> studentRows = service.list(s);
        assertEquals(1, studentRows.size());
        assertEquals("approved", studentRows.get(0).get("status"));

        List<Map<String, Object>> adminRows = service.list(a);
        assertEquals(2, adminRows.size());
        assertEquals("pending", adminRows.get(1).get("status"));
    }

    @Test
    void decide_enforcesAdminDecisionBoundsAndSettlesApprovedExceptions() {
        User s = student(10);
        User a = admin();
        Payment p = payment(s, LocalDateTime.now().minusDays(40));

        assertThrows(AccessDeniedException.class,
                () -> service.decide(s, "exception-ORD-22", "approved", "Ok"));
        assertThrows(IllegalArgumentException.class,
                () -> service.decide(a, "exception-ORD-22", "maybe", "Reason"));
        assertThrows(IllegalArgumentException.class,
                () -> service.decide(a, "bad-id", "approved", "Reason"));

        when(audit.get("exception-ORD-22")).thenReturn(Map.of("orderId", "ORD-22", "amount", "400.00"));
        when(payments.findByOrderIdForUpdate("ORD-22")).thenReturn(Optional.of(p));
        when(audit.exists("decision-exception-ORD-22")).thenReturn(false);

        // Approved amount > requested must be rejected
        assertThrows(IllegalArgumentException.class,
                () -> service.decide(a, "exception-ORD-22", "approved", "Partial", new BigDecimal("450.00")));
        assertThrows(IllegalArgumentException.class,
                () -> service.decide(a, "exception-ORD-22", "approved", "Partial", BigDecimal.ZERO));

        // Partial approval settles exact approved amount
        when(audit.get("decision-exception-ORD-22")).thenReturn(Map.of("decision", "approved"));
        service.decide(a, "exception-ORD-22", "approved", "Partial refund granted", new BigDecimal("150.00"));

        verify(settlement).processApprovedException(eq(a),
                argThat(r -> "ORD-22".equals(r.getPaymentId()) && new BigDecimal("150.00").equals(r.getAmount())));
        verify(audit).record(eq("decision-exception-ORD-22"), eq("refund.exception.decision"), any());

        // Rejection records zero settled amount without calling settlement
        when(audit.get("exception-ORD-23")).thenReturn(Map.of("orderId", "ORD-22", "amount", "400.00"));
        when(audit.exists("decision-exception-ORD-23")).thenReturn(false);
        when(audit.get("decision-exception-ORD-23"))
                .thenReturn(Map.of("decision", "rejected", "approvedAmount", BigDecimal.ZERO));
        Map<String, Object> rejected = service.decide(a, "exception-ORD-23", "rejected", "Course already completed");
        assertEquals("rejected", rejected.get("decision"));
        verify(settlement, times(1)).processApprovedException(any(), any());

        // Double decision throws IllegalStateException
        when(audit.exists("decision-exception-ORD-22")).thenReturn(true);
        assertThrows(IllegalStateException.class,
                () -> service.decide(a, "exception-ORD-22", "rejected", "Already decided"));
    }
}
