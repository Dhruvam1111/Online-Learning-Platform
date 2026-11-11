package com.olp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing dynamic NPCI UPI QR code and deep-link payload (FR-PG01, FR-PG03).
 * Owned by Shubh (Digital Payment Instruments).
 */
public class UpiQrResponse {

    private String orderId;
    private String upiUri;
    private String qrBase64;
    private BigDecimal amount;
    private String currency;
    private String payeeVpa;
    private String payeeName;
    private LocalDateTime expiresAt;

    public UpiQrResponse() {}

    public UpiQrResponse(String orderId, String upiUri, String qrBase64,
                          BigDecimal amount, String currency, String payeeVpa,
                          String payeeName, LocalDateTime expiresAt) {
        this.orderId = orderId;
        this.upiUri = upiUri;
        this.qrBase64 = qrBase64;
        this.amount = amount;
        this.currency = currency;
        this.payeeVpa = payeeVpa;
        this.payeeName = payeeName;
        this.expiresAt = expiresAt;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUpiUri() { return upiUri; }
    public void setUpiUri(String upiUri) { this.upiUri = upiUri; }

    public String getQrBase64() { return qrBase64; }
    public void setQrBase64(String qrBase64) { this.qrBase64 = qrBase64; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPayeeVpa() { return payeeVpa; }
    public void setPayeeVpa(String payeeVpa) { this.payeeVpa = payeeVpa; }

    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
