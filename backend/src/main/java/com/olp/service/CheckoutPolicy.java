package com.olp.service;
/** Versioned policy shown before every normal checkout and retained with order acceptance. */
public final class CheckoutPolicy {
    private CheckoutPolicy(){}
    public static final String VERSION="refund-2026-11-v1";
    public static final String TEXT="Refunds: request a full or partial refund within 30 days of purchase. Approved refunds restore the original simulated gateway/wallet sources and revoke course access. Simulator refunds are recorded immediately when processed; pending requests await confirmation. Exceptional requests require administrator approval. No real money moves.";
}
