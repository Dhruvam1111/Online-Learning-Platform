package com.olp.dto;

import java.math.BigDecimal;

/**
 * Extensible grading policy configuration (FR-I22).
 * Provides clear integration hooks for future negative marking, partial marking (FR-I65),
 * and custom tolerance overrides without altering core auto-evaluation logic.
 */
public class GradingPolicy {

    private boolean enableNegativeMarking = false;
    private boolean enablePartialMarking = false;
    private BigDecimal customTolerance;

    public GradingPolicy() {}

    public GradingPolicy(boolean enableNegativeMarking, boolean enablePartialMarking, BigDecimal customTolerance) {
        this.enableNegativeMarking = enableNegativeMarking;
        this.enablePartialMarking = enablePartialMarking;
        this.customTolerance = customTolerance;
    }

    public static GradingPolicy defaultPolicy() {
        return new GradingPolicy(false, false, null);
    }

    public static GradingPolicy withNegativeMarking(boolean enableNegativeMarking) {
        GradingPolicy policy = new GradingPolicy();
        policy.setEnableNegativeMarking(enableNegativeMarking);
        return policy;
    }

    public static GradingPolicy withPartialMarking(boolean enablePartialMarking) {
        GradingPolicy policy = new GradingPolicy();
        policy.setEnablePartialMarking(enablePartialMarking);
        return policy;
    }

    public boolean isEnableNegativeMarking() {
        return enableNegativeMarking;
    }

    public void setEnableNegativeMarking(boolean enableNegativeMarking) {
        this.enableNegativeMarking = enableNegativeMarking;
    }

    public boolean isEnablePartialMarking() {
        return enablePartialMarking;
    }

    public void setEnablePartialMarking(boolean enablePartialMarking) {
        this.enablePartialMarking = enablePartialMarking;
    }

    public BigDecimal getCustomTolerance() {
        return customTolerance;
    }

    public void setCustomTolerance(BigDecimal customTolerance) {
        this.customTolerance = customTolerance;
    }
}

