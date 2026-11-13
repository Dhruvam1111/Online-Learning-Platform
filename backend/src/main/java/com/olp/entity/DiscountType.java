package com.olp.entity;

public enum DiscountType {
    PERCENTAGE,
    FIXED;

    public static DiscountType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String clean = value.trim().toUpperCase();
        if ("PERCENT".equals(clean) || "PERCENTAGE".equals(clean)) {
            return PERCENTAGE;
        }
        if ("FIXED".equals(clean) || "AMOUNT".equals(clean) || "FIXED_AMOUNT".equals(clean)) {
            return FIXED;
        }
        return DiscountType.valueOf(clean);
    }
}

