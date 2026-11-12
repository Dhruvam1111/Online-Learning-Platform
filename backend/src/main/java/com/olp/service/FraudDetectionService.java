package com.olp.service;

import com.olp.entity.Payment;
import com.olp.entity.User;
import com.olp.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Enterprise Fraud Detection & Risk Scoring Engine (FR-PG11).
 * Owned by Shubh (Fraud & Security Engine).
 * Analyzes transaction velocity, historical failure rates, and order anomalies.
 */
@Service
public class FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionService.class);
    private final PaymentRepository paymentRepository;

    public FraudDetectionService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public static class RiskAssessment {
        private final int score; // 0 - 100
        private final String level; // LOW, MEDIUM, HIGH
        private final List<String> flaggedFactors;

        public RiskAssessment(int score, String level, List<String> flaggedFactors) {
            this.score = score;
            this.level = level;
            this.flaggedFactors = flaggedFactors;
        }

        public int getScore() { return score; }
        public String getLevel() { return level; }
        public List<String> getFlaggedFactors() { return flaggedFactors; }
    }

    /**
     * Evaluates real-time risk score for an incoming order request.
     */
    public RiskAssessment evaluateRisk(User student, BigDecimal amount) {
        int score = 5; // Base low baseline score
        List<String> factors = new java.util.ArrayList<>();

        LocalDateTime fiveMinsAgo = LocalDateTime.now().minusMinutes(5);
        List<Payment> recentPayments = paymentRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());

        // 1. Velocity Check: high checkout attempts in short window
        long recentCount = recentPayments.stream()
                .filter(p -> p.getCreatedAt().isAfter(fiveMinsAgo))
                .count();

        if (recentCount >= 3) {
            score += 35;
            factors.add("High checkout velocity (" + recentCount + " attempts in past 5 mins)");
        }

        // 2. Failure Rate Check: multiple recent failed transactions
        long recentFailures = recentPayments.stream()
                .limit(5)
                .filter(p -> "failed".equalsIgnoreCase(p.getStatus()))
                .count();

        if (recentFailures >= 2) {
            score += 30;
            factors.add("Repeated payment authorization failures detected (" + recentFailures + " failures)");
        }

        // 3. High Transaction Value Check (> ₹5,000)
        if (amount != null && amount.compareTo(BigDecimal.valueOf(5000)) > 0) {
            score += 20;
            factors.add("High transaction value threshold exceeded (> ₹5,000)");
        }

        // 4. New Account Flag (< 24 hours old)
        if (student.getCreatedAt() != null && student.getCreatedAt().isAfter(LocalDateTime.now().minusHours(24))) {
            score += 10;
            factors.add("Account created within past 24 hours");
        }

        score = Math.min(score, 100);
        String level = (score >= 65) ? "HIGH" : (score >= 30) ? "MEDIUM" : "LOW";

        log.info("[Fraud Engine] Assessed Student ID {} - Score: {} ({}) - Factors: {}",
                student.getId(), score, level, factors);

        return new RiskAssessment(score, level, factors);
    }
}
