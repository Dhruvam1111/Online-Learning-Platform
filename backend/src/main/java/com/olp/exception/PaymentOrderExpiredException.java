package com.olp.exception;
/** Expiration commits the release of reserved wallet funds. Other failures roll back. */
public class PaymentOrderExpiredException extends IllegalStateException {
    public PaymentOrderExpiredException(String message) { super(message); }
}
