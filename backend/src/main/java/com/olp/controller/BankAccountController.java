package com.olp.controller;

import com.olp.dto.BankPayoutRequest;
import com.olp.dto.PaymentSimulationDtos.BankAccountDecisionRequest;
import com.olp.dto.PaymentSimulationDtos.BankAccountRequest;
import com.olp.entity.User;
import com.olp.service.BankAccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/payments/bank-accounts")
public class BankAccountController {
    private final BankAccountService accounts;

    public BankAccountController(BankAccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public Map<String, Object> current(@AuthenticationPrincipal User user) {
        return accounts.current(user);
    }

    @PutMapping
    public Map<String, Object> save(@AuthenticationPrincipal User user, @RequestBody BankAccountRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Bank account request body is required.");
        }
        return accounts.save(user, new BankPayoutRequest(request.accountNumber(), request.ifscCode(), request.bankName()),
                request.holderName(), request.consent());
    }

    @GetMapping("/pending")
    public List<Map<String, Object>> pending(@AuthenticationPrincipal User user) {
        return accounts.pending(user);
    }

    @PostMapping("/decide")
    public Map<String, Object> decide(@AuthenticationPrincipal User user, @RequestBody BankAccountDecisionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Bank account decision request body is required.");
        }
        return accounts.decide(user, request.instructorId(), request.accountId(),
                request.approved(), request.identityChecked(), request.reason());
    }
}
