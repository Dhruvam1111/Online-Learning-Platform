package com.olp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class InstructorEarningsResponse {

    private Integer instructorId;
    private String instructorName;
    private BigDecimal lifetimeGrossRevenue;
    private BigDecimal platformCommissionDeducted;
    private BigDecimal netTakeHomeEarnings;
    private BigDecimal unpaidBalance;
    private List<SaleRecord> sales;

    public static class SaleRecord {
        private Integer id;
        private Integer courseId;
        private String courseTitle;
        private String orderId;
        private BigDecimal grossAmount;
        private BigDecimal platformFee;
        private BigDecimal netEarning;
        private String payoutStatus;
        private LocalDateTime soldAt;

        public SaleRecord() {}

        public SaleRecord(Integer id, Integer courseId, String courseTitle, String orderId,
                          BigDecimal grossAmount, BigDecimal platformFee, BigDecimal netEarning,
                          String payoutStatus, LocalDateTime soldAt) {
            this.id = id;
            this.courseId = courseId;
            this.courseTitle = courseTitle;
            this.orderId = orderId;
            this.grossAmount = grossAmount;
            this.platformFee = platformFee;
            this.netEarning = netEarning;
            this.payoutStatus = payoutStatus;
            this.soldAt = soldAt;
        }

        public Integer getId() { return id; }
        public Integer getCourseId() { return courseId; }
        public String getCourseTitle() { return courseTitle; }
        public String getOrderId() { return orderId; }
        public BigDecimal getGrossAmount() { return grossAmount; }
        public BigDecimal getPlatformFee() { return platformFee; }
        public BigDecimal getNetEarning() { return netEarning; }
        public String getPayoutStatus() { return payoutStatus; }
        public LocalDateTime getSoldAt() { return soldAt; }
    }

    public InstructorEarningsResponse() {}

    public InstructorEarningsResponse(Integer instructorId, String instructorName,
                                      BigDecimal lifetimeGrossRevenue,
                                      BigDecimal platformCommissionDeducted,
                                      BigDecimal netTakeHomeEarnings,
                                      BigDecimal unpaidBalance,
                                      List<SaleRecord> sales) {
        this.instructorId = instructorId;
        this.instructorName = instructorName;
        this.lifetimeGrossRevenue = lifetimeGrossRevenue;
        this.platformCommissionDeducted = platformCommissionDeducted;
        this.netTakeHomeEarnings = netTakeHomeEarnings;
        this.unpaidBalance = unpaidBalance;
        this.sales = sales;
    }

    public Integer getInstructorId() { return instructorId; }
    public String getInstructorName() { return instructorName; }
    public BigDecimal getLifetimeGrossRevenue() { return lifetimeGrossRevenue; }
    public BigDecimal getPlatformCommissionDeducted() { return platformCommissionDeducted; }
    public BigDecimal getNetTakeHomeEarnings() { return netTakeHomeEarnings; }
    public BigDecimal getUnpaidBalance() { return unpaidBalance; }
    public List<SaleRecord> getSales() { return sales; }
}
