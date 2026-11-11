package com.olp.service;
/** Provider-specific signature protocols stay behind this interface. */
public interface PaymentWebhookAuthenticator {
    String sign(String payload);
    boolean verify(String payload,String header);
}
