package com.olp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity tracking instructor sales royalties, platform commissions, and payout ledger (FR-PG07, FR-PG08, FR-A25).
 */
@Entity
@Table(name = "instructor_earnings")
public class InstructorEarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "gross_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "platform_commission", nullable = false, precision = 10, scale = 2)
    private BigDecimal platformCommission;

    @Column(name = "net_instructor_earning", nullable = false, precision = 10, scale = 2)
    private BigDecimal netInstructorEarning;

    @Column(name = "payout_status", nullable = false, length = 20)
    private String payoutStatus = "unpaid"; // 'unpaid', 'processing', 'paid'

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.payoutStatus == null) {
            this.payoutStatus = "unpaid";
        }
    }

    public InstructorEarning() {}

    public InstructorEarning(User instructor, Course course, Payment payment,
                             BigDecimal grossAmount, BigDecimal platformCommission,
                             BigDecimal netInstructorEarning) {
        this.instructor = instructor;
        this.course = course;
        this.payment = payment;
        this.grossAmount = grossAmount;
        this.platformCommission = platformCommission;
        this.netInstructorEarning = netInstructorEarning;
        this.payoutStatus = "unpaid";
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public User getInstructor() { return instructor; }
    public void setInstructor(User instructor) { this.instructor = instructor; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }

    public BigDecimal getPlatformCommission() { return platformCommission; }
    public void setPlatformCommission(BigDecimal platformCommission) { this.platformCommission = platformCommission; }

    public BigDecimal getNetInstructorEarning() { return netInstructorEarning; }
    public void setNetInstructorEarning(BigDecimal netInstructorEarning) { this.netInstructorEarning = netInstructorEarning; }

    public String getPayoutStatus() { return payoutStatus; }
    public void setPayoutStatus(String payoutStatus) { this.payoutStatus = payoutStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
