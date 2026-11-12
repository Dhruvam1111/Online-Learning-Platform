package com.olp.service;

import com.olp.entity.Payment;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/** Shared policy for synchronous and asynchronous simulated refunds. */
public final class RefundPolicy {
    private RefundPolicy() {}
    public static void withinWindow(Payment payment) {
        if (payment.getCreatedAt() == null || payment.getCreatedAt().plusDays(30).isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("The 30-day refund window has expired. Submit an exceptional refund request for admin review.");
    }
    public static BigDecimal amount(Payment p, BigDecimal requested) {
        BigDecimal total = p.getAmount().add(wallet(p));
        BigDecimal value = requested == null ? total : requested;
        if (value.signum() <= 0 || value.stripTrailingZeros().scale() > 2 || value.compareTo(total) > 0)
            throw new IllegalArgumentException("Refund amount must be positive, in whole paisa, and no greater than the amount paid.");
        return value;
    }
    public static BigDecimal wallet(Payment p) { return p.getWalletAmountUsed() == null ? BigDecimal.ZERO : p.getWalletAmountUsed(); }
    public static BigDecimal walletShare(Payment p, BigDecimal refund) {
        BigDecimal total = p.getAmount().add(wallet(p));
        return refund.multiply(wallet(p)).divide(total, 2, RoundingMode.HALF_UP).min(wallet(p));
    }
    public static boolean hasActiveRefund(com.olp.repository.RefundRepository refunds,Integer paymentId){
        if(!refunds.existsByPaymentId(paymentId))return false;
        var rows=refunds.findByPaymentIdOrderByCreatedAtDesc(paymentId);
        return rows.isEmpty() || rows.stream().anyMatch(r -> !"failed".equals(r.getStatus()));
    }
    public static boolean blocksPayout(Payment payment){
        if("pending".equals(payment.getRefundStatus()))return true;
        if(!"refunded".equals(payment.getRefundStatus()))return false;
        return payment.getRefundAmount()==null || payment.getRefundAmount().signum()<=0 || payment.getRefundAmount().compareTo(payment.getAmount().add(wallet(payment)))>=0;
    }
    public static String reason(String value) {
        if (value == null || value.isBlank() || value.length() > 1000) throw new IllegalArgumentException("A reason of 1–1000 characters is required.");
        return value.trim();
    }
}
