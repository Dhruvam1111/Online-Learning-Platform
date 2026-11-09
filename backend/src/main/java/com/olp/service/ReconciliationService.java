package com.olp.service;

import com.olp.dto.ReconciliationReportResponse;
import com.olp.entity.Payment;
import com.olp.entity.Refund;
import com.olp.entity.User;
import com.olp.repository.InstructorEarningRepository;
import com.olp.repository.PaymentRepository;
import com.olp.repository.RefundRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Enterprise Financial Ledger & Gateway Reconciliation Service (FR-PG12, FR-A23).
 * Owned by Dhruvam (Ledger & Reconciliation Engine).
 * Audits internal accounting records against gateway transactions.
 */
@Service
public class ReconciliationService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final InstructorEarningRepository earningRepository;

    public ReconciliationService(PaymentRepository paymentRepository,
                                 RefundRepository refundRepository,
                                 InstructorEarningRepository earningRepository) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.earningRepository = earningRepository;
    }

    @Transactional(readOnly = true)
    public ReconciliationReportResponse generateReconciliationReport(User admin) {
        if (!"admin".equalsIgnoreCase(admin.getRole())) {
            throw new AccessDeniedException("Access denied: Administrative audit privileges required.");
        }

        List<Payment> allPayments = paymentRepository.findAll();
        List<Refund> allRefunds = refundRepository.findAll();

        long totalOrders = allPayments.size();
        long successCount = 0;
        long failedCount = 0;
        long refundedCount = 0;

        BigDecimal grossVolume = BigDecimal.ZERO;
        BigDecimal totalRefunded = BigDecimal.ZERO;
        BigDecimal netPlatformRevenue = BigDecimal.ZERO;
        BigDecimal totalInstructorLiabilities = BigDecimal.ZERO;
        BigDecimal totalDiscounts = BigDecimal.ZERO;
        BigDecimal totalWalletCredits = BigDecimal.ZERO;

        List<String> discrepancies = new ArrayList<>();

        for (Payment payment : allPayments) {
            if ("success".equalsIgnoreCase(payment.getStatus())) {
                successCount++;
                grossVolume = grossVolume.add(payment.getAmount());
                if (payment.getPlatformFee() != null) {
                    netPlatformRevenue = netPlatformRevenue.add(payment.getPlatformFee());
                }
                if (payment.getInstructorEarning() != null) {
                    totalInstructorLiabilities = totalInstructorLiabilities.add(payment.getInstructorEarning());
                }
                if (payment.getDiscountAmount() != null) {
                    totalDiscounts = totalDiscounts.add(payment.getDiscountAmount());
                }
                if (payment.getWalletAmountUsed() != null) {
                    totalWalletCredits = totalWalletCredits.add(payment.getWalletAmountUsed());
                }

                // Discrepancy Check 1: Missing gateway payment token on successful order
                if (payment.getPaymentId() == null || payment.getPaymentId().isBlank()) {
                    discrepancies.add("Order " + payment.getOrderId() + " marked success without gateway payment ID token");
                }

                // Discrepancy Check 2: High risk score flagged
                if (payment.getRiskScore() != null && payment.getRiskScore() >= 60) {
                    discrepancies.add("Order " + payment.getOrderId() + " has high fraud risk score (" + payment.getRiskScore() + "/100)");
                }
            } else if ("failed".equalsIgnoreCase(payment.getStatus())) {
                failedCount++;
            }

            if ("refunded".equalsIgnoreCase(payment.getRefundStatus())) {
                refundedCount++;
            }
        }

        for (Refund refund : allRefunds) {
            if ("processed".equalsIgnoreCase(refund.getStatus())) {
                totalRefunded = totalRefunded.add(refund.getAmount());
            }
        }

        String auditStatus = discrepancies.isEmpty() ? "BALANCED" : "ATTENTION_REQUIRED";

        return new ReconciliationReportResponse(
                totalOrders,
                successCount,
                failedCount,
                refundedCount,
                grossVolume,
                totalRefunded,
                netPlatformRevenue,
                totalInstructorLiabilities,
                totalDiscounts,
                totalWalletCredits,
                discrepancies.size(),
                auditStatus,
                LocalDateTime.now(),
                discrepancies
        );
    }
}
