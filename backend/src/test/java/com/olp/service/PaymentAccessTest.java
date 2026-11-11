package com.olp.service;

import com.olp.entity.Payment;
import com.olp.entity.User;
import com.olp.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentAccessTest {

    private PaymentRepository payments;
    private PaymentAccess paymentAccess;
    private User owner;
    private Payment payment;

    @BeforeEach
    void setUp() {
        payments = mock(PaymentRepository.class);
        paymentAccess = new PaymentAccess(payments);
        owner = new User("Alice", "alice@example.com", "hash", "student");
        owner.setId(12);
        payment = new Payment("ORDER-1", owner, null, new BigDecimal("25.00"), "INR");
        when(payments.findByOrderId("ORDER-1")).thenReturn(Optional.of(payment));
    }

    @Test
    void ownerCanAccessTheirOrder() {
        assertTrue(paymentAccess.owns(authenticate(owner, "ROLE_student"), "ORDER-1"));
    }

    @Test
    void differentStudentCannotAccessOrder() {
        User other = new User("Bob", "bob@example.com", "hash", "student");
        other.setId(13);

        assertFalse(paymentAccess.owns(authenticate(other, "ROLE_student"), "ORDER-1"));
    }

    @Test
    void administratorCanAccessAnyOrder() {
        User admin = new User("Admin", "admin@example.com", "hash", "admin");

        assertTrue(paymentAccess.owns(authenticate(admin, "ROLE_admin"), "ORDER-1"));
    }

    @Test
    void absentAuthenticationOrderOrPaymentIsDenied() {
        assertFalse(paymentAccess.owns(null, "ORDER-1"));
        assertFalse(paymentAccess.owns(authenticate(owner, "ROLE_student"), null));
        assertFalse(paymentAccess.owns(authenticate(owner, "ROLE_student"), " "));
        assertFalse(paymentAccess.owns(authenticate(owner, "ROLE_student"), "MISSING"));
        when(payments.findByOrderId("NO-STUDENT")).thenReturn(Optional.of(
                new Payment("NO-STUDENT", null, null, BigDecimal.ONE, "INR")));
        assertFalse(paymentAccess.owns(authenticate(owner, "ROLE_student"), "NO-STUDENT"));
    }

    private UsernamePasswordAuthenticationToken authenticate(User user, String authority) {
        return new UsernamePasswordAuthenticationToken(
                user, "ignored", List.of(new SimpleGrantedAuthority(authority)));
    }
}
