package com.olp.service;

import com.olp.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/** Display-only simulated FX feed. Checkout always settles in INR. */
@Service
public class CurrencyService {
    @Autowired private PaymentAuditService audit;
    private static final Map<String,BigDecimal> DEFAULTS=Map.of("INR",BigDecimal.ONE,"USD",new BigDecimal("84.50"),"EUR",new BigDecimal("91.80"),"GBP",new BigDecimal("109.20"),"AED",new BigDecimal("23.00"),"SGD",new BigDecimal("64.10"));
    private static final Map<String,String> SYMBOLS=Map.of("INR","₹","USD","$","EUR","€","GBP","£","AED","د.إ","SGD","S$");
    private Map<String,BigDecimal> rates() {
        Map<String,BigDecimal> rates=new TreeMap<>(DEFAULTS);
        if (audit!=null) for (var row:audit.list("currency.rate")) rates.put(row.get("currency").toString(),new BigDecimal(row.get("rate").toString()));
        return rates;
    }
    public Map<String,Object> getSupportedCurrencies() {
        var changes=audit==null?List.<Map<String,Object>>of():audit.list("currency.rate");
        String asOf=changes.isEmpty()?"2026-11-01T00:00:00":changes.get(changes.size()-1).get("recordedAt").toString();
        return Map.of("baseCurrency","INR","ratesAgainstInr",rates(),"symbols",SYMBOLS,"lastRefreshedAt",asOf,"mode","simulation","rateSource","Simulated FX feed; not live market rates");
    }
    public Map<String,Object> convertFromInr(BigDecimal inrAmount,String targetCurrency) {
        if(inrAmount==null || inrAmount.signum()<0) throw new IllegalArgumentException("Amount must be nonnegative.");
        String currency=targetCurrency==null?"":targetCurrency.toUpperCase(Locale.ROOT);
        BigDecimal rate=rates().get(currency);
        if(rate==null) throw new IllegalArgumentException("Unsupported currency: "+targetCurrency);
        BigDecimal converted=inrAmount.divide(rate,2,RoundingMode.HALF_UP);
        return Map.of("originalInrAmount",inrAmount,"targetCurrency",currency,"currencySymbol",SYMBOLS.get(currency),"convertedAmount",converted,"exchangeRate",rate,"formatted",SYMBOLS.get(currency)+converted,"mode","simulation");
    }
    @Transactional
    public Map<String,Object> updateRate(User admin,String currency,BigDecimal rate) {
        if(admin==null || !"admin".equalsIgnoreCase(admin.getRole())) throw new AccessDeniedException("Administrator required.");
        currency=currency==null?"":currency.toUpperCase(Locale.ROOT);
        if(!DEFAULTS.containsKey(currency) || "INR".equals(currency) || rate==null || rate.signum()<=0 || rate.compareTo(new BigDecimal("1000000"))>0 || rate.stripTrailingZeros().scale()>6) throw new IllegalArgumentException("Choose a foreign currency and a positive rate with up to six decimal places.");
        audit.record("fx-"+UUID.randomUUID(),"currency.rate",Map.of("currency",currency,"rate",rate,"actorId",admin.getId()));
        return getSupportedCurrencies();
    }
}
