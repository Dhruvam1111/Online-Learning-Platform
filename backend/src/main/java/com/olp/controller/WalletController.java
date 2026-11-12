package com.olp.controller;

import com.olp.entity.User;
import com.olp.entity.Wallet;
import com.olp.entity.WalletTransaction;
import com.olp.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Controller exposing student wallet balance, top-up simulation, and credit history (FR-PG02).
 * Owned by Shubh (Wallet Engine).
 */
@RestController
@RequestMapping("/api/payments/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Returns student's active wallet balance and transaction ledger.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getWallet(
            @AuthenticationPrincipal User currentUser) {
        Wallet wallet = walletService.getOrCreateWallet(currentUser);
        List<WalletTransaction> history = walletService.getTransactionHistory(currentUser);

        return ResponseEntity.ok(Map.of(
                "studentId", currentUser.getId(),
                "balance", wallet.getBalance(),
                "currency", "INR",
                "transactions", history
        ));
    }

    /**
     * Adds promotional reward credits to student's wallet (e.g. referral bonus, quiz achievement).
     */
    @PostMapping("/add-reward")
    public ResponseEntity<Map<String, Object>> addRewardCredit(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, Object> payload) {
        BigDecimal amount = new BigDecimal(String.valueOf(payload.getOrDefault("amount", 50)));
        String reason = (String) payload.getOrDefault("reason", "Achievement Bonus Reward");

        walletService.creditWallet(currentUser, amount, reason);
        BigDecimal newBalance = walletService.getBalance(currentUser);

        return ResponseEntity.ok(Map.of(
                "message", "Reward credits added successfully",
                "creditedAmount", amount,
                "currentBalance", newBalance
        ));
    }
    public record TopUpRequest(BigDecimal amount, String requestId, String outcome) {}
    @PostMapping("/top-up")
    public Map<String,Object> topUp(@AuthenticationPrincipal User user, @RequestBody TopUpRequest request) {
        return walletService.topUp(user,request.amount(),request.requestId(),request.outcome());
    }

}
