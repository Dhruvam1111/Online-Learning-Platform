package com.olp.service;

import com.olp.dto.InstructorEarningsResponse;
import com.olp.entity.InstructorEarning;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.InstructorEarningRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing instructor course royalties, commission accounting, and payouts (FR-PG07, FR-PG08, FR-A25, FR-A26).
 * Owned by Dhruvam (Instructor Royalty & Payout Auditor).
 */
@Service
public class InstructorPayoutService {

    private final InstructorEarningRepository earningRepository;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private boolean payable(InstructorEarning earning) {
        if (entityManager != null) {
            entityManager.find(com.olp.entity.Payment.class, earning.getPayment().getId(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            entityManager.refresh(earning);
            entityManager.refresh(earning.getPayment());
        }
        return eligibleForPayout(earning);
    }

    private boolean eligibleForPayout(InstructorEarning earning) {
        return "success".equals(earning.getPayment().getStatus())
            && !java.util.Set.of("refunded", "pending").contains(earning.getPayment().getRefundStatus() == null ? "" : earning.getPayment().getRefundStatus())
            && java.util.Set.of("unpaid", "processing", "failed", "delayed").contains(earning.getPayoutStatus());
    }

    public InstructorPayoutService(InstructorEarningRepository earningRepository) {
        this.earningRepository = earningRepository;
    }

    /**
     * Retrieves full revenue report and sales ledger for the authenticated instructor.
     */
    @Transactional(readOnly = true)
    public InstructorEarningsResponse getEarningsReport(User instructor) {
        if (instructor == null || (!"instructor".equalsIgnoreCase(instructor.getRole()) && !"admin".equalsIgnoreCase(instructor.getRole()))) {
            throw new AccessDeniedException("Access denied: Instructor or Administrative role required.");
        }

        List<InstructorEarning> earnings = ("instructor".equalsIgnoreCase(instructor.getRole()))
                ? earningRepository.findByInstructorIdOrderByCreatedAtDesc(instructor.getId())
                : earningRepository.findAll();

        BigDecimal lifetimeGross = earnings.stream()
                .map(InstructorEarning::getGrossAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal lifetimeCommission = earnings.stream()
                .map(InstructorEarning::getPlatformCommission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netTakeHome = earnings.stream()
                .map(InstructorEarning::getNetInstructorEarning)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal unpaidBalance = earnings.stream()
                .filter(this::eligibleForPayout)
                .filter(e -> "unpaid".equalsIgnoreCase(e.getPayoutStatus()))
                .map(InstructorEarning::getNetInstructorEarning)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<InstructorEarningsResponse.SaleRecord> sales = earnings.stream()
                .map(e -> new InstructorEarningsResponse.SaleRecord(
                        e.getId(),
                        e.getCourse().getId(),
                        e.getCourse().getTitle(),
                        e.getPayment().getOrderId(),
                        e.getGrossAmount(),
                        e.getPlatformCommission(),
                        e.getNetInstructorEarning(),
                        e.getPayoutStatus(),
                        e.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return new InstructorEarningsResponse(
                instructor != null ? instructor.getId() : 1,
                instructor != null ? instructor.getName() : "Instructor",
                lifetimeGross,
                lifetimeCommission,
                netTakeHome,
                unpaidBalance,
                sales
        );
    }

    /**
     * Admin view of all instructor earnings and payouts across the platform.
     */
    @Transactional(readOnly = true)
    public List<InstructorEarningsResponse.SaleRecord> getAllEarningsForAdmin(User admin) {
        if (admin == null || !"admin".equalsIgnoreCase(admin.getRole())) {
            throw new AccessDeniedException("Administrative privileges required.");
        }
        return earningRepository.findAll().stream()
                .map(e -> new InstructorEarningsResponse.SaleRecord(
                        e.getId(),
                        e.getCourse().getId(),
                        e.getCourse().getTitle(),
                        e.getPayment().getOrderId(),
                        e.getGrossAmount(),
                        e.getPlatformCommission(),
                        e.getNetInstructorEarning(),
                        e.getPayoutStatus(),
                        e.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Admin action to settle instructor payouts (FR-A26).
     */
    @Transactional
    public void settleEarningPayout(User admin, Integer earningId) {
        if (admin == null || !"admin".equalsIgnoreCase(admin.getRole())) {
            throw new AccessDeniedException("Administrative privileges required to settle payouts.");
        }

        InstructorEarning earning = earningRepository.findById(earningId)
                .orElseThrow(() -> new ResourceNotFoundException("Earning record not found: " + earningId));

        if ("paid".equals(earning.getPayoutStatus())) return;
        if (!payable(earning)) throw new IllegalStateException("This payout is not eligible for settlement.");
        earning.setPayoutStatus("paid");
        earningRepository.save(earning);
    }

    /**
     * Admin action to halt an instructor payout in case of disputes or policy reviews (US-A26).
     */
    @Transactional
    public void haltEarningPayout(User admin, Integer earningId, String reason) {
        if (admin == null || !"admin".equalsIgnoreCase(admin.getRole())) {
            throw new AccessDeniedException("Administrative privileges required to halt payouts.");
        }

        InstructorEarning earning = earningRepository.findById(earningId)
                .orElseThrow(() -> new ResourceNotFoundException("Earning record not found: " + earningId));

        earning.setPayoutStatus("halted");
        earningRepository.save(earning);
    }

    /**
     * Admin action to release/unhalt a previously disputed instructor payout (US-A26).
     */
    @Transactional
    public void unhaltEarningPayout(User admin, Integer earningId) {
        if (admin == null || !"admin".equalsIgnoreCase(admin.getRole())) {
            throw new AccessDeniedException("Administrative privileges required to release payouts.");
        }

        InstructorEarning earning = earningRepository.findById(earningId)
                .orElseThrow(() -> new ResourceNotFoundException("Earning record not found: " + earningId));

        earning.setPayoutStatus("unpaid");
        earningRepository.save(earning);
    }

    /**
     * Viva Demo Simulation: Simulates direct bank transfer (IMPS/NEFT) payout to the instructor.
     */
    @Transactional
    public java.util.Map<String, Object> withdrawAllEarningsForInstructor(User instructor) {
        if (instructor == null || !"instructor".equalsIgnoreCase(instructor.getRole())) {
            throw new AccessDeniedException("Access denied: Instructor role required to withdraw earnings.");
        }

        List<InstructorEarning> unpaidEarnings = earningRepository.findByInstructorIdOrderByCreatedAtDesc(instructor.getId())
                .stream()
                .filter(e -> "unpaid".equalsIgnoreCase(e.getPayoutStatus()))
                .collect(Collectors.toList());

        unpaidEarnings.removeIf(e -> !payable(e));

        BigDecimal totalSettled = unpaidEarnings.stream()
                .map(InstructorEarning::getNetInstructorEarning)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (InstructorEarning earning : unpaidEarnings) {
            earning.setPayoutStatus("paid");
            earningRepository.save(earning);
        }

        long ref = (long) (Math.random() * 900000000000L + 100000000000L);
        String impsRef = "IMPS-" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + ref;

        return java.util.Map.of(
                "success", true,
                "amountWithdrawn", totalSettled,
                "itemsSettled", unpaidEarnings.size(),
                "utrNumber", impsRef,
                "bankName", "HDFC Bank (A/C •••• 4892)",
                "ifscCode", "HDFC0001234",
                "recipientName", instructor != null ? instructor.getName() : "Dr. Sarah Chen",
                "message", "Direct bank transfer simulated successfully! Payout credited to your linked account."
        );
    }
}
