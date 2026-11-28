package com.olp.controller;

import com.olp.dto.PaymentSimulationDtos.CurrencyRateUpdateRequest;
import com.olp.entity.User;
import com.olp.service.CurrencyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Controller exposing foreign exchange rates and cross-border currency conversion (FR-PG01).
 * Owned by Dhruvam (Forex & Pricing Engine).
 */
@RestController
@RequestMapping("/api/payments/currencies")
public class CurrencyController {

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    /**
     * Lists all supported currencies and exchange rates against INR.
     */
    @GetMapping("/rates")
    public ResponseEntity<Map<String, Object>> getRates() {
        return ResponseEntity.ok(currencyService.getSupportedCurrencies());
    }

    /**
     * Converts a given amount to target foreign currency.
     */
    @PostMapping("/convert")
    public ResponseEntity<Map<String, Object>> convertCurrency(
            @RequestBody(required = false) Map<String, Object> payload) {
        Object rawAmount = payload != null ? payload.get("amount") : null;
        BigDecimal amount;
        try {
            amount = rawAmount != null ? new BigDecimal(String.valueOf(rawAmount).trim()) : BigDecimal.valueOf(499);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid amount format for currency conversion");
        }
        Object rawTarget = payload != null ? payload.get("targetCurrency") : null;
        String targetCurrency = rawTarget != null ? String.valueOf(rawTarget).trim() : "USD";

        Map<String, Object> converted = currencyService.convertFromInr(amount, targetCurrency);
        return ResponseEntity.ok(converted);
    }

    @PatchMapping("/rates")
    public Map<String, Object> updateRate(@AuthenticationPrincipal User user,
                                          @RequestBody CurrencyRateUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Currency rate update request body is required");
        }
        return currencyService.updateRate(user, request.currency(), request.rate());
    }
}
