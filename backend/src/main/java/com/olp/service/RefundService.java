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
 * Service orchestrating refund transactions and automatic enrollment revocation (FR-PG05, FR-PG06, FR-A24).
 * Owned by Dhruvam (Refund & Audit Engine).
 */
@Service
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final RazorpayGatewayClient razorpayClient;
    private final EmailService emailService;
    private final WalletService walletService;

    @Autowired
    public RefundService(PaymentRepository paymentRepository,
                         RefundRepository refundRepository,
                         EnrollmentRepository enrollmentRepository,
                         RazorpayGatewayClient razorpayClient,
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
                         RazorpayGatewayClient razorpayClient,
                         EmailService emailService) {
        this(paymentRepository, refundRepository, enrollmentRepository, razorpayClient, emailService, null);
    }

    public RefundService(PaymentRepository paymentRepository,
                         RefundRepository refundRepository,
                         EnrollmentRepository enrollmentRepository,
                         RazorpayGatewayClient razorpayClient) {
        this(paymentRepository, refundRepository, enrollmentRepository, razorpayClient, null, null);
    }

    /**
     * Issues full or partial refund via gateway and atomically revokes course access (FR-PG05, NFR-12).
     */
    @Transactional
    public RefundResponse processRefund(User requester, RefundRequest request) {
        // Bug 5: Anonymous users cannot request refunds
        if (requester == null) {
            throw new AccessDeniedException("Authentication required: Anonymous users cannot request refunds.");
        }

        Payment payment = paymentRepository.findByPaymentIdForUpdate(request.getPaymentId())
                .or(() -> paymentRepository.findByOrderIdForUpdate(request.getPaymentId()))
                .or(() -> paymentRepository.findByPaymentId(request.getPaymentId()))
                .or(() -> paymentRepository.findByOrderId(request.getPaymentId()))
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with reference: " + request.getPaymentId()));

        boolean isAdmin = "admin".equalsIgnoreCase(requester.getRole());
        boolean isOwner = payment.getStudent() != null && payment.getStudent().getId().equals(requester.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Unauthorized: You can only request refunds for your own purchases.");
        }

        if (!"success".equalsIgnoreCase(payment.getStatus())) {
            throw new IllegalArgumentException("Only confirmed successful payments can be refunded.");
        }

        if ("refunded".equalsIgnoreCase(payment.getRefundStatus()) || refundRepository.existsByPaymentId(payment.getId())) {
            throw new IllegalArgumentException("This payment has already been refunded.");
        }

        BigDecimal walletUsed = payment.getWalletAmountUsed() != null ? payment.getWalletAmountUsed() : BigDecimal.ZERO;
        BigDecimal totalPaid = payment.getAmount().add(walletUsed);

        BigDecimal requestedAmt = (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) > 0)
                ? request.getAmount()
                : totalPaid;

        if (requestedAmt.compareTo(totalPaid) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed original payment amount.");
        }

        // Calculate gateway and wallet refund portions (Bug 4)
        BigDecimal gatewayRefundAmt = requestedAmt.min(payment.getAmount());
        BigDecimal walletRefundAmt = requestedAmt.subtract(gatewayRefundAmt);

        if (walletService != null && walletRefundAmt.compareTo(BigDecimal.ZERO) > 0) {
            walletService.creditWallet(payment.getStudent(), walletRefundAmt,
                    "Refund credit for course purchase: " + payment.getCourse().getTitle() + " (Order " + payment.getOrderId() + ")");
            log.info("[Refund] Restored ₹{} wallet credits to Student ID {}", walletRefundAmt, payment.getStudent().getId());
        }

        // 1. Invoke Gateway Refund API (Bug 3: generate unique reference for wallet refunds)
        String refundId;
        if (gatewayRefundAmt.compareTo(BigDecimal.ZERO) > 0 && payment.getPaymentId() != null && !payment.getPaymentId().startsWith("TXN-WAL")) {
            refundId = razorpayClient.executeRefund(payment.getPaymentId(), gatewayRefundAmt, request.getReason());
        } else {
            refundId = "RFD-WAL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        }

        BigDecimal totalRefunded = gatewayRefundAmt.add(walletRefundAmt);

        // 2. Persist Refund Record
        Refund refund = new Refund(payment, refundId, totalRefunded, "processed", request.getReason());
        refundRepository.save(refund);

        // 3. Update Payment Audit Fields
        payment.setRefundStatus("refunded");
        payment.setRefundAmount(totalRefunded);
        payment.setRefundReason(request.getReason());
        paymentRepository.save(payment);

        // 4. Atomic Course Revocation: revoke student's lecture access
        boolean revoked = false;
        if (enrollmentRepository.existsByStudentIdAndCourseId(payment.getStudent().getId(), payment.getCourse().getId())) {
            enrollmentRepository.deleteByStudentIdAndCourseId(payment.getStudent().getId(), payment.getCourse().getId());
            revoked = true;
            log.info("[Refund] Revoked course enrollment for Student ID {} in Course ID {}",
                    payment.getStudent().getId(), payment.getCourse().getId());
        }

        log.info("[Refund] Completed refund {} of ₹{} (Gateway: ₹{}, Wallet: ₹{}) for payment {}",
                refundId, totalRefunded, gatewayRefundAmt, walletRefundAmt, payment.getPaymentId());

        // 5. Dispatch Official Refund Confirmation Email (FR-PG05, FR-PG06)
        if (emailService != null && payment.getStudent() != null && payment.getStudent().getEmail() != null) {
            try {
                emailService.sendRefundConfirmationEmail(
                        payment.getStudent().getEmail(),
                        payment.getStudent().getName(),
                        payment.getCourse().getTitle(),
                        payment.getOrderId(),
                        refundId,
                        totalRefunded,
                        request.getReason()
                );
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
                refund.getCreatedAt()
        );
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
                        r.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}
