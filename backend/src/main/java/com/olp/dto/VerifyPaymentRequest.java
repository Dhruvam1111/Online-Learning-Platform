package com.olp.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyPaymentRequest {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "Payment ID is required")
    private String paymentId;

    @NotBlank(message = "Payment signature is required")
    private String signature;

    private String paymentMethod = "UPI";

    public VerifyPaymentRequest() {}

    public VerifyPaymentRequest(String orderId, String paymentId, String signature, String paymentMethod) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.signature = signature;
        this.paymentMethod = paymentMethod;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
