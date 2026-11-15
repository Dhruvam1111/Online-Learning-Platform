package com.olp.service;

import com.olp.dto.RefundRequest;
import com.olp.entity.*;
import com.olp.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

/** Durable request -> approved/rejected workflow, serialized on the payment row. */
@Service
public class ExceptionalRefundService {
    @org.springframework.beans.factory.annotation.Autowired private EmailService email;
    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final PaymentAuditService audit;
    private final RefundService settlement;
    public ExceptionalRefundService(PaymentRepository payments, RefundRepository refunds, PaymentAuditService audit, RefundService settlement) {
        this.payments=payments; this.refunds=refunds; this.audit=audit; this.settlement=settlement;
    }
    private void admin(User user) { if (user == null || !"admin".equalsIgnoreCase(user.getRole())) throw new AccessDeniedException("Administrator required."); }
    /** Route by the server's purchase timestamp; late requests never settle automatically. */
    @Transactional
    public com.olp.dto.RefundResponse submit(User user, RefundRequest request) {
        if (user == null) throw new AccessDeniedException("Sign in first.");
        Payment p = payments.findByPaymentIdForUpdate(request.getPaymentId())
            .or(() -> payments.findByOrderIdForUpdate(request.getPaymentId()))
            .orElseThrow(() -> new IllegalArgumentException("Order not found."));
        if (!"admin".equalsIgnoreCase(user.getRole()) && !user.getId().equals(p.getStudent().getId()))
            throw new AccessDeniedException("This purchase belongs to another user.");
        if (p.getCreatedAt() == null) throw new IllegalStateException("Purchase date is unavailable.");
        if (!p.getCreatedAt().plusDays(30).isBefore(java.time.LocalDateTime.now()))
            return settlement.processRefund(user, request);
        var submitted = request(user, new RefundRequest(p.getOrderId(), request.getAmount(), request.getReason()));
        return new com.olp.dto.RefundResponse(submitted.get("eventId").toString(), p.getPaymentId(), p.getOrderId(),
            p.getCourse().getId(), p.getCourse().getTitle(), RefundPolicy.amount(p, request.getAmount()),
            "pending_review", request.getReason(), false, java.time.LocalDateTime.now());
    }
    @Transactional
    public Map<String,Object> request(User user, RefundRequest request) {
        if (user == null) throw new AccessDeniedException("Sign in first.");
        Payment p=payments.findByOrderIdForUpdate(request.getPaymentId()).orElseThrow(() -> new IllegalArgumentException("Order not found."));
        if (!"admin".equalsIgnoreCase(user.getRole()) && !user.getId().equals(p.getStudent().getId())) throw new AccessDeniedException("This purchase belongs to another user.");
        if (!"success".equals(p.getStatus()) || refunds.existsByPaymentId(p.getId())) throw new IllegalStateException("Purchase is not refundable.");
        String id="exception-"+p.getOrderId();
        if (audit.exists(id)) throw new IllegalStateException("An exceptional refund request already exists for this purchase.");
        audit.record(id,"refund.exception.request",Map.of("orderId",p.getOrderId(),"requesterId",user.getId(),"amount",RefundPolicy.amount(p,request.getAmount()),"reason",RefundPolicy.reason(request.getReason())));
        return audit.get(id);
    }
    @Transactional(readOnly = true)
    public PaymentAuditService.RefundQueue queue(User user, String status, String search, int page, int size) {
        admin(user);
        if (!Set.of("pending", "history", "approved", "rejected").contains(status))
            throw new IllegalArgumentException("Invalid refund queue status.");
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Invalid refund queue page or size.");
        return audit.refundQueue(status, search.strip(), page, size);
    }

    @Transactional(readOnly=true)
    public List<Map<String,Object>> list(User user) {
        if (user == null) throw new AccessDeniedException("Sign in first.");
        return audit.list("refund.exception.request").stream().filter(r -> "admin".equalsIgnoreCase(user.getRole()) || user.getId().toString().equals(r.get("requesterId").toString())).map(r -> {
            String decision="decision-"+r.get("eventId");
            r.put("status", audit.exists(decision) ? audit.get(decision).get("decision") : "pending");
            if (audit.exists(decision)) r.put("decisionRecord",audit.get(decision));
            return r;
        }).toList();
    }
    @Transactional
    public Map<String,Object> decide(User user, String id, String decision, String reason) {
        return decide(user, id, decision, reason, null);
    }
    @Transactional
    public Map<String,Object> decide(User user, String id, String decision, String reason, BigDecimal approvedAmount) {
        admin(user); RefundPolicy.reason(reason);
        if (!Set.of("approved","rejected").contains(decision)) throw new IllegalArgumentException("Decision must be approved or rejected.");
        Map<String,Object> request=audit.get(id);
        if (!id.startsWith("exception-")) throw new IllegalArgumentException("Not an exceptional refund request.");
        String order=request.get("orderId").toString();
        payments.findByOrderIdForUpdate(order).orElseThrow(() -> new IllegalArgumentException("Order not found."));
        if (audit.exists("decision-"+id)) throw new IllegalStateException("This request has already been decided.");
        BigDecimal requestedAmount = new BigDecimal(request.get("amount").toString());
        BigDecimal settledAmount = "approved".equals(decision) ? (approvedAmount == null ? requestedAmount : approvedAmount) : BigDecimal.ZERO;
        if ("approved".equals(decision) && (settledAmount.signum() <= 0 || settledAmount.stripTrailingZeros().scale() > 2 || settledAmount.compareTo(requestedAmount) > 0))
            throw new IllegalArgumentException("Approved amount must be positive, in whole paisa, and no greater than the requested amount.");
        if ("approved".equals(decision)) settlement.processApprovedException(user,new RefundRequest(order,settledAmount,reason));
        audit.record("decision-"+id,"refund.exception.decision",Map.of("requestId",id,"orderId",order,"actorId",user.getId(),"decision",decision,"reason",reason,"approvedAmount",settledAmount));
        var payment = payments.findByOrderIdForUpdate(order).orElseThrow();
        String recipient = payment.getStudent().getEmail();
        String studentName = payment.getStudent().getName();
        String courseTitle = payment.getCourse().getTitle();
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override public void afterCommit() {
                    email.sendRefundDecisionEmail(recipient, studentName, courseTitle, order, decision, reason, settledAmount);
                }
            });
        return audit.get("decision-"+id);
    }
}
