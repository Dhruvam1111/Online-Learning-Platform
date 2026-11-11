package com.olp.service;

import com.olp.entity.*;
import com.olp.repository.*;
import com.olp.dto.RefundRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.BigDecimal;
import java.util.*;

/** State changes for simulated asynchronous refunds, payouts and disputes. */
@Service
public class PaymentEventActions {
    @org.springframework.beans.factory.annotation.Autowired private PaymentAuditService audit;
    @org.springframework.beans.factory.annotation.Autowired(required=false) private PaymentSettlementService settlement;
    @org.springframework.beans.factory.annotation.Autowired(required=false) private PaymentGateway gateway;
    @org.springframework.beans.factory.annotation.Autowired(required=false) private RoyaltyRefundService royalties;
    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final InstructorEarningRepository earnings;
    private final EnrollmentRepository enrollments;
    private final WalletService wallets;
    private final EmailService emailService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentEventActions.class);
    public PaymentEventActions(PaymentRepository payments, RefundRepository refunds,
            InstructorEarningRepository earnings, EnrollmentRepository enrollments, WalletService wallets, EmailService emailService) {
        this.payments=payments; this.refunds=refunds; this.earnings=earnings;
        this.enrollments=enrollments; this.wallets=wallets; this.emailService=emailService;
    }
    @Transactional
    public Map<String,Object> requestRefund(User user, RefundRequest request) {
        if (user == null) throw new AccessDeniedException("Sign in to request a refund.");
        Payment p=payments.findByOrderIdForUpdate(request.getPaymentId()).orElseThrow(() -> new IllegalArgumentException("Order not found."));
        if (!"admin".equals(user.getRole()) && !p.getStudent().getId().equals(user.getId())) throw new AccessDeniedException("This purchase belongs to another account.");
        if (!"success".equals(p.getStatus()) || RefundPolicy.hasActiveRefund(refunds,p.getId())) throw new IllegalStateException("This purchase is not eligible for a new refund.");
        RefundPolicy.withinWindow(p);
        RefundPolicy.reason(request.getReason());
        BigDecimal total=p.getAmount().add(p.getWalletAmountUsed()==null?BigDecimal.ZERO:p.getWalletAmountUsed());
        BigDecimal amount=request.getAmount()==null?total:request.getAmount();
        requireWholePaisa(amount);
        if (amount.compareTo(total) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed the amount paid.");
        }
        String id="RFD-SIM-"+UUID.randomUUID();
        refunds.save(new Refund(p,id,amount,"pending",request.getReason()));
        p.setRefundStatus("pending"); payments.save(p);
        return Map.of("refundId",id,"orderId",p.getOrderId(),"amount",amount,"status","pending");
    }
    private static void requireWholePaisa(BigDecimal amount) {
        if (amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Refund amount must be at least ₹0.01 and in whole paisa.");
        }
    }

    public void refund(Payment p, String refundId, String state) {
        Refund r=refunds.findByRazorpayRefundId(refundId).orElseThrow(() -> new IllegalArgumentException("Refund not found."));
        if (!r.getPayment().getId().equals(p.getId())) throw new IllegalArgumentException("Refund does not belong to this order.");
        if (!"pending".equals(r.getStatus())) return; // Terminal outcomes cannot be downgraded or applied twice.
        // Older pending refunds may have been rounded to zero when saved.
        if ("processed".equals(state)) requireWholePaisa(r.getAmount());
        r.setStatus(state); p.setRefundStatus(state);
        if ("processed".equals(state)) {
            BigDecimal wallet=RefundPolicy.walletShare(p, r.getAmount());
            BigDecimal cash=r.getAmount().subtract(wallet);
            if (audit != null) audit.record("gateway-refund-"+refundId, "gateway.refund", (settlement==null?Map.of("orderId",p.getOrderId(),"refundId",refundId,"amount",cash,"status","processed"):settlement.refundRecord(p,refundId,cash)));
            if (wallet.signum()>0) wallets.creditWallet(p.getStudent(),wallet,"Refund "+refundId);
            p.setRefundStatus("refunded"); p.setRefundAmount(r.getAmount()); p.setRefundReason(r.getReason());
            enrollments.deleteByStudentIdAndCourseId(p.getStudent().getId(),p.getCourse().getId());
            if(royalties!=null)royalties.adjust(p,r.getAmount(),refundId);
            else earnings.findByPaymentId(p.getId()).forEach(e -> { e.setPayoutStatus("halted"); earnings.save(e); });
        }
        refunds.save(r); payments.save(p);
        if ("processed".equals(state)) scheduleRefundConfirmation(p, r, refundId);
    }

    private void scheduleRefundConfirmation(Payment payment, Refund refund, String refundId) {
        User student = payment.getStudent();
        if (student.getEmail() == null || student.getEmail().isBlank()) return;

        String email = student.getEmail();
        String name = student.getName();
        String courseTitle = payment.getCourse().getTitle();
        String orderId = payment.getOrderId();
        BigDecimal amount = refund.getAmount();
        String reason = refund.getReason();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    emailService.sendRefundConfirmationEmail(email, name, courseTitle, orderId, refundId, amount, reason);
                } catch (RuntimeException exception) {
                    log.warn("Could not send confirmation for refund {}", refundId, exception);
                }
            }
        });
    }
    /** A valid late confirmation is compensated without granting or revoking another purchase's access. */
    public void reverseLateCapture(Payment payment){
        if(!Set.of("expired","failed","cancelled").contains(payment.getStatus()) || refunds.existsByPaymentId(payment.getId()))return;
        var quote=settlement==null?Map.<String,Object>of("providerCurrency","INR","providerAmount",payment.getAmount()):settlement.forOrder(payment);
        String refundId=gateway==null?"RFD-LATE-"+UUID.nameUUIDFromBytes(payment.getOrderId().getBytes(java.nio.charset.StandardCharsets.UTF_8)):gateway.reverseLateCapture(payment.getOrderId(),new BigDecimal(quote.get("providerAmount").toString()),quote.get("providerCurrency").toString());
        if(refundId==null || refundId.isBlank())throw new IllegalStateException("Provider reversal is pending. Retry the verified callback.");
        Refund reversal=new Refund(payment,refundId,payment.getAmount(),"processed","Late capture after order closure; source reversal");
        refunds.save(reversal);
        scheduleRefundConfirmation(payment,reversal,refundId);
        payment.setRefundStatus("refunded");payment.setRefundAmount(payment.getAmount());payment.setRefundReason("Late capture automatically reversed");payments.save(payment);
        if(audit!=null){
            var capture=new LinkedHashMap<String,Object>(quote);capture.put("orderId",payment.getOrderId());capture.put("amount",payment.getAmount());capture.put("currency",payment.getCurrency());capture.put("status","success");
            audit.record("gateway-capture-"+payment.getOrderId(),"gateway.capture",capture);
            audit.record("gateway-refund-"+refundId,"gateway.refund",settlement==null?Map.of("orderId",payment.getOrderId(),"refundId",refundId,"amount",payment.getAmount(),"status","processed"):settlement.refundRecord(payment,refundId,payment.getAmount()));
            audit.record("reversal-"+payment.getOrderId(),"gateway.reversal",Map.of("orderId",payment.getOrderId(),"refundId",refundId,"amount",payment.getAmount(),"mode","simulation"));
        }
    }
    public void payout(Payment p, int earningId, String state) {
        InstructorEarning e=earnings.findById(earningId).orElseThrow(() -> new IllegalArgumentException("Payout not found."));
        if (!e.getPayment().getId().equals(p.getId())) throw new IllegalArgumentException("Payout does not belong to this order.");
        if (!"success".equals(p.getStatus()) || RefundPolicy.blocksPayout(p) || "halted".equals(e.getPayoutStatus()))
            throw new IllegalStateException("This payout is on hold.");
        if ("paid".equals(e.getPayoutStatus())) return;
        e.setPayoutStatus(state); earnings.save(e);
    }
    public void dispute(Payment p, String event, String outcome) {
        if (event.equals("chargeback.opened")) {
            if (!"success".equals(p.getStatus())) return;
            p.setStatus("disputed");
            earnings.findByPaymentId(p.getId()).forEach(e -> {
                e.setPayoutStatus("paid".equals(e.getPayoutStatus())?"disputed_paid":"disputed"); earnings.save(e);
            });
        } else {
            if (!Set.of("won","lost").contains(outcome)) throw new IllegalArgumentException("Dispute outcome must be won or lost.");
            if (!"disputed".equals(p.getStatus())) return;
            boolean won="won".equals(outcome); p.setStatus(won?"success":"charged_back");
            earnings.findByPaymentId(p.getId()).forEach(e -> {
                e.setPayoutStatus(won?("disputed_paid".equals(e.getPayoutStatus())?"paid":"unpaid"):"reversed"); earnings.save(e);
            });
            if (!won) enrollments.deleteByStudentIdAndCourseId(p.getStudent().getId(),p.getCourse().getId());
        }
        payments.save(p);
    }
}
