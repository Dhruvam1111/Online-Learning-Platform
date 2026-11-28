package com.olp.controller;

import com.olp.dto.PaymentSimulationDtos.CoursePaymentPolicyUpdateRequest;
import com.olp.dto.PaymentSimulationDtos.PaymentPolicyUpdateRequest;
import com.olp.entity.CourseStatus;
import com.olp.entity.User;
import com.olp.repository.CourseRepository;
import com.olp.service.PaymentPolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/policy")
public class PaymentPolicyController {
    private final PaymentPolicyService policies;
    private final CourseRepository courses;

    public PaymentPolicyController(PaymentPolicyService policies) {
        this(policies, null);
    }

    @Autowired
    public PaymentPolicyController(PaymentPolicyService policies,
                                   @Autowired(required = false) CourseRepository courses) {
        this.policies = policies;
        this.courses = courses;
    }

    @PutMapping("/courses/{id}")
    public Map<String, Object> course(@AuthenticationPrincipal User user,
                                      @PathVariable Integer id,
                                      @RequestBody CoursePaymentPolicyUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Course policy update request body is required.");
        }
        if (courses == null || !courses.existsById(id)) {
            throw new IllegalArgumentException("Course not found.");
        }
        return policies.updateCourse(user, id, request.price(), request.taxRate());
    }

    @GetMapping("/courses/{id}")
    public Map<String, Object> quote(@PathVariable Integer id) {
        if (courses == null) {
            throw new IllegalArgumentException("Course not found.");
        }
        var course = courses.findById(id).orElseThrow(() -> new IllegalArgumentException("Course not found."));
        return Map.of("amount", policies.price(course), "currency", "INR", "taxInclusive", true);
    }

    @GetMapping("/catalog")
    public List<Map<String, Object>> catalog() {
        if (courses == null) {
            return List.of();
        }
        return courses.findAll().stream()
                .filter(c -> c.getStatus() == CourseStatus.PUBLISHED)
                .map(c -> Map.<String, Object>of(
                        "courseId", c.getId(),
                        "amount", policies.price(c),
                        "currency", "INR",
                        "taxInclusive", true))
                .toList();
    }

    @GetMapping
    public Map<String, Object> current() {
        return policies.current();
    }

    @PutMapping
    public Map<String, Object> update(@AuthenticationPrincipal User user,
                                      @RequestBody PaymentPolicyUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Payment policy update request body is required.");
        }
        return policies.update(user, request.commissionRate(), request.taxRate(), request.defaultPrice());
    }
}
