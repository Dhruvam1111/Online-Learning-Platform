package com.olp.service;
import com.olp.entity.User;
import com.olp.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.math.BigDecimal;
import java.util.*;
/** Simulator finance policy. Tax applicability must be configured by an authorised administrator. */
@Service
public class FinancialGovernanceService {
    private final PaymentAuditService audit;
    private final WalletRepository locks;
    public FinancialGovernanceService(PaymentAuditService audit,WalletRepository locks){this.audit=audit;this.locks=locks;}
    public Map<String,Object> policy(){var rows=audit.list("finance.governance");return rows.isEmpty()?Map.of("financialRetentionDays",2557,"adminAuditRetentionDays",365,"withholdingRate",BigDecimal.ZERO,"commissionTaxRate",new BigDecimal("0.18"),"mode","simulation"):rows.get(rows.size()-1);}
    public BigDecimal commissionTaxRate(){return new BigDecimal(policy().getOrDefault("commissionTaxRate",new BigDecimal("0.18")).toString());}
    public BigDecimal withholdingRate(){return new BigDecimal(policy().get("withholdingRate").toString());}
    public void admin(User user){if(user==null || !"admin".equals(user.getRole()))throw new AccessDeniedException("Administrator required.");}
    @Transactional public Map<String,Object> update(User user,Integer financialDays,Integer auditDays,BigDecimal rate){
        return update(user,financialDays,auditDays,rate,commissionTaxRate());
    }
    @Transactional public Map<String,Object> update(User user,Integer financialDays,Integer auditDays,BigDecimal rate,BigDecimal commissionTax){
        admin(user);if(commissionTax==null || commissionTax.signum()<0 || commissionTax.compareTo(BigDecimal.ONE)>0 || commissionTax.stripTrailingZeros().scale()>4)throw new IllegalArgumentException("Commission tax rate must be between 0 and 1.");
        if(financialDays==null || financialDays<2557 || financialDays>36500 || auditDays==null || auditDays<365 || auditDays>36500 || rate==null || rate.signum()<0 || rate.compareTo(BigDecimal.ONE)>0 || rate.stripTrailingZeros().scale()>4)throw new IllegalArgumentException("Retain financial records for at least 2557 days, admin audit for at least 365 days, and choose a withholding rate between 0 and 1.");
        locks.lockStudent(user.getId());var data=Map.<String,Object>of("financialRetentionDays",financialDays,"adminAuditRetentionDays",auditDays,"withholdingRate",rate,"commissionTaxRate",commissionTax,"actorId",user.getId(),"mode","simulation");
        audit.record("finance-policy-"+UUID.randomUUID(),"finance.governance",data);return data;
    }
}
