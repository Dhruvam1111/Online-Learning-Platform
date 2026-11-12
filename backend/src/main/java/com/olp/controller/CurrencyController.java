package com.olp.controller;

import com.olp.service.CurrencyService;
import org.springframework.http.ResponseEntity;
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
            @RequestBody Map<String, Object> payload) {
        BigDecimal amount = new BigDecimal(String.valueOf(payload.getOrDefault("amount", 499)));
        String targetCurrency = (String) payload.getOrDefault("targetCurrency", "USD");

        Map<String, Object> converted = currencyService.convertFromInr(amount, targetCurrency);
        return ResponseEntity.ok(converted);
    }
    public record RateUpdate(String currency, BigDecimal rate) {}
    @PatchMapping("/rates")
    public Map<String,Object> updateRate(@org.springframework.security.core.annotation.AuthenticationPrincipal com.olp.entity.User user, @RequestBody RateUpdate request) {
        return currencyService.updateRate(user,request.currency(),request.rate());
    }

}
