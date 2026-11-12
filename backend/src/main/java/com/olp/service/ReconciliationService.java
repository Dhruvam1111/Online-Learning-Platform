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

    @org.springframework.beans.factory.annotation.Autowired private PaymentAuditService audit;
    @org.springframework.beans.factory.annotation.Autowired(required=false) private PaymentGateway gateway;
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
        if (admin == null || !"admin".equalsIgnoreCase(admin.getRole())) {
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
                grossVolume = grossVolume.add(payment.getAmount().add(RefundPolicy.wallet(payment)));
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

        if (audit != null) {
            java.util.Map<String,Payment> byOrder = new java.util.HashMap<>();
            allPayments.forEach(p -> byOrder.put(p.getOrderId(),p));
            java.util.Map<String,java.util.Map<String,Object>> settlementQuotes=new java.util.HashMap<>();
            for(var quote:audit.list("order.settlement"))if(quote.get("orderId")!=null)settlementQuotes.put(quote.get("orderId").toString(),quote);
            java.util.Set<String> reversed=new java.util.HashSet<>();
            for(var row:audit.list("gateway.reversal"))reversed.add(row.get("orderId").toString());
            java.util.Set<String> captured = new java.util.HashSet<>();
            for (var entry : (gateway==null?audit.list("gateway.capture"):gateway.captureReports())) {
                String order=entry.get("orderId").toString(); captured.add(order); Payment p=byOrder.get(order);
                var quote=settlementQuotes.get(order);
                if(quote!=null && (!quote.get("providerCurrency").equals(entry.get("providerCurrency")) || entry.get("providerAmount")==null || new BigDecimal(quote.get("providerAmount").toString()).compareTo(new BigDecimal(entry.get("providerAmount").toString()))!=0))discrepancies.add("Provider-currency capture mismatch: "+order);
                if (p == null) discrepancies.add("Gateway capture has no internal order: "+order);
                else if ((!java.util.Set.of("success","disputed","charged_back").contains(p.getStatus()) && !reversed.contains(order)) || p.getAmount().compareTo(new BigDecimal(entry.get("amount").toString())) != 0 || !p.getCurrency().equals(entry.get("currency"))) discrepancies.add("Gateway capture amount/status/currency mismatch: "+order);
            }
            for (Payment p:allPayments) if ("success".equals(p.getStatus()) && !captured.contains(p.getOrderId())) discrepancies.add("Missing simulated gateway capture: "+p.getOrderId());
            java.util.Map<String,Refund> byRefund = new java.util.HashMap<>();
            allRefunds.stream().filter(r -> "processed".equals(r.getStatus())).forEach(r -> byRefund.put(r.getRazorpayRefundId(),r));
            for (var entry : (gateway==null?audit.list("gateway.refund"):gateway.refundReports())) {
                Refund r=byRefund.remove(entry.get("refundId").toString());
                if (r == null) discrepancies.add("Gateway refund has no processed internal refund: "+entry.get("refundId"));
                else if (!r.getPayment().getOrderId().equals(entry.get("orderId")) || r.getAmount().subtract(RefundPolicy.walletShare(r.getPayment(),r.getAmount())).compareTo(new BigDecimal(entry.get("amount").toString())) != 0) discrepancies.add("Gateway refund amount/order mismatch: "+entry.get("refundId"));
            }
            byRefund.keySet().forEach(id -> discrepancies.add("Missing simulated gateway refund: "+id));
        }
        for (Refund refund: allRefunds) {
            if (!"processed".equals(refund.getStatus())) continue;
            BigDecimal total=refund.getPayment().getAmount().add(RefundPolicy.wallet(refund.getPayment()));
            BigDecimal originalFee=refund.getPayment().getPlatformFee()==null?BigDecimal.ZERO:refund.getPayment().getPlatformFee();
            BigDecimal commission=total.signum()==0?BigDecimal.ZERO:refund.getAmount().multiply(originalFee).divide(total,2,java.math.RoundingMode.HALF_UP);
            netPlatformRevenue=netPlatformRevenue.subtract(commission);
            totalInstructorLiabilities=totalInstructorLiabilities.subtract(refund.getAmount().subtract(commission));
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
