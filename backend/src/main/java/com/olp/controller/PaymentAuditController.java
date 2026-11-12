package com.olp.controller;
import com.olp.entity.User;
import com.olp.service.PaymentAuditService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/payments/governance")
public class PaymentAuditController {
    private final PaymentAuditService audit;
    public PaymentAuditController(PaymentAuditService audit) { this.audit=audit; }
    @GetMapping("/audit")
    public List<Map<String,Object>> history(@AuthenticationPrincipal User user) {
        if(user==null || !"admin".equalsIgnoreCase(user.getRole())) throw new AccessDeniedException("Administrator required.");
        List<Map<String,Object>> rows=new ArrayList<>();
        for(String type:List.of("payout.hold","payout.release","payout.settled","refund.exception.request","refund.exception.decision","currency.rate","financial.policy","reconciliation.daily","admin.payment.action","admin.payment.outcome","admin.payment.subject","bank.account","bank.decision","finance.governance","royalty.debt","royalty.refund","royalty.recovered","order.risk"))
            for(var row:audit.list(type)) { row.remove("providerAccountToken"); row.put("type",type); rows.add(row); }
        rows.sort(Comparator.comparing(r -> r.get("recordedAt").toString()));
        return rows;
    }
}
