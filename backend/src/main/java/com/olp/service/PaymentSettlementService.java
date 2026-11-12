package com.olp.service;
import com.olp.entity.Payment;
import org.springframework.stereotype.Service;
import java.math.*;
import java.util.*;
/** INR accounting with explicitly quoted simulated provider-currency settlement. */
@Service
public class PaymentSettlementService {
    private final CurrencyService currencies;
    private final PaymentAuditService audit;
    public PaymentSettlementService(CurrencyService currencies,PaymentAuditService audit){this.currencies=currencies;this.audit=audit;}
    public Map<String,Object> quote(BigDecimal cash,String requested){
        String currency=requested==null?"INR":requested.toUpperCase(Locale.ROOT).trim();
        var converted=currencies.convertFromInr(cash,currency);
        if(cash.signum()>0 && new BigDecimal(converted.get("convertedAmount").toString()).signum()==0)throw new IllegalArgumentException("The remaining amount is below the selected currency minimum. Choose INR or pay fully with the wallet.");
        BigDecimal rate=new BigDecimal(converted.get("exchangeRate").toString());
        BigDecimal amount=new BigDecimal(converted.get("convertedAmount").toString());
        return Map.of("providerCurrency",currency,"providerAmount",amount,"inrCashAmount",cash,"inrPerUnit",rate,"fxRoundingInr",amount.multiply(rate).subtract(cash).setScale(4,RoundingMode.HALF_UP),"mode","simulation");
    }
    public void record(String order,Map<String,Object> quote){var data=new LinkedHashMap<>(quote);data.put("orderId",order);audit.record("settlement-"+order,"order.settlement",data);}
    public Map<String,Object> forOrder(Payment payment){return audit.exists("settlement-"+payment.getOrderId())?audit.get("settlement-"+payment.getOrderId()):Map.of("providerCurrency","INR","providerAmount",payment.getAmount(),"inrPerUnit",BigDecimal.ONE,"fxRoundingInr",BigDecimal.ZERO);}
    public Map<String,Object> refundRecord(Payment payment,String refundId,BigDecimal inrCash){
        var quote=forOrder(payment);
        return Map.of("orderId",payment.getOrderId(),"refundId",refundId,"amount",inrCash,"status","processed","providerCurrency",quote.get("providerCurrency"),"providerAmount",refundProviderAmount(payment,inrCash));
    }
    public BigDecimal refundProviderAmount(Payment payment,BigDecimal inrRefund){
        var snapshot=forOrder(payment);BigDecimal original=new BigDecimal(snapshot.get("providerAmount").toString());
        return payment.getAmount().signum()==0?BigDecimal.ZERO:original.multiply(inrRefund).divide(payment.getAmount(),2,RoundingMode.HALF_UP).min(original);
    }
}
