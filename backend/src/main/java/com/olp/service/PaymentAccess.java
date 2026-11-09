package com.olp.service;

import com.olp.entity.User;
import com.olp.repository.PaymentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("paymentAccess")
public class PaymentAccess {
    private final PaymentRepository payments;

    public PaymentAccess(PaymentRepository payments) {
        this.payments = payments;
    }

    public boolean owns(Authentication auth, String orderId) {
        if (auth == null || orderId == null) return false;
        return payments.findByOrderId(orderId).map(p -> {
            if (p.getStudent() == null) return false;
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_admin") || a.getAuthority().equalsIgnoreCase("admin"));
            if (isAdmin) return true;

            Object principal = auth.getPrincipal();
            if (principal instanceof User user) {
                if ("admin".equalsIgnoreCase(user.getRole())) return true;
                if (user.getId() != null && user.getId().equals(p.getStudent().getId())) return true;
                if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(p.getStudent().getEmail())) return true;
            }
            String name = auth.getName();
            if (name != null && p.getStudent().getEmail() != null && name.equalsIgnoreCase(p.getStudent().getEmail())) {
                return true;
            }
            return false;
        }).orElse(false);
    }
}
