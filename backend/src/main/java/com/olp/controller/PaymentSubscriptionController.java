package com.olp.controller;

import com.olp.dto.PaymentSimulationDtos.SubscriptionApprovalRequest;
import com.olp.dto.PaymentSimulationDtos.SubscriptionCreateRequest;
import com.olp.entity.User;
import com.olp.service.PaymentSubscriptionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/payments/subscriptions")
public class PaymentSubscriptionController {
    private final PaymentSubscriptionService service;

    public PaymentSubscriptionController(PaymentSubscriptionService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> list(@AuthenticationPrincipal User user) {
        return service.list(user);
    }

    @PostMapping
    public Map<String, Object> create(@AuthenticationPrincipal User user,
                                      @RequestBody SubscriptionCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Subscription creation request body is required.");
        }
        return service.create(user, request.courseId(), request.token(), request.consent(),
                request.durationMonths(), request.planName(), request.monthlyPrice(), request.totalAmount());
    }

    @DeleteMapping("/{id}")
    public Map<String, String> cancel(@AuthenticationPrincipal User user, @PathVariable String id) {
        service.cancel(user, id);
        return Map.of("status", "cancelled");
    }

    @GetMapping("/{id}/quote")
    public Map<String, Object> quote(@AuthenticationPrincipal User user, @PathVariable String id) {
        return service.quote(user, id);
    }

    @PostMapping("/{id}/approve")
    public Map<String, Object> approve(@AuthenticationPrincipal User user,
                                       @PathVariable String id,
                                       @RequestBody SubscriptionApprovalRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Subscription approval request body is required.");
        }
        return service.approve(user, id, request.approval(), request.expectedAmount(), request.requestId());
    }
}
