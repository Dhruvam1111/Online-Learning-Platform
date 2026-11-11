package com.olp.service;

import com.olp.entity.User;
import com.olp.repository.PaymentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Authorization helper used by payment endpoint method-security expressions.
 * Kept with the payment core so this branch can run independently.
 */
@Component("paymentAccess")
public class PaymentAccess {

    private final PaymentRepository payments;

    public PaymentAccess(PaymentRepository payments) {
        this.payments = payments;
    }

    public boolean owns(Authentication authentication, String orderId) {
        if (authentication == null || orderId == null || orderId.isBlank()) {
            return false;
        }

        return payments.findByOrderId(orderId).map(payment -> {
            User student = payment.getStudent();
            if (student == null) {
                return false;
            }

            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_admin".equalsIgnoreCase(authority.getAuthority())
                            || "admin".equalsIgnoreCase(authority.getAuthority()));
            if (isAdmin) {
                return true;
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof User user) {
                if ("admin".equalsIgnoreCase(user.getRole())) {
                    return true;
                }
                if (user.getId() != null && user.getId().equals(student.getId())) {
                    return true;
                }
                if (user.getEmail() != null && student.getEmail() != null
                        && user.getEmail().equalsIgnoreCase(student.getEmail())) {
                    return true;
                }
            }

            String name = authentication.getName();
            return name != null && student.getEmail() != null
                    && name.equalsIgnoreCase(student.getEmail());
        }).orElse(false);
    }
}
