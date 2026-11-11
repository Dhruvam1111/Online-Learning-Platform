package com.olp.service;
import java.math.BigDecimal;
/** Provider boundary. Implementations must return stable provider references and never accept raw card data. */
public interface PaymentGateway {
    String createGatewayOrder(BigDecimal amount, String currency, String receipt, String courseTitle);
    String executeRefund(String paymentId, BigDecimal amount, String reason);
    default String executeRefund(String paymentId,BigDecimal amount,String currency,String reason){return executeRefund(paymentId,amount,reason);}
    String getKeyId();
    String createMethodToken(String method);
    String reverseLateCapture(String orderId,BigDecimal amount,String currency);
    String linkBankAccount(com.olp.dto.BankPayoutRequest bank,String holderName);
    String transferPayout(String providerAccountToken,BigDecimal amount,String currency,String idempotencyKey);
    java.util.List<java.util.Map<String,Object>> captureReports();
    java.util.List<java.util.Map<String,Object>> refundReports();
}
