package com.olp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Consolidated DTOs for payment simulation, bank payouts, subscriptions,
 * wallet top-up, financial governance, currency rates, and refund queue views.
 */
public final class PaymentSimulationDtos {

    private PaymentSimulationDtos() {}

    public record BankAccountRequest(
            String accountNumber,
            String ifscCode,
            String bankName,
            String holderName,
            boolean consent
    ) {}

    public record BankAccountDecisionRequest(
            Integer instructorId,
            String accountId,
            boolean approved,
            boolean identityChecked,
            String reason
    ) {}

    public record CurrencyRateUpdateRequest(
            String currency,
            BigDecimal rate
    ) {}

    public record FinancialGovernancePolicyRequest(
            Integer financialRetentionDays,
            Integer adminAuditRetentionDays,
            BigDecimal withholdingRate,
            BigDecimal commissionTaxRate
    ) {}

    public record PolicyAcceptanceRequest(
            String policyVersion
    ) {}

    public record PaymentPolicyUpdateRequest(
            BigDecimal commissionRate,
            BigDecimal taxRate,
            BigDecimal defaultPrice
    ) {}

    public record CoursePaymentPolicyUpdateRequest(
            BigDecimal price,
            BigDecimal taxRate
    ) {}

    public record SavedMethodRequest(
            String method,
            boolean consent,
            String cardBrand,
            String last4,
            String expiryMonth,
            String expiryYear,
            String cardholderName,
            String upiId,
            String bankName,
            String walletProvider,
            String displayName
    ) {
        public SavedMethodRequest(String method, boolean consent) {
            this(method, consent, null, null, null, null, null, null, null, null, null);
        }
    }

    public record TokenPaymentRequest(
            String orderId
    ) {}

    public record SubscriptionCreateRequest(
            Integer courseId,
            String token,
            boolean consent,
            Integer durationMonths,
            String planName,
            BigDecimal monthlyPrice,
            BigDecimal totalAmount
    ) {
        public SubscriptionCreateRequest(Integer courseId, String token, boolean consent) {
            this(courseId, token, consent, 1, "Monthly Plan", null, null);
        }
    }

    public record SubscriptionApprovalRequest(
            boolean approval,
            BigDecimal expectedAmount,
            String requestId
    ) {}

    public record WalletTopUpRequest(
            BigDecimal amount,
            String requestId,
            String outcome,
            String method,
            String token
    ) {}

    public record RefundQueueResponse(
            List<Map<String, Object>> content,
            int totalPages,
            long pendingCount,
            LocalDateTime oldestPendingAt
    ) {}
}
