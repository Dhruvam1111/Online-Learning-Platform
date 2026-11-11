package com.olp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Local payment reference generator — replaces external payment gateway client.
 * No external SDK required; generates unique order and payment reference IDs.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="payments.provider",havingValue="simulation",matchIfMissing=true)
public class RazorpayGatewayClient implements PaymentGateway {

    @org.springframework.beans.factory.annotation.Autowired(required=false) private PaymentAuditService journal;
    private static final Logger log = LoggerFactory.getLogger(RazorpayGatewayClient.class);

    /**
     * Generates a unique local order reference ID.
     */
    public String createGatewayOrder(BigDecimal amount, String currency, String receipt, String courseTitle) {
        String orderId = "OLP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        log.info("[Payment] Generated order reference: {} for '{}' ({}{})", orderId, courseTitle, currency, amount);
        return orderId;
    }

    /**
     * Generates a local refund reference ID.
     */
    public String executeRefund(String paymentId, BigDecimal amount, String reason) {
        String refundId = "RFD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        log.info("[Payment] Generated refund reference: {} for payment {} (reason: {})", refundId, paymentId, reason);
        return refundId;
    }

    /**
     * Returns a placeholder key (no external gateway configured).
     */
    public String getKeyId() {
        return "olp-qr-payment";
    }
    @Override public String createMethodToken(String method){
        if(!java.util.Set.of("card","upi","netbanking","wallet").contains(method))throw new IllegalArgumentException("Unsupported payment method.");
        return "simtok_"+UUID.randomUUID().toString().replace("-","");
    }
    @Override public String linkBankAccount(com.olp.dto.BankPayoutRequest bank,String holderName){bank.validate();return "sim_bank_"+UUID.randomUUID();}
    @Override public String transferPayout(String token,BigDecimal amount,String currency,String idempotencyKey){
        if(token==null || !token.startsWith("sim_bank_") || amount==null || amount.signum()<0 || !"INR".equals(currency))throw new IllegalArgumentException("Invalid simulated bank payout.");
        return "IMPS-SIM-"+UUID.nameUUIDFromBytes(idempotencyKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    @Override public java.util.List<java.util.Map<String,Object>> captureReports(){return journal==null?java.util.List.of():journal.list("gateway.capture");}
    @Override public java.util.List<java.util.Map<String,Object>> refundReports(){return journal==null?java.util.List.of():journal.list("gateway.refund");}
    @Override public String reverseLateCapture(String order,BigDecimal amount,String currency){
        return "RFD-LATE-"+UUID.nameUUIDFromBytes(order.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
