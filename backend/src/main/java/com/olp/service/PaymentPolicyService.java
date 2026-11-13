package com.olp.service;

import com.olp.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.math.BigDecimal;
import java.util.*;

/** Versioned policies and immutable per-order snapshots; amounts remain tax inclusive. */
@Service
public class PaymentPolicyService {
    @org.springframework.beans.factory.annotation.Autowired(required=false) private FinancialGovernanceService governance;
    private final PaymentAuditService audit;
    private final com.olp.repository.WalletRepository locks;
    public PaymentPolicyService(PaymentAuditService audit, com.olp.repository.WalletRepository locks) { this.audit=audit; this.locks=locks; }
    public Map<String,Object> current() {
        var rows=audit.list("financial.policy");
        var data=new LinkedHashMap<String,Object>(rows.isEmpty()?Map.of("commissionRate",new BigDecimal("0.20"),"taxRate",new BigDecimal("0.18"),"defaultPrice",new BigDecimal("499.00")):rows.get(rows.size()-1));data.put("commissionTaxRate",governance==null?new BigDecimal("0.18"):governance.commissionTaxRate());return data;
    }
    @Transactional
    public Map<String,Object> update(User admin,BigDecimal commission,BigDecimal tax,BigDecimal price) {
        if(admin==null || !"admin".equals(admin.getRole())) throw new AccessDeniedException("Administrator required.");
        rate(commission);rate(tax);
        if(price==null || price.signum()<=0 || price.compareTo(new BigDecimal("1000000"))>0 || price.stripTrailingZeros().scale()>2) throw new IllegalArgumentException("Price must be positive, in whole paisa, and at most 1000000 INR.");
        locks.lockStudent(admin.getId());
        var data=Map.<String,Object>of("commissionRate",commission,"taxRate",tax,"defaultPrice",price,"actorId",admin.getId());
        audit.record("policy-"+UUID.randomUUID(),"financial.policy",data);return data;
    }
    private void rate(BigDecimal value) {
        if(value==null || value.signum()<0 || value.compareTo(BigDecimal.ONE)>0 || value.stripTrailingZeros().scale()>4) throw new IllegalArgumentException("Rates must be between 0 and 1, with at most four decimal places.");
    }
    public BigDecimal value(Map<String,Object> data,String key) {return new BigDecimal(data.get(key).toString());}

    public Optional<BigDecimal> getAdminOverridePrice(Integer courseId) {
        if (courseId == null) return Optional.empty();
        var rows = audit.list("course.policy");
        for (int i = rows.size() - 1; i >= 0; i--) {
            if (((Number) rows.get(i).get("courseId")).intValue() == courseId) {
                return Optional.of(value(rows.get(i), "price"));
            }
        }
        return Optional.empty();
    }

    public BigDecimal getTaxRateForCourse(Integer courseId) {
        if (courseId != null) {
            var rows = audit.list("course.policy");
            for (int i = rows.size() - 1; i >= 0; i--) {
                if (((Number) rows.get(i).get("courseId")).intValue() == courseId && rows.get(i).containsKey("taxRate")) {
                    return value(rows.get(i), "taxRate");
                }
            }
        }
        return value(current(), "taxRate");
    }

    public BigDecimal price(com.olp.entity.Course course) {
        if (course == null) return value(current(), "defaultPrice");
        var rows=audit.list("course.policy");
        if (course.getId() != null) {
            for(int i=rows.size()-1;i>=0;i--)if(((Number)rows.get(i).get("courseId")).intValue()==course.getId())return value(rows.get(i),"price");
        }
        if (course.getPrice() != null && course.getPrice().compareTo(BigDecimal.ZERO) >= 0) return course.getPrice();
        var catalog=PaymentService.catalogPrice(course);
        return catalog.compareTo(new BigDecimal("499"))==0?value(current(),"defaultPrice"):catalog;
    }
    @Transactional
    public Map<String,Object> updateCourse(User admin,Integer courseId,BigDecimal price,BigDecimal tax) {
        if(admin==null || !"admin".equals(admin.getRole()))throw new AccessDeniedException("Administrator required.");
        rate(tax);
        if(courseId==null || courseId<1 || price==null || price.signum()<=0 || price.compareTo(new BigDecimal("1000000"))>0 || price.stripTrailingZeros().scale()>2)throw new IllegalArgumentException("Choose a course and a positive INR price in whole paisa.");
        locks.lockStudent(admin.getId());
        var data=Map.<String,Object>of("courseId",courseId,"price",price,"taxRate",tax,"actorId",admin.getId());
        audit.record("course-policy-"+UUID.randomUUID(),"course.policy",data);return data;
    }
    public void snapshot(String orderId,com.olp.entity.Course course) {
        var policy=new LinkedHashMap<>(current());
        var rows=audit.list("course.policy");
        for(int i=rows.size()-1;i>=0;i--)if(((Number)rows.get(i).get("courseId")).intValue()==course.getId()){policy.put("taxRate",rows.get(i).get("taxRate"));break;}
        audit.record("policy-order-"+orderId,"order.policy",policy);
    }
    public void snapshot(String orderId) {audit.record("policy-order-"+orderId,"order.policy",current());}
    public Map<String,Object> forOrder(String orderId) {
        return audit.exists("policy-order-"+orderId)?audit.get("policy-order-"+orderId):Map.of("commissionRate",new BigDecimal("0.20"),"taxRate",new BigDecimal("0.18"));
    }
}
