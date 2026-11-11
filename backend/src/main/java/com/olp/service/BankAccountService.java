package com.olp.service;

import com.olp.entity.User;
import com.olp.dto.BankPayoutRequest;
import com.olp.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;

/** Persistent demo payout-account workflow. Stores provider tokens and last-four digits only. */
@Service
public class BankAccountService {
    private final PaymentAuditService audit;
    private final WalletRepository locks;
    private final PaymentGateway gateway;
    public BankAccountService(PaymentAuditService audit,WalletRepository locks,PaymentGateway gateway){this.audit=audit;this.locks=locks;this.gateway=gateway;}
    private void instructor(User user){if(user==null || !"instructor".equals(user.getRole()))throw new AccessDeniedException("Instructor account required.");}
    @Transactional(readOnly=true)
    public Map<String,Object> current(User user){instructor(user);return publicView(latest(user.getId()));}
    private Map<String,Object> publicView(Map<String,Object> account){var view=new LinkedHashMap<>(account);view.remove("providerAccountToken");return view;}
    private Map<String,Object> latest(Integer owner){
        var rows=audit.list("bank.account");
        for(int i=rows.size()-1;i>=0;i--){var row=rows.get(i);if(((Number)row.get("instructorId")).intValue()==owner){
            var data=new LinkedHashMap<>(row);String decision="bank-decision-"+row.get("id");
            if(audit.exists(decision))data.putAll(audit.get(decision));return data;
        }}
        return Map.of("status","not_configured","mode","simulation");
    }
    @Transactional
    public Map<String,Object> save(User user,BankPayoutRequest bank,String holderName,boolean consent){
        instructor(user);if(bank==null)throw new IllegalArgumentException("Bank details are required.");bank.validate();
        if(!consent)throw new IllegalArgumentException("Consent is required to save a payout account.");
        if(holderName==null || user.getName()==null || !holderName.trim().replaceAll("\\s+"," ").equalsIgnoreCase(user.getName().trim().replaceAll("\\s+"," ")))throw new IllegalArgumentException("Use the instructor's own account-holder name.");
        locks.lockStudent(user.getId());String id="bank_"+UUID.randomUUID();
        var data=Map.<String,Object>ofEntries(Map.entry("id",id),Map.entry("instructorId",user.getId()),Map.entry("holderName",holderName.trim()),Map.entry("bankName",bank.bankName()),Map.entry("ifscCode",bank.ifscCode()),Map.entry("account","•••• "+bank.accountNumber().substring(bank.accountNumber().length()-4)),Map.entry("providerAccountToken",gateway.linkBankAccount(bank,holderName)),Map.entry("status","pending"),Map.entry("consent",true),Map.entry("mode","simulation"));
        audit.record(id,"bank.account",data);return publicView(data);
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> pending(User admin){
        admin(admin);var owners=new LinkedHashSet<Integer>();audit.list("bank.account").forEach(r -> owners.add(((Number)r.get("instructorId")).intValue()));
        return owners.stream().map(this::latest).filter(r -> "pending".equals(r.get("status"))).map(this::publicView).toList();
    }
    private void admin(User user){if(user==null || !"admin".equals(user.getRole()))throw new AccessDeniedException("Administrator required.");}
    @Transactional
    public Map<String,Object> decide(User admin,Integer owner,String id,boolean approve,boolean identityChecked,String reason){
        admin(admin);if(owner==null || owner<1 || id==null || !id.matches("bank_[a-f0-9-]{36}"))throw new IllegalArgumentException("Choose a valid pending account.");RefundPolicy.reason(reason);locks.lockStudent(owner);var account=latest(owner);
        if(!id.equals(account.get("id")))throw new IllegalStateException("Account details changed; review the current account.");
        if(approve && !identityChecked)throw new IllegalArgumentException("Confirm the simulated identity and ownership check before approval.");
        String key="bank-decision-"+id;
        if(audit.exists(key))throw new IllegalStateException("This account already has a decision; submit an updated account for another review.");
        var data=Map.<String,Object>of("id",id,"instructorId",owner,"actorId",admin.getId(),"status",approve?"verified":"rejected","reason",reason.trim(),"identityChecked",identityChecked,"mode","simulation");
        audit.record(key,"bank.decision",data);return data;
    }
    @Transactional
    public Map<String,Object> requireVerified(User user){
        instructor(user);locks.lockStudent(user.getId());var profile=latest(user.getId());
        if(!"verified".equals(profile.get("status")))throw new IllegalStateException("Save your payout account and obtain simulated admin verification before withdrawing.");
        return profile;
    }
}
