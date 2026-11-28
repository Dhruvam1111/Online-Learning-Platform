package com.olp.controller;

import com.olp.dto.PaymentSimulationDtos.RefundQueueResponse;
import com.olp.dto.RefundRequest;
import com.olp.entity.User;
import com.olp.service.ExceptionalRefundService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/payments/refunds/exceptions")
public class ExceptionalRefundController {
    private final ExceptionalRefundService service;

    public ExceptionalRefundController(ExceptionalRefundService service) {
        this.service = service;
    }

    @PostMapping
    public Map<String, Object> request(@AuthenticationPrincipal User user, @Valid @RequestBody RefundRequest request) {
        return service.request(user, request);
    }

    @GetMapping
    public List<Map<String, Object>> list(@AuthenticationPrincipal User user) {
        return service.list(user);
    }

    @GetMapping("/queue")
    public RefundQueueResponse queue(@AuthenticationPrincipal User user,
                                     @RequestParam(defaultValue = "pending") String status,
                                     @RequestParam(defaultValue = "") String search,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return service.queue(user, status, search, page, size);
    }

    @PostMapping("/{id}/decision")
    public Map<String, Object> decide(@AuthenticationPrincipal User user,
                                      @PathVariable String id,
                                      @RequestBody(required = false) Map<String, String> body) {
        if (body == null) {
            throw new IllegalArgumentException("Decision request body is required.");
        }
        String rawAmount = body.get("approvedAmount");
        BigDecimal approvedAmount = null;
        if (rawAmount != null && !rawAmount.isBlank()) {
            try {
                approvedAmount = new BigDecimal(rawAmount.trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Approved amount must be a valid decimal number.");
            }
        }
        return service.decide(user, id, body.get("decision"), body.get("reason"), approvedAmount);
    }
}
