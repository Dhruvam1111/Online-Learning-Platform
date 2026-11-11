package com.olp.service;
import com.olp.entity.User;
import com.olp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
@Service
public class PaymentEvidenceService {
    private final PaymentAuditService audit;
    private final PaymentRepository payments;
    public PaymentEvidenceService(PaymentAuditService audit,PaymentRepository payments){this.audit=audit;this.payments=payments;}
    @Transactional public void accept(User user,String orderId,String version){
        if(user==null)throw new AccessDeniedException("Sign in to accept the checkout policy.");
        var payment=payments.findByOrderIdForUpdate(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found."));
        if(!user.getId().equals(payment.getStudent().getId()))throw new AccessDeniedException("This order belongs to another student.");
        if(!CheckoutPolicy.VERSION.equals(version))throw new IllegalArgumentException("Review the current refund policy before paying.");
        audit.record("accepted-policy-"+orderId,"order.policy.accepted",Map.of("orderId",orderId,"studentId",user.getId(),"courseId",payment.getCourse().getId(),"policyVersion",version,"policyText",CheckoutPolicy.TEXT));
    }
    @Transactional(readOnly=true) public Map<String,Object> evidence(User admin,String orderId){
        if(admin==null || !"admin".equals(admin.getRole()))throw new AccessDeniedException("Administrator required.");
        var p=payments.findByOrderId(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found."));
        var result=new LinkedHashMap<String,Object>();result.put("orderId",orderId);result.put("studentId",p.getStudent().getId());result.put("courseId",p.getCourse().getId());
        result.put("paymentStatus",p.getStatus());result.put("enrollmentSnapshot",audit.exists("enrollment-"+orderId)?audit.get("enrollment-"+orderId):Map.of());
        result.put("policyAcceptance",audit.exists("accepted-policy-"+orderId)?audit.get("accepted-policy-"+orderId):Map.of());
        result.put("accessHistory",audit.list("course.access").stream().filter(r -> p.getStudent().getId().equals(((Number)r.get("studentId")).intValue()) && p.getCourse().getId().equals(((Number)r.get("courseId")).intValue())).toList());
        return result;
    }
}
