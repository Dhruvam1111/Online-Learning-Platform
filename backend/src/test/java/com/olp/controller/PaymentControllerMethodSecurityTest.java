package com.olp.controller;

import com.olp.entity.Payment;
import com.olp.entity.User;
import com.olp.repository.PaymentRepository;
import com.olp.service.PaymentAccess;
import com.olp.service.PaymentAuditService;
import com.olp.service.PaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(PaymentControllerMethodSecurityTest.TestConfig.class)
class PaymentControllerMethodSecurityTest {

    @Autowired private PaymentController controller;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentService paymentService;

    @BeforeEach
    void resetMocks() {
        reset(paymentRepository, paymentService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ownerPassesPaymentControllerMethodSecurity() {
        User owner = new User("Alice", "alice@example.com", "hash", "student");
        owner.setId(12);
        Payment payment = new Payment("ORDER-1", owner, null, new BigDecimal("25.00"), "INR");
        when(paymentRepository.findByOrderId("ORDER-1")).thenReturn(Optional.of(payment));
        authenticate(owner, "ROLE_student");

        assertNotNull(controller.confirmPayment("ORDER-1", null, null));
        verify(paymentService).confirmPayment("ORDER-1");
    }

    @Test
    void anotherStudentIsDeniedBeforePaymentServiceRuns() {
        User owner = new User("Alice", "alice@example.com", "hash", "student");
        owner.setId(12);
        User other = new User("Bob", "bob@example.com", "hash", "student");
        other.setId(13);
        Payment payment = new Payment("ORDER-1", owner, null, new BigDecimal("25.00"), "INR");
        when(paymentRepository.findByOrderId("ORDER-1")).thenReturn(Optional.of(payment));
        authenticate(other, "ROLE_student");

        assertThrows(AccessDeniedException.class,
                () -> controller.confirmPayment("ORDER-1", null, null));
        verifyNoInteractions(paymentService);
    }

    private void authenticate(User user, String authority) {
        var authentication = new UsernamePasswordAuthenticationToken(
                user, "ignored", List.of(new SimpleGrantedAuthority(authority)));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    @Configuration
    @EnableMethodSecurity
    @Import(PaymentAccess.class)
    static class TestConfig {
        @Bean
        PaymentRepository paymentRepository() {
            return mock(PaymentRepository.class);
        }

        @Bean
        PaymentService paymentService() {
            return mock(PaymentService.class);
        }

        @Bean
        PaymentAuditService paymentAuditService() {
            return mock(PaymentAuditService.class);
        }

        @Bean
        PaymentController paymentController(PaymentService paymentService) {
            return new PaymentController(paymentService);
        }
    }
}
