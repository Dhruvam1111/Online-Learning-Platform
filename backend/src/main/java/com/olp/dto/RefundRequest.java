package com.olp.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class RefundRequest {

    @NotBlank(message = "Payment reference is required")
    private String paymentId;

    private BigDecimal amount;

    @NotBlank(message = "Refund reason is required")
    private String reason;

    public RefundRequest() {}

    public RefundRequest(String paymentId, BigDecimal amount, String reason) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.reason = reason;
    }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
