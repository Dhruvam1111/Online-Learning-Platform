package com.olp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing platform ledger audit and reconciliation report (FR-PG12, FR-A23).
 * Owned by Dhruvam (Ledger & Reconciliation Engine).
 */
public class ReconciliationReportResponse {

    private long totalOrdersCount;
    private long successfulOrdersCount;
    private long failedOrdersCount;
    private long refundedOrdersCount;

    private BigDecimal grossCapturedVolume;
    private BigDecimal totalRefundedAmount;
    private BigDecimal netPlatformRevenue;
    private BigDecimal totalInstructorLiabilities;
    private BigDecimal totalDiscountsGranted;
    private BigDecimal totalWalletCreditsUsed;

    private int unresolvedDiscrepancies;
    private String status; // 'BALANCED', 'DISCREPANCIES_DETECTED'
    private LocalDateTime generatedAt;
    private List<String> discrepancyDetails;

    public ReconciliationReportResponse() {}

    public ReconciliationReportResponse(long totalOrdersCount, long successfulOrdersCount,
                                        long failedOrdersCount, long refundedOrdersCount,
                                        BigDecimal grossCapturedVolume, BigDecimal totalRefundedAmount,
                                        BigDecimal netPlatformRevenue, BigDecimal totalInstructorLiabilities,
                                        BigDecimal totalDiscountsGranted, BigDecimal totalWalletCreditsUsed,
                                        int unresolvedDiscrepancies, String status,
                                        LocalDateTime generatedAt, List<String> discrepancyDetails) {
        this.totalOrdersCount = totalOrdersCount;
        this.successfulOrdersCount = successfulOrdersCount;
        this.failedOrdersCount = failedOrdersCount;
        this.refundedOrdersCount = refundedOrdersCount;
        this.grossCapturedVolume = grossCapturedVolume;
        this.totalRefundedAmount = totalRefundedAmount;
        this.netPlatformRevenue = netPlatformRevenue;
        this.totalInstructorLiabilities = totalInstructorLiabilities;
        this.totalDiscountsGranted = totalDiscountsGranted;
        this.totalWalletCreditsUsed = totalWalletCreditsUsed;
        this.unresolvedDiscrepancies = unresolvedDiscrepancies;
        this.status = status;
        this.generatedAt = generatedAt;
        this.discrepancyDetails = discrepancyDetails;
    }

    public long getTotalOrdersCount() { return totalOrdersCount; }
    public void setTotalOrdersCount(long totalOrdersCount) { this.totalOrdersCount = totalOrdersCount; }

    public long getSuccessfulOrdersCount() { return successfulOrdersCount; }
    public void setSuccessfulOrdersCount(long successfulOrdersCount) { this.successfulOrdersCount = successfulOrdersCount; }

    public long getFailedOrdersCount() { return failedOrdersCount; }
    public void setFailedOrdersCount(long failedOrdersCount) { this.failedOrdersCount = failedOrdersCount; }

    public long getRefundedOrdersCount() { return refundedOrdersCount; }
    public void setRefundedOrdersCount(long refundedOrdersCount) { this.refundedOrdersCount = refundedOrdersCount; }

    public BigDecimal getGrossCapturedVolume() { return grossCapturedVolume; }
    public void setGrossCapturedVolume(BigDecimal grossCapturedVolume) { this.grossCapturedVolume = grossCapturedVolume; }

    public BigDecimal getTotalRefundedAmount() { return totalRefundedAmount; }
    public void setTotalRefundedAmount(BigDecimal totalRefundedAmount) { this.totalRefundedAmount = totalRefundedAmount; }

    public BigDecimal getNetPlatformRevenue() { return netPlatformRevenue; }
    public void setNetPlatformRevenue(BigDecimal netPlatformRevenue) { this.netPlatformRevenue = netPlatformRevenue; }

    public BigDecimal getTotalInstructorLiabilities() { return totalInstructorLiabilities; }
    public void setTotalInstructorLiabilities(BigDecimal totalInstructorLiabilities) { this.totalInstructorLiabilities = totalInstructorLiabilities; }

    public BigDecimal getTotalDiscountsGranted() { return totalDiscountsGranted; }
    public void setTotalDiscountsGranted(BigDecimal totalDiscountsGranted) { this.totalDiscountsGranted = totalDiscountsGranted; }

    public BigDecimal getTotalWalletCreditsUsed() { return totalWalletCreditsUsed; }
    public void setTotalWalletCreditsUsed(BigDecimal totalWalletCreditsUsed) { this.totalWalletCreditsUsed = totalWalletCreditsUsed; }

    public int getUnresolvedDiscrepancies() { return unresolvedDiscrepancies; }
    public void setUnresolvedDiscrepancies(int unresolvedDiscrepancies) { this.unresolvedDiscrepancies = unresolvedDiscrepancies; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public List<String> getDiscrepancyDetails() { return discrepancyDetails; }
    public void setDiscrepancyDetails(List<String> discrepancyDetails) { this.discrepancyDetails = discrepancyDetails; }
}
