package com.olp.service;

import com.olp.entity.User;
import com.olp.dto.CreatePaymentOrderRequest;
import com.olp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Durable, consent-based monthly simulator. Each debit requires a new explicit customer approval. */
@Service
public class PaymentSubscriptionService {
    private final PaymentAuditService audit;
    private final WalletRepository locks;
    private final SimulatedPaymentMethods methods;
    private final PaymentService payments;
    private final CourseRepository courses;
    private final PaymentPolicyService policies;
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private EmailService emailService;

    public PaymentSubscriptionService(PaymentAuditService audit,WalletRepository locks,SimulatedPaymentMethods methods,PaymentService payments,CourseRepository courses,PaymentPolicyService policies){
        this.audit=audit;this.locks=locks;this.methods=methods;this.payments=payments;this.courses=courses;this.policies=policies;
    }
    public PaymentSubscriptionService(PaymentAuditService audit,WalletRepository locks,SimulatedPaymentMethods methods,PaymentService payments,CourseRepository courses,PaymentPolicyService policies,EmailService emailService){
        this.audit=audit;this.locks=locks;this.methods=methods;this.payments=payments;this.courses=courses;this.policies=policies;this.emailService=emailService;
    }
    private void student(User user){if(user==null || !"student".equals(user.getRole()))throw new AccessDeniedException("Student account required.");}
    @Transactional
    public Map<String,Object> create(User user,Integer courseId,String token,boolean consent){
        return create(user,courseId,token,consent,1,"Monthly Plan",null,null);
    }
    @Transactional
    public Map<String,Object> create(User user,Integer courseId,String token,boolean consent,Integer durationMonths,String planName,java.math.BigDecimal customMonthlyPrice,java.math.BigDecimal customTotalAmount){
        student(user);locks.lockStudent(user.getId());
        if(!consent)throw new IllegalArgumentException("Explicit subscription consent is required.");
        if(methods.list(user).stream().noneMatch(m -> token!=null && token.equals(m.get("token"))))throw new IllegalArgumentException("Choose an active saved method.");
        var courseOpt = courses.findById(courseId);
        if(courseId==null || courseOpt.filter(c -> c.getStatus()==com.olp.entity.CourseStatus.PUBLISHED).isEmpty())throw new IllegalArgumentException("Choose a published course for subscription billing.");
        if(list(user).stream().anyMatch(m -> courseId.equals(((Number)m.get("courseId")).intValue()) && !"cancelled".equals(m.get("status"))))throw new IllegalStateException("An active subscription already exists for this course.");

        var course = courseOpt.get();
        java.math.BigDecimal monthlyPrice = policies.price(course);
        int duration = (durationMonths != null && durationMonths > 0) ? durationMonths : 1;
        String resolvedPlan = (planName != null && !planName.isBlank()) ? planName : (duration == 1 ? "Monthly Plan" : duration + "-Month Plan");
        java.math.BigDecimal totalPlanCost = (customTotalAmount != null && customTotalAmount.compareTo(java.math.BigDecimal.ZERO) > 0)
                ? customTotalAmount
                : monthlyPrice.multiply(java.math.BigDecimal.valueOf(duration));

        String id="sub_"+UUID.randomUUID();
        Instant nextRenewal = Instant.now().plus(30,ChronoUnit.DAYS);
        Map<String,Object> state=new LinkedHashMap<>();
        state.put("id",id);
        state.put("owner",user.getId());
        state.put("courseId",courseId);
        state.put("courseTitle",course.getTitle());
        state.put("token",token);
        state.put("consent",true);
        state.put("status","active");
        state.put("durationMonths",duration);
        state.put("planName",resolvedPlan);
        state.put("monthlyPrice",monthlyPrice);
        state.put("totalPlanCost",totalPlanCost);
        state.put("payableAmount",monthlyPrice);
        state.put("nextRenewalAt",nextRenewal.toString());
        state.put("createdAt",Instant.now().toString());

        // Locate recognizable saved method details for confirmation email / user summary
        var savedMethodOpt = methods.list(user).stream().filter(m -> token.equals(m.get("token"))).findFirst();
        String methodSummary = savedMethodOpt.map(m -> {
            Object disp = m.get("displayName");
            if (disp != null) return disp.toString();
            String mType = String.valueOf(m.get("method"));
            return mType.toUpperCase() + " (ends in " + token.substring(Math.max(0, token.length() - 4)) + ")";
        }).orElse("Saved Payment Method");
        state.put("paymentMethodSummary", methodSummary);

        audit.record(id,"subscription.created",state);

        String successMessage = String.format(
            "Subscription activated successfully for '%s'! Plan: %s (%d Month%s) at ₹%s/month (Total plan cost: ₹%s). Next renewal scheduled for %s. A confirmation email with plan details has been sent to %s.",
            course.getTitle(), resolvedPlan, duration, duration > 1 ? "s" : "", monthlyPrice.toPlainString(), totalPlanCost.toPlainString(), nextRenewal.toString().substring(0, 10), user.getEmail()
        );
        state.put("successMessage", successMessage);

        if (emailService != null) {
            try {
                emailService.sendSubscriptionConfirmationEmail(
                    user.getEmail(),
                    user.getName(),
                    course.getTitle(),
                    resolvedPlan,
                    duration,
                    monthlyPrice,
                    totalPlanCost,
                    nextRenewal.toString(),
                    methodSummary,
                    id
                );
            } catch (Exception ignored) {
            }
        }

        return state;
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> list(User user){
        student(user);
        return audit.list("subscription.created").stream().filter(m -> ((Number)m.get("owner")).intValue()==user.getId()).map(m -> state(m.get("id").toString())).toList();
    }
    private Map<String,Object> state(String id){
        Map<String,Object> state=new LinkedHashMap<>(audit.get(id));
        for(var renewal:audit.list("subscription.renewed"))if(id.equals(renewal.get("subscriptionId")))state.put("nextRenewalAt",renewal.get("nextRenewalAt"));
        if(audit.exists("cancel-"+id))state.put("status","cancelled");
        String cycle=id+"-"+state.get("nextRenewalAt");
        if(audit.exists("notice-"+cycle))state.put("notice",audit.get("notice-"+cycle));

        if(state.get("courseId") != null) {
            courses.findById(((Number)state.get("courseId")).intValue()).ifPresent(c -> {
                if(state.get("courseTitle") == null) {
                    state.put("courseTitle", c.getTitle());
                }
                if(state.get("payableAmount") == null) {
                    state.put("payableAmount", policies.price(c));
                }
                if(state.get("monthlyPrice") == null) {
                    state.put("monthlyPrice", policies.price(c));
                }
            });
        }
        return state;
    }
    private Map<String,Object> owned(User user,String id){
        student(user);locks.lockStudent(user.getId());var state=state(id);
        if(((Number)state.get("owner")).intValue()!=user.getId())throw new AccessDeniedException("This subscription belongs to another account.");
        return state;
    }
    @Transactional
    public void cancel(User user,String id){owned(user,id);audit.record("cancel-"+id,"subscription.cancelled",Map.of("subscriptionId",id,"actorId",user.getId()));}
    /** Inbox notice is durable and available at least 24 hours before an approved debit. */
    @Transactional
    public void prepareNotices(){
        for(var original:audit.list("subscription.created")){
            Integer owner=((Number)original.get("owner")).intValue();locks.lockStudent(owner);
            var state=state(original.get("id").toString());if("cancelled".equals(state.get("status")))continue;
            Instant due=Instant.parse(state.get("nextRenewalAt").toString());
            if(Instant.now().isBefore(due.minus(1,ChronoUnit.DAYS)))continue;
            String cycle=state.get("id")+"-"+due;
            audit.record("notice-"+cycle,"subscription.notice",Map.of("subscriptionId",state.get("id"),"owner",owner,"dueAt",due.toString(),"notifiedAt",Instant.now().toString(),"message","Renewal requires your approval; no automatic debit. Review the current INR price before approving."));
        }
    }
    @Transactional
    public Map<String,Object> quote(User user,String id){
        var state=owned(user,id);
        var course=courses.findById(((Number)state.get("courseId")).intValue()).orElseThrow(() -> new IllegalArgumentException("Course no longer available."));
        if(course.getStatus()!=com.olp.entity.CourseStatus.PUBLISHED)throw new IllegalStateException("The course is not currently available for renewal.");
        var price=policies.price(course);
        return Map.of("amount",price,"currency","INR","taxInclusive",true,"mode","simulation","refundPolicy","30-day eligibility; full or partial refunds to original simulated sources.");
    }
    @Transactional
    public Map<String,Object> approve(User user,String id,boolean approval,java.math.BigDecimal expectedAmount,String requestId){
        var state=owned(user,id);
        if(requestId==null || !requestId.matches("[A-Za-z0-9_-]{1,36}"))throw new IllegalArgumentException("A unique approval request ID is required.");
        String approvalId="approve-"+id+"-"+requestId;
        if(audit.exists(approvalId))return audit.get(approvalId);
        if(!approval)throw new IllegalArgumentException("Explicit approval is required for this simulated debit.");
        if("cancelled".equals(state.get("status")))throw new IllegalStateException("Subscription is cancelled.");
        Instant due=Instant.parse(state.get("nextRenewalAt").toString());
        String cycle=id+"-"+due;
        if(Instant.now().isBefore(due) || !audit.exists("notice-"+cycle) || Instant.now().isBefore(Instant.parse(audit.get("notice-"+cycle).get("notifiedAt").toString()).plus(1,ChronoUnit.DAYS)))throw new IllegalStateException("Renewal requires a due date and at least 24 hours advance notice.");
        // Locking the owner serializes renewal and cancellation, including across application instances.
        var request=new CreatePaymentOrderRequest(((Number)state.get("courseId")).intValue(),null,"INR");
        var quote=quote(user,id);
        if(expectedAmount==null || expectedAmount.compareTo(new java.math.BigDecimal(quote.get("amount").toString()))!=0)throw new IllegalArgumentException("Review and approve the current renewal total.");
        var created=payments.createPaymentOrder(user,request);
        if(created.getAmount().compareTo(expectedAmount)!=0)throw new IllegalStateException("The price changed. Please review the renewal again.");
        String order=created.getOrderId();
        audit.record("accepted-policy-"+order,"order.policy.accepted",Map.of("orderId",order,"studentId",user.getId(),"courseId",state.get("courseId"),"policyVersion",CheckoutPolicy.VERSION,"policyText",CheckoutPolicy.TEXT));
        methods.pay(user,state.get("token").toString(),order);
        Map<String,Object> result=Map.of("subscriptionId",id,"orderId",order,"nextRenewalAt",Instant.now().plus(30,ChronoUnit.DAYS).toString(),"actorId",user.getId());
        audit.record("renew-"+cycle,"subscription.renewed",result);audit.record(approvalId,"subscription.approval",result);return result;
    }
}
