package com.olp.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Enterprise Multi-Currency & Forex Rate Engine (FR-PG01, FR-PG03).
 * Owned by Dhruvam (Forex & Pricing Engine).
 * Supports real-time currency conversion for international cross-border enrollments.
 */
@Service
public class CurrencyService {

    // Base currency is INR. Exchange rates against 1 unit of foreign currency:
    private static final Map<String, BigDecimal> RATES = new LinkedHashMap<>();
    private static final Map<String, String> SYMBOLS = new LinkedHashMap<>();

    static {
        RATES.put("INR", BigDecimal.valueOf(1.00));
        RATES.put("USD", BigDecimal.valueOf(84.50));
        RATES.put("EUR", BigDecimal.valueOf(91.80));
        RATES.put("GBP", BigDecimal.valueOf(109.20));
        RATES.put("AED", BigDecimal.valueOf(23.00));
        RATES.put("SGD", BigDecimal.valueOf(64.10));

        SYMBOLS.put("INR", "₹");
        SYMBOLS.put("USD", "$");
        SYMBOLS.put("EUR", "€");
        SYMBOLS.put("GBP", "£");
        SYMBOLS.put("AED", "د.إ");
        SYMBOLS.put("SGD", "S$");
    }

    public Map<String, Object> getSupportedCurrencies() {
        return Map.of(
                "baseCurrency", "INR",
                "ratesAgainstInr", RATES,
                "symbols", SYMBOLS,
                "lastRefreshedAt", LocalDateTime.now()
        );
    }

    /**
     * Converts INR course price to target international currency.
     */
    public Map<String, Object> convertFromInr(BigDecimal inrAmount, String targetCurrency) {
        if (targetCurrency == null || !RATES.containsKey(targetCurrency.toUpperCase())) {
            throw new IllegalArgumentException("Unsupported currency: " + targetCurrency);
        }

        String currency = targetCurrency.toUpperCase();
        BigDecimal rate = RATES.get(currency);
        BigDecimal converted = inrAmount.divide(rate, 2, RoundingMode.HALF_UP);

        return Map.of(
                "originalInrAmount", inrAmount,
                "targetCurrency", currency,
                "currencySymbol", SYMBOLS.get(currency),
                "convertedAmount", converted,
                "exchangeRate", rate,
                "formatted", SYMBOLS.get(currency) + converted
        );
    }
}
