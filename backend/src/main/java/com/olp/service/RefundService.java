package com.olp.service;

import com.olp.dto.RefundRequest;
import com.olp.dto.RefundResponse;
import com.olp.entity.Payment;
import com.olp.entity.Refund;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.EnrollmentRepository;
import com.olp.repository.PaymentRepository;
import com.olp.repository.RefundRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service orchestrating refund transactions and automatic enrollment revocation
 * (FR-PG05, FR-PG06, FR-A24).
 * Owned by Dhruvam (Refund & Audit Engine).
 */
@Service
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    @Autowired
    private PaymentAuditService audit;
    @Autowired(required = false)
    private PaymentSettlementService settlement;
    @Autowired(required = false)
    private RoyaltyRefundService royalties;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentGateway razorpayClient;
    private final EmailService emailService;
    private final WalletService walletService;

    @Autowired
    public RefundService(PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            EnrollmentRepository enrollmentRepository,
            PaymentGateway razorpayClient,
            EmailService emailService,
            @Autowired(required = false) WalletService walletService) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.razorpayClient = razorpayClient;
        this.emailService = emailService;
        this.walletService = walletService;
    }

    public RefundService(PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            EnrollmentRepository enrollmentRepository,
            PaymentGateway razorpayClient,
            EmailService emailService) {
        this(paymentRepository, refundRepository, enrollmentRepository, razorpayClient, emailService, null);
    }

    public RefundService(PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            EnrollmentRepository enrollmentRepository,
            PaymentGateway razorpayClient) {
        this(paymentRepository, refundRepository, enrollmentRepository, razorpayClient, null, null);
    }

    /**
     * Issues full or partial refund via gateway and atomically revokes course
     * access (FR-PG05, NFR-12).
     */
    @Transactional
    public RefundResponse processRefund(User requester, RefundRequest request) {
        return process(requester, request, false);
    }

    @Transactional
    RefundResponse processApprovedException(User requester, RefundRequest request) {
        if (requester == null || !"admin".equalsIgnoreCase(requester.getRole()))
            throw new AccessDeniedException("Administrator required.");
        return process(requester, request, true);
    }

    private RefundResponse process(User requester, RefundRequest request, boolean approvedException) {
        // Bug 5: Anonymous users cannot request refunds
        if (requester == null) {
            throw new AccessDeniedException("Authentication required: Anonymous users cannot request refunds.");
        }

        Payment payment = paymentRepository.findByPaymentIdForUpdate(request.getPaymentId())
                .or(() -> paymentRepository.findByOrderIdForUpdate(request.getPaymentId()))
                .or(() -> paymentRepository.findByPaymentId(request.getPaymentId()))
                .or(() -> paymentRepository.findByOrderId(request.getPaymentId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with reference: " + request.getPaymentId()));

        boolean isAdmin = "admin".equalsIgnoreCase(requester.getRole());
        boolean isOwner = payment.getStudent() != null && payment.getStudent().getId().equals(requester.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Unauthorized: You can only request refunds for your own purchases.");
        }

        if (!"success".equalsIgnoreCase(payment.getStatus())) {
            throw new IllegalArgumentException("Only confirmed successful payments can be refunded.");
        }

        if ("refunded".equalsIgnoreCase(payment.getRefundStatus())
                || RefundPolicy.hasActiveRefund(refundRepository, payment.getId())) {
            throw new IllegalArgumentException("This payment has already been refunded.");
        }

        if (!approvedException)
            RefundPolicy.withinWindow(payment);
        RefundPolicy.reason(request.getReason());

        BigDecimal requestedAmt = RefundPolicy.amount(payment, request.getAmount());
        BigDecimal walletRefundAmt = RefundPolicy.walletShare(payment, requestedAmt);
        BigDecimal gatewayRefundAmt = requestedAmt.subtract(walletRefundAmt);

        if (walletService != null && walletRefundAmt.compareTo(BigDecimal.ZERO) > 0) {
            walletService.creditWallet(payment.getStudent(), walletRefundAmt,
                    "Refund credit for course purchase: " + payment.getCourse().getTitle() + " (Order "
                            + payment.getOrderId() + ")");
            log.info("[Refund] Restored ₹{} wallet credits to Student ID {}", walletRefundAmt,
                    payment.getStudent().getId());
        }

        // 1. Invoke Gateway Refund API (Bug 3: generate unique reference for wallet
        // refunds)
        String refundId;
        if (gatewayRefundAmt.compareTo(BigDecimal.ZERO) > 0 && payment.getPaymentId() != null
                && !payment.getPaymentId().startsWith("TXN-WAL")) {
            refundId = (settlement == null
                    ? razorpayClient.executeRefund(payment.getPaymentId(), gatewayRefundAmt, request.getReason())
                    : razorpayClient.executeRefund(payment.getPaymentId(),
                            settlement.refundProviderAmount(payment, gatewayRefundAmt),
                            settlement.forOrder(payment).get("providerCurrency").toString(), request.getReason()));
        } else {
            refundId = "RFD-WAL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        }

        BigDecimal totalRefunded = gatewayRefundAmt.add(walletRefundAmt);
        if (audit != null)
            audit.record("gateway-refund-" + refundId, "gateway.refund",
                    (settlement == null
                            ? java.util.Map.of("orderId", payment.getOrderId(), "refundId", refundId, "amount",
                                    gatewayRefundAmt, "status", "processed")
                            : settlement.refundRecord(payment, refundId, gatewayRefundAmt)));

        // 2. Persist Refund Record
        Refund refund = new Refund(payment, refundId, totalRefunded, "processed", request.getReason());
        refundRepository.save(refund);

        // 3. Update Payment Audit Fields
        payment.setRefundStatus("refunded");
        payment.setRefundAmount(totalRefunded);
        payment.setRefundReason(request.getReason());
        paymentRepository.save(payment);

        if (royalties != null)
            royalties.adjust(payment, totalRefunded, refundId);

        // 4. Atomic Course Revocation: revoke student's lecture access
        boolean revoked = false;
        if (enrollmentRepository.existsByStudentIdAndCourseId(payment.getStudent().getId(),
                payment.getCourse().getId())) {
            enrollmentRepository.deleteByStudentIdAndCourseId(payment.getStudent().getId(),
                    payment.getCourse().getId());
            revoked = true;
            log.info("[Refund] Revoked course enrollment for Student ID {} in Course ID {}",
                    payment.getStudent().getId(), payment.getCourse().getId());
        }

        log.info("[Refund] Completed refund {} of ₹{} (Gateway: ₹{}, Wallet: ₹{}) for payment {}",
                refundId, totalRefunded, gatewayRefundAmt, walletRefundAmt, payment.getPaymentId());

        // 5. Dispatch Official Refund Confirmation Email (FR-PG05, FR-PG06)
        if (emailService != null && payment.getStudent() != null && payment.getStudent().getEmail() != null) {
            try {
                PaymentService.afterCommit(() -> {
                    emailService.sendRefundConfirmationEmail(
                            payment.getStudent().getEmail(),
                            payment.getStudent().getName(),
                            payment.getCourse().getTitle(),
                            payment.getOrderId(),
                            refundId,
                            totalRefunded,
                            request.getReason());
                });
            } catch (Exception mailEx) {
                log.warn("[Refund] Failed to send email receipt for refund {}: {}", refundId, mailEx.getMessage());
            }
        }

        return new RefundResponse(
                refundId,
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getCourse().getId(),
                payment.getCourse().getTitle(),
                totalRefunded,
                "processed",
                request.getReason(),
                revoked,
                refund.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> getAllRefunds(User admin) {
        if (admin == null || !"admin".equalsIgnoreCase(admin.getRole())) {
            throw new AccessDeniedException("Administrative access required.");
        }
        return refundRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(r -> new RefundResponse(
                        r.getRazorpayRefundId(),
                        r.getPayment().getPaymentId(),
                        r.getPayment().getOrderId(),
                        r.getPayment().getCourse().getId(),
                        r.getPayment().getCourse().getTitle(),
                        r.getAmount(),
                        r.getStatus(),
                        r.getReason(),
                        true,
                        r.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> refundsPage(User user, int page, int size) {
        if (user == null || !"admin".equals(user.getRole()))
            throw new AccessDeniedException("Administrator required.");
        if (page < 0 || page > 1000000 || size < 1 || size > 200)
            throw new IllegalArgumentException("Choose page >=0 and size 1–200.");
        var rows = refundRepository
                .findAllByOrderByCreatedAtDescIdDesc(org.springframework.data.domain.PageRequest.of(page, size));
        var records = rows.getContent().stream()
                .map(r -> new RefundResponse(r.getRazorpayRefundId(), r.getPayment().getPaymentId(),
                        r.getPayment().getOrderId(), r.getPayment().getCourse().getId(),
                        r.getPayment().getCourse().getTitle(), r.getAmount(), r.getStatus(), r.getReason(),
                        "processed".equals(r.getStatus()), r.getCreatedAt()))
                .toList();
        return java.util.Map.of("content", records, "page", page, "size", size, "totalElements",
                rows.getTotalElements(), "totalPages", rows.getTotalPages());
    }
}
