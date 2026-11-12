package com.olp.controller;
import com.olp.entity.User;
import com.olp.dto.BankPayoutRequest;
import com.olp.service.BankAccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/payments/bank-accounts")
public class BankAccountController {
    private final BankAccountService accounts;
    public BankAccountController(BankAccountService accounts){this.accounts=accounts;}
    public record Account(String accountNumber,String ifscCode,String bankName,String holderName,boolean consent){}
    public record Decision(Integer instructorId,String accountId,boolean approved,boolean identityChecked,String reason){}
    @GetMapping public Map<String,Object> current(@AuthenticationPrincipal User user){return accounts.current(user);}
    @PutMapping public Map<String,Object> save(@AuthenticationPrincipal User user,@RequestBody Account request){return accounts.save(user,new BankPayoutRequest(request.accountNumber(),request.ifscCode(),request.bankName()),request.holderName(),request.consent());}
    @GetMapping("/pending") public List<Map<String,Object>> pending(@AuthenticationPrincipal User user){return accounts.pending(user);}
    @PostMapping("/decide") public Map<String,Object> decide(@AuthenticationPrincipal User user,@RequestBody Decision request){return accounts.decide(user,request.instructorId(),request.accountId(),request.approved(),request.identityChecked(),request.reason());}
}
