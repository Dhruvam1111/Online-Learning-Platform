package com.olp.service;

import com.olp.entity.Payment;
import com.olp.entity.User;
import com.olp.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentAccessTest {

    @Mock
    private PaymentRepository payments;

    @InjectMocks
    private PaymentAccess paymentAccess;

    private User student(Integer id, String email) {
        User u = new User("Student", email, "hash", "student");
        u.setId(id);
        return u;
    }

    private Payment paymentWithStudent(User student) {
        Payment payment = new Payment();
        payment.setOrderId("order-123");
        payment.setStudent(student);
        return payment;
    }

    @Test
    void owns_rejectsNullOrBlankArgumentsAndMissingPayments() {
        Authentication auth = new UsernamePasswordAuthenticationToken("a@olp.test", null, List.of());

        assertFalse(paymentAccess.owns(null, "order-123"));
        assertFalse(paymentAccess.owns(auth, null));
        assertFalse(paymentAccess.owns(auth, "   "));

        when(payments.findByOrderId("order-missing")).thenReturn(Optional.empty());
        assertFalse(paymentAccess.owns(auth, "order-missing"));

        Payment orphan = new Payment();
        orphan.setOrderId("order-orphan");
        when(payments.findByOrderId("order-orphan")).thenReturn(Optional.of(orphan));
        assertFalse(paymentAccess.owns(auth, "order-orphan"));
    }

    @Test
    void owns_grantsAdminAuthorityAndAdminUserRole() {
        User owner = student(10, "owner@olp.test");
        when(payments.findByOrderId("order-123")).thenReturn(Optional.of(paymentWithStudent(owner)));

        Authentication adminAuthority = new UsernamePasswordAuthenticationToken(
                "admin@olp.test", null, List.of(new SimpleGrantedAuthority("ROLE_admin"))
        );
        assertTrue(paymentAccess.owns(adminAuthority, "order-123"));

        User adminEntity = new User("Admin", "admin@olp.test", "hash", "admin");
        adminEntity.setId(99);
        Authentication adminRoleUser = new UsernamePasswordAuthenticationToken(adminEntity, null, List.of());
        assertTrue(paymentAccess.owns(adminRoleUser, "order-123"));
    }

    @Test
    void owns_grantsOwnerByIdEmailOrAuthenticationNameAndDeniesOtherStudent() {
        User owner = student(10, "Owner@olp.test");
        when(payments.findByOrderId("order-123")).thenReturn(Optional.of(paymentWithStudent(owner)));

        User sameIdDifferentEmail = student(10, "alias@olp.test");
        assertTrue(paymentAccess.owns(
                new UsernamePasswordAuthenticationToken(sameIdDifferentEmail, null, List.of()), "order-123"));

        User sameEmailDifferentId = student(25, "owner@olp.test");
        assertTrue(paymentAccess.owns(
                new UsernamePasswordAuthenticationToken(sameEmailDifferentId, null, List.of()), "order-123"));

        Authentication nameMatch = new UsernamePasswordAuthenticationToken("owner@olp.test", null, List.of());
        assertTrue(paymentAccess.owns(nameMatch, "order-123"));

        User intruder = student(77, "intruder@olp.test");
        assertFalse(paymentAccess.owns(
                new UsernamePasswordAuthenticationToken(intruder, null, List.of(new SimpleGrantedAuthority("ROLE_student"))),
                "order-123"));
    }
}
