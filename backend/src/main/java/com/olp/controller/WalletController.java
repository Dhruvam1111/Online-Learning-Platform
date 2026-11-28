package com.olp.controller;

import com.olp.dto.PaymentSimulationDtos.WalletTopUpRequest;
import com.olp.entity.User;
import com.olp.entity.Wallet;
import com.olp.entity.WalletTransaction;
import com.olp.exception.InvalidCredentialsException;
import com.olp.service.SimulatedPaymentMethods;
import com.olp.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller exposing student wallet balance, top-up simulation, and credit history (FR-PG02).
 * Owned by Shubh (Wallet Engine).
 */
@RestController
@RequestMapping("/api/payments/wallet")
public class WalletController {

    private final WalletService walletService;
    private final SimulatedPaymentMethods methods;

    public WalletController(WalletService walletService) {
        this(walletService, null);
    }

    @Autowired
    public WalletController(WalletService walletService,
                            @Autowired(required = false) SimulatedPaymentMethods methods) {
        this.walletService = walletService;
        this.methods = methods;
    }

    /**
     * Returns student's active wallet balance and transaction ledger.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getWallet(
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
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
            @RequestBody(required = false) Map<String, Object> payload) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        Object rawAmount = payload != null ? payload.get("amount") : null;
        BigDecimal amount;
        try {
            amount = rawAmount != null ? new BigDecimal(String.valueOf(rawAmount).trim()) : BigDecimal.valueOf(50);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid reward credit amount");
        }
        Object rawReason = payload != null ? payload.get("reason") : null;
        String reason = rawReason != null ? String.valueOf(rawReason) : "Achievement Bonus Reward";

        walletService.creditWallet(currentUser, amount, reason);
        BigDecimal newBalance = walletService.getBalance(currentUser);

        return ResponseEntity.ok(Map.of(
                "message", "Reward credits added successfully",
                "creditedAmount", amount,
                "currentBalance", newBalance
        ));
    }

    @PostMapping("/top-up")
    public Map<String, Object> topUp(@AuthenticationPrincipal User user, @RequestBody WalletTopUpRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Wallet top-up request body is required.");
        }
        String method = request.method();
        if (request.token() != null && !request.token().isBlank()) {
            if (methods == null) {
                throw new IllegalArgumentException("Saved payment methods are unavailable.");
            }
            var saved = methods.list(user).stream()
                    .filter(entry -> request.token().equals(entry.get("token")))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Choose an active saved payment method."));
            Object savedMethod = saved.get("method");
            method = savedMethod != null ? savedMethod.toString() : null;
        }
        if (method == null || !Set.of("card", "upi", "netbanking").contains(method)) {
            throw new IllegalArgumentException("Choose card, UPI or net banking. Wallet credit cannot fund itself.");
        }
        return walletService.topUp(user, request.amount(), request.requestId(), request.outcome());
    }
}
