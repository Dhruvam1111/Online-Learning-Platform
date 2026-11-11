package com.olp.dto;

/** Demo bank details; only the last four account digits are persisted. */
public record BankPayoutRequest(String accountNumber, String ifscCode, String bankName) {
    public void validate() {
        if (accountNumber == null || !accountNumber.matches("[0-9]{9,18}")) throw new IllegalArgumentException("Account number must contain 9–18 digits.");
        if (ifscCode == null || !ifscCode.matches("[A-Z]{4}0[A-Z0-9]{6}")) throw new IllegalArgumentException("Enter a valid IFSC format, for example HDFC0001234.");
        if (bankName == null || !bankName.matches("[A-Za-z][A-Za-z .&-]{1,79}")) throw new IllegalArgumentException("Enter a valid bank name.");
    }
}
