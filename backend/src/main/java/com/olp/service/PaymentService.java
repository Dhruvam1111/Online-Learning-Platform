package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service orchestrating the Payment Gateway lifecycle.
 * Implements:
 * - Order initialization with QR-based payment flow (FR-PG01, FR-PG03)
 * - Atomic payment-to-enrollment guarantee preventing ghost enrollments
 * (FR-PG04, NFR-12)
 * - Automated instructor revenue split (FR-PG07, FR-A25)
 * - Financial audit logging and ledger reporting (FR-A23, NFR-33)
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final BigDecimal DEFAULT_COURSE_PRICE = BigDecimal.valueOf(499);
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private PaymentPolicyService policies;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private PaymentGateway gateway;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private FraudDetectionService fraud;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private PaymentSettlementService settlement;

    @org.springframework.beans.factory.annotation.Autowired
    private PaymentAuditService audit;
    private final PaymentRepository paymentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final InstructorEarningRepository instructorEarningRepository;
    private final CouponService couponService;
    private final EmailService emailService;
    private final InvoiceService invoiceService;
    private final UserRepository userRepository;
    private final WalletService walletService;

    public static final int ORDER_EXPIRY_MINUTES = 15;

    @org.springframework.beans.factory.annotation.Autowired
    public PaymentService(PaymentRepository paymentRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            InstructorEarningRepository instructorEarningRepository,
            CouponService couponService,
            EmailService emailService,
            InvoiceService invoiceService,
            UserRepository userRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) WalletService walletService) {
        this.paymentRepository = paymentRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.instructorEarningRepository = instructorEarningRepository;
        this.couponService = couponService;
        this.emailService = emailService;
        this.invoiceService = invoiceService;
        this.userRepository = userRepository;
        this.walletService = walletService;
    }

    public PaymentService(PaymentRepository paymentRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            InstructorEarningRepository instructorEarningRepository,
            CouponService couponService,
            EmailService emailService,
            InvoiceService invoiceService,
            UserRepository userRepository) {
        this(paymentRepository, courseRepository, enrollmentRepository, instructorEarningRepository,
                couponService, emailService, invoiceService, userRepository, null);
    }

    /**
     * Initializes a new payment order for the specified course (FR-PG01, FR-PG03).
     * Creates a pending payment record and returns the order details for QR
     * generation.
     */
    @Transactional
    public PaymentOrderResponse createPaymentOrder(User student, CreatePaymentOrderRequest request) {
        if (student == null || !java.util.Set.of("student", "admin").contains(student.getRole()))
            throw new AccessDeniedException("Sign in as a student to purchase a course.");

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getCourseId()));

        if (course.getStatus() != CourseStatus.PUBLISHED)
            throw new IllegalStateException("This course is not currently available for purchase.");

        // FR-I10: Prevent enrollment when course enrollment limit is reached
        if (course.getEnrollmentLimit() != null && course.getEnrollmentLimit() > 0) {
            long currentCount = enrollmentRepository.countByCourseId(course.getId());
            if (currentCount >= course.getEnrollmentLimit()) {
                throw new IllegalStateException("Course enrollment limit has been reached.");
            }
        }

        log.info("[Payment Gateway] Initializing checkout order for student '{}' on course '{}' (ID: {})",
                student != null ? student.getEmail() : "demo", course.getTitle(), course.getId());

        BigDecimal basePrice = (policies == null ? catalogPrice(course) : policies.price(course)).setScale(2);

        String currency = (request.getCurrency() != null && !request.getCurrency().isBlank())
                ? request.getCurrency().toUpperCase().trim()
                : "INR";

        if (!"INR".equals(currency))
            throw new IllegalArgumentException(
                    "Checkout settles in INR; foreign currencies are display estimates only.");

        // Promotional Coupon Validation & Discount Deduction (FR-A25)
        BigDecimal discountAmount = BigDecimal.ZERO;
        String couponCode = null;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            Map<String, Object> couponResult = couponService.validateAndApply(request.getCouponCode(), basePrice,
                    student);
            discountAmount = (BigDecimal) couponResult.get("discountAmount");
            couponCode = (String) couponResult.get("couponCode");
        } else if (course.getDiscountType() != null && course.getDiscountValue() != null &&
                course.getDiscountValue().compareTo(BigDecimal.ZERO) > 0) {
            // Apply course-level discount if active (FR-I10)
            LocalDateTime now = LocalDateTime.now();
            boolean validStart = course.getDiscountStartDate() == null || !now.isBefore(course.getDiscountStartDate());
            boolean validEnd = course.getDiscountEndDate() == null || !now.isAfter(course.getDiscountEndDate());
            if (validStart && validEnd) {
                if (course.getDiscountType() == com.olp.entity.DiscountType.PERCENTAGE) {
                    discountAmount = basePrice.multiply(course.getDiscountValue())
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                            .min(basePrice);
                } else if (course.getDiscountType() == com.olp.entity.DiscountType.FIXED) {
                    discountAmount = course.getDiscountValue().min(basePrice);
                }
            }
        }

        BigDecimal finalPayable = basePrice.subtract(discountAmount).max(BigDecimal.ZERO);

        // Virtual Wallet Credit Deduction (FR-PG02)
        BigDecimal walletUsed = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(request.getUseWallet()) && student != null && walletService != null) {
            walletUsed = walletService.debitWallet(student, finalPayable,
                    "Applied to course purchase: " + course.getTitle());
            finalPayable = finalPayable.subtract(walletUsed);
        }

        Map<String, Object> quoted = settlement == null
                ? Map.of("providerCurrency", "INR", "providerAmount", finalPayable, "fxRoundingInr", BigDecimal.ZERO)
                : settlement.quote(finalPayable, request.getSettlementCurrency());
        // Generate a unique order reference
        String orderId = gateway == null
                ? "OLP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase()
                : gateway.createGatewayOrder((BigDecimal) quoted.get("providerAmount"),
                        quoted.get("providerCurrency").toString(), "course-" + course.getId(), course.getTitle());
        if (orderId == null || orderId.isBlank())
            throw new IllegalStateException("Payment provider did not return an order reference. Please retry later.");
        if (settlement != null)
            settlement.record(orderId, quoted);
        if (policies != null)
            policies.snapshot(orderId, course);
        if (request.getPolicyAcceptedVersion() != null) {
            if (!CheckoutPolicy.VERSION.equals(request.getPolicyAcceptedVersion()))
                throw new IllegalArgumentException("Review the current refund policy.");
            if (audit != null)
                audit.record("accepted-policy-" + orderId, "order.policy.accepted",
                        Map.of("orderId", orderId, "studentId", student.getId(), "courseId", course.getId(),
                                "policyVersion", CheckoutPolicy.VERSION, "policyText", CheckoutPolicy.TEXT));
        }

        Payment payment = new Payment(orderId, student, course, finalPayable, currency);
        payment.setStatus("created");
        if (fraud != null) {
            var risk = fraud.evaluateRisk(student, finalPayable.add(walletUsed));
            payment.setRiskScore(risk.getScore());
            if (audit != null)
                audit.record("risk-" + orderId, "order.risk", Map.of("orderId", orderId, "score", risk.getScore(),
                        "level", risk.getLevel(), "factors", risk.getFlaggedFactors()));
        }

        payment.setCouponCode(couponCode);
        payment.setDiscountAmount(discountAmount);
        payment.setWalletAmountUsed(walletUsed);
        if (request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank()) {
            payment.setPaymentMethod(normalizePaymentMethod(request.getPaymentMethod()));
        }

        paymentRepository.save(payment);
        payment.setInvoiceNumber("INV-" + String.format(java.util.Locale.ROOT, "%010d", payment.getId()));

        // If wallet covers 100% of the cost, complete settlement instantly
        if (finalPayable.compareTo(BigDecimal.ZERO) == 0) {
            long randomDigits = (long) (Math.random() * 900000000000L + 100000000000L);
            String transactionId = "TXN-WAL-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + "-" + randomDigits;
            payment.setPaymentId(transactionId);
            payment.setPaymentMethod("Wallet");
            paymentRepository.save(payment);
            processSuccessfulSettlement(payment, student);
            log.info("[Payment] Order {} fully paid via student wallet credits.", orderId);
        } else {
            paymentRepository.save(payment);
        }

        log.info("[Payment] Order created: {} for Course '{}' (Base: ₹{}, Discount: ₹{}, Wallet: ₹{}, Final: ₹{})",
                orderId, course.getTitle(), basePrice, discountAmount, walletUsed, finalPayable);

        PaymentOrderResponse response = new PaymentOrderResponse(
                orderId,
                finalPayable,
                finalPayable.movePointRight(2).longValueExact(),
                currency,
                course.getId(),
                course.getTitle(),
                null,
                student.getName(),
                student.getEmail());
        BigDecimal taxRate = policies == null ? new BigDecimal("0.18")
                : policies.value(policies.forOrder(orderId), "taxRate");
        BigDecimal total = finalPayable.add(walletUsed);
        response.setSettlementCurrency(quoted.get("providerCurrency").toString());
        response.setSettlementAmount((BigDecimal) quoted.get("providerAmount"));
        response.setFxRoundingInr((BigDecimal) quoted.get("fxRoundingInr"));
        response.setTaxRate(taxRate);
        response.setTotalAmount(total);
        response.setTaxAmount(total.subtract(total.divide(BigDecimal.ONE.add(taxRate), 2, RoundingMode.HALF_UP)));
        response.setRefundPolicy(CheckoutPolicy.TEXT);
        response.setRefundPolicyVersion(CheckoutPolicy.VERSION);
        response.setRiskScore(payment.getRiskScore());
        response.setDiscountAmount(discountAmount);
        response.setCouponCode(couponCode);
        response.setWalletAmountUsed(walletUsed);
        if ("success".equalsIgnoreCase(payment.getStatus())) {
            response.setStatus("success");
        }
        return response;
    }

    // Courses have no persisted price column. Use the existing catalog tiers on the
    // server.
    static BigDecimal catalogPrice(Course course) {
        if (course != null && course.getPrice() != null && course.getPrice().compareTo(BigDecimal.ZERO) >= 0) {
            return course.getPrice();
        }
        if (course == null) return DEFAULT_COURSE_PRICE;
        String title = course.getTitle() == null ? "" : course.getTitle().toLowerCase(java.util.Locale.ROOT);
        Integer id = course.getId();
        if (title.contains("machine learning") || title.contains("ai") || title.contains("deep learning")
                || Integer.valueOf(4).equals(id))
            return BigDecimal.valueOf(899);
        if (title.contains("spring") || title.contains("java") || title.contains("backend")
                || Integer.valueOf(5).equals(id))
            return BigDecimal.valueOf(799);
        if (title.contains("database") || title.contains("sql") || title.contains("postgres")
                || Integer.valueOf(2).equals(id))
            return BigDecimal.valueOf(699);
        return DEFAULT_COURSE_PRICE;
    }

    public boolean isOrderExpired(Payment payment) {
        return payment != null
                && "created".equalsIgnoreCase(payment.getStatus())
                && payment.getCreatedAt() != null
                && payment.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(ORDER_EXPIRY_MINUTES));
    }

    /**
     * Confirms a successful QR payment and atomically enrolls the student (FR-PG04,
     * NFR-12).
     * Called when the student's payment is confirmed (simulated or real).
     */
    @Transactional(noRollbackFor = com.olp.exception.PaymentOrderExpiredException.class)
    public PaymentTransactionResponse confirmPayment(String orderId) {
        return confirmPayment(orderId, null);
    }

    @Transactional(noRollbackFor = com.olp.exception.PaymentOrderExpiredException.class)
    public PaymentTransactionResponse confirmPayment(String orderId, String paymentMethod) {
        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)

                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found: " + orderId));

        // Ownership enforcement: If an authenticated user is present in the context,
        // verify order ownership
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null
                && !"anonymousUser".equals(auth.getPrincipal())) {
            assertPaymentOwnership(auth, payment);
        }

        // Idempotency: if already processed, return existing record
        if ("success".equalsIgnoreCase(payment.getStatus())) {
            return mapToResponse(payment);
        }

        // Bug 2: Failed or expired orders cannot later be confirmed
        if ("failed".equalsIgnoreCase(payment.getStatus())) {
            throw new IllegalStateException(
                    "Payment order " + orderId + " has already failed and cannot be confirmed.");
        }
        if ("expired".equalsIgnoreCase(payment.getStatus())) {
            throw new IllegalStateException(
                    "Payment order " + orderId + " has already expired and cannot be confirmed.");
        }

        if (!"created".equalsIgnoreCase(payment.getStatus()))
            throw new IllegalStateException("This order cannot be confirmed.");

        // Automatic Order Expiry check
        if (isOrderExpired(payment)) {
            payment.setStatus("expired");
            payment.setFailureReason("Order expired after " + ORDER_EXPIRY_MINUTES + " minutes without completion.");
            if (payment.getWalletAmountUsed() != null && payment.getWalletAmountUsed().compareTo(BigDecimal.ZERO) > 0
                    && walletService != null) {
                BigDecimal refundAmt = payment.getWalletAmountUsed();
                payment.setWalletAmountUsed(BigDecimal.ZERO);
                walletService.creditWallet(payment.getStudent(), refundAmt,
                        "Refund for expired order " + payment.getOrderId());
            }
            paymentRepository.save(payment);
            throw new com.olp.exception.PaymentOrderExpiredException(
                    "Payment order " + orderId + " has expired. Please initiate a new order.");
        }

        // Generate realistic arbitrary banking / UPI transaction reference ID
        long randomDigits = (long) (Math.random() * 900000000000L + 100000000000L);
        String transactionId = "TXN-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-"
                + randomDigits;
        payment.setPaymentId(transactionId);
        payment.setPaymentMethod(resolvePaymentMethod(payment, paymentMethod));

        processSuccessfulSettlement(payment, payment.getStudent());

        log.info("[Payment] Confirmed and enrolled for Order {}", orderId);
        return mapToResponse(payment);
    }

    /**
     * Marks a payment as failed and ensures no enrollment is created (FR-PG04).
     * System returns to original state — course remains unenrolled.
     */
    @Transactional
    public PaymentTransactionResponse failPayment(String orderId) {
        return closeUnpaidOrder(orderId, false);
    }

    @Transactional
    public PaymentTransactionResponse cancelPayment(String orderId) {
        return closeUnpaidOrder(orderId, true);
    }

    private PaymentTransactionResponse closeUnpaidOrder(String orderId, boolean cancelled) {
        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)

                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found: " + orderId));

        // Bug 1: Repeated failure requests must not credit wallet twice or re-fail
        if ("success".equalsIgnoreCase(payment.getStatus())
                || "failed".equalsIgnoreCase(payment.getStatus())
                || "expired".equalsIgnoreCase(payment.getStatus())) {
            return mapToResponse(payment);
        }

        if (!"created".equalsIgnoreCase(payment.getStatus()))
            return mapToResponse(payment);

        payment.setStatus("failed");
        payment.setFailureReason(cancelled ? "Checkout was cancelled." : "Payment was not completed.");
        if (payment.getWalletAmountUsed() != null && payment.getWalletAmountUsed().compareTo(BigDecimal.ZERO) > 0
                && walletService != null) {
            BigDecimal refundAmt = payment.getWalletAmountUsed();
            payment.setWalletAmountUsed(BigDecimal.ZERO); // Zero out to prevent duplicate credit
            walletService.creditWallet(payment.getStudent(), refundAmt,
                    "Refund for failed order " + payment.getOrderId());
        }
        paymentRepository.save(payment);

        // Replacing or closing checkout releases funds without sending a decline email.
        if (cancelled)
            return mapToResponse(payment);

        // Notify student via email that payment attempt failed (no charges made)
        try {
            emailService.sendPaymentFailedEmail(
                    payment.getStudent().getEmail(),
                    payment.getStudent().getName(),
                    payment.getCourse().getTitle(),
                    payment.getOrderId(),
                    payment.getFailureReason());
        } catch (Exception e) {
            log.warn("[Payment Email] Could not send failure email: {}", e.getMessage());
        }

        log.info("[Payment] Order {} marked as failed — no enrollment created.", orderId);
        return mapToResponse(payment);
    }

    /**
     * Completes post-payment accounting: revenue split, instructor royalty credit,
     * and atomic course enrollment.
     */
    private void processSuccessfulSettlement(Payment payment, User student) {
        if (audit != null) {
            var capture = new java.util.LinkedHashMap<String, Object>();
            capture.put("orderId", payment.getOrderId());
            capture.put("amount", payment.getAmount());
            capture.put("status", "success");
            capture.put("currency", payment.getCurrency());
            if (settlement != null)
                capture.putAll(settlement.forOrder(payment));
            audit.record("gateway-capture-" + payment.getOrderId(), "gateway.capture", capture);
        }
        payment.setStatus("success");

        // Calculate Revenue Split on total course value including wallet credits
        // (FR-PG07, FR-A25)
        BigDecimal walletPaid = payment.getWalletAmountUsed() != null ? payment.getWalletAmountUsed() : BigDecimal.ZERO;
        BigDecimal gross = payment.getAmount().add(walletPaid);
        BigDecimal platformCommission = gross
                .multiply(policies == null ? new BigDecimal("0.20")
                        : policies.value(policies.forOrder(payment.getOrderId()), "commissionRate"))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal netInstructor = gross.subtract(platformCommission);

        payment.setPlatformFee(platformCommission);
        payment.setInstructorEarning(netInstructor);
        paymentRepository.save(payment);

        // Record Instructor Earning
        User instructor = payment.getCourse().getInstructor();
        if (instructor != null && !instructorEarningRepository.existsByPaymentId(payment.getId())) {
            InstructorEarning earning = new InstructorEarning(
                    instructor,
                    payment.getCourse(),
                    payment,
                    gross,
                    platformCommission,
                    netInstructor);
            instructorEarningRepository.save(earning);
        }

        // Atomic Course Enrollment (FR-PG04, NFR-12)
        if (!enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), payment.getCourse().getId())) {
            if (payment.getCourse().getEnrollmentLimit() != null && payment.getCourse().getEnrollmentLimit() > 0) {
                long currentCount = enrollmentRepository.countByCourseId(payment.getCourse().getId());
                if (currentCount >= payment.getCourse().getEnrollmentLimit()) {
                    throw new IllegalStateException("Course enrollment limit has been reached.");
                }
            }
            Enrollment enrollment = new Enrollment(student, payment.getCourse());
            enrollmentRepository.save(enrollment);
            log.info("[Payment & Enrollment] Enrolled Student ID {} in Course ID {} via Order {}",
                    student.getId(), payment.getCourse().getId(), payment.getOrderId());
        }

        if (audit != null)
            audit.record("enrollment-" + payment.getOrderId(), "enrollment.granted",
                    Map.of("orderId", payment.getOrderId(), "studentId", student.getId(), "courseId",
                            payment.getCourse().getId(), "confirmedPaymentId", payment.getPaymentId(), "policyVersion",
                            CheckoutPolicy.VERSION));
        // Record promotional coupon redemption if applicable
        if (payment.getCouponCode() != null && !payment.getCouponCode().isBlank()) {
            couponService.recordRedemption(payment.getCouponCode(), payment.getDiscountAmount(), payment.getOrderId(),
                    student);
        }

        // Email payment receipt and GST tax invoice to student
        try {
            String invoiceHtml = null;
            try {
                invoiceHtml = invoiceService.generateInvoiceHtml(student, payment.getOrderId());
            } catch (Exception ex) {
                log.warn("[Payment Email] Failed to generate invoice HTML attachment: {}", ex.getMessage());
            }

            final String attachment = invoiceHtml;
            afterCommit(() -> {
                emailService.sendPaymentSuccessEmail(
                        student.getEmail(),
                        student.getName(),
                        payment.getCourse().getTitle(),
                        payment.getOrderId(),
                        payment.getPaymentId(),
                        payment.getInvoiceNumber(),
                        gross,
                        null,
                        attachment);
            });
        } catch (Exception e) {
            log.warn("[Payment Email] Failed to send payment receipt email: {}", e.getMessage());
        }
    }

    /**
     * Checks current order status for QR payment poller (FR-PG01).
     */
    @Transactional(readOnly = false)
    public Map<String, Object> getOrderStatus(String orderId) {
        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found: " + orderId));

        if (isOrderExpired(payment)) {
            payment.setStatus("expired");
            payment.setFailureReason("Order expired after " + ORDER_EXPIRY_MINUTES + " minutes without completion.");
            if (payment.getWalletAmountUsed() != null && payment.getWalletAmountUsed().compareTo(BigDecimal.ZERO) > 0
                    && walletService != null) {
                BigDecimal released = payment.getWalletAmountUsed();
                payment.setWalletAmountUsed(BigDecimal.ZERO);
                walletService.creditWallet(payment.getStudent(), released,
                        "Refund for expired order " + payment.getOrderId());
            }
            paymentRepository.save(payment);
        }

        return Map.of(
                "orderId", payment.getOrderId(),
                "status", payment.getStatus(),
                "amount", payment.getAmount(),
                "paymentId", payment.getPaymentId() != null ? payment.getPaymentId() : "",
                "invoiceNumber", payment.getInvoiceNumber() != null ? payment.getInvoiceNumber() : "",
                "courseTitle", payment.getCourse().getTitle());
    }

    /**
     * Viva Demo Simulation: Simulates QR scan and instant payment capture.
     */
    @Transactional
    public PaymentTransactionResponse simulatePaymentSuccess(String orderId) {
        return confirmPayment(orderId);
    }

    @Transactional(readOnly = true)
    public List<PaymentTransactionResponse> getStudentTransactions(User student) {
        if (student == null)
            throw new AccessDeniedException("Sign in to view your transactions.");
        return paymentRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())
                .stream()
                .filter(p -> !"created".equalsIgnoreCase(p.getStatus()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void assertPaymentOwnership(org.springframework.security.core.Authentication auth, Payment payment) {
        if (auth == null || payment == null)
            return;
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_admin")
                        || a.getAuthority().equalsIgnoreCase("admin"));
        if (isAdmin)
            return;

        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            if ("admin".equalsIgnoreCase(user.getRole()))
                return;
            if (payment.getStudent() != null) {
                if (user.getId() != null && user.getId().equals(payment.getStudent().getId()))
                    return;
                if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(payment.getStudent().getEmail()))
                    return;
                throw new org.springframework.security.access.AccessDeniedException(
                        "Access denied: You cannot confirm another student's order.");
            }
        }
        String name = auth.getName();
        if (name != null && payment.getStudent() != null && payment.getStudent().getEmail() != null) {
            if (name.equalsIgnoreCase(payment.getStudent().getEmail()))
                return;
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access denied: You cannot confirm another student's order.");
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentTransactionResponse> getAllTransactions(User admin) {
        if (admin == null || !"admin".equalsIgnoreCase(admin.getRole())) {
            throw new AccessDeniedException("Access denied: Administrative privileges required.");
        }
        return paymentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PaymentTransactionResponse mapToResponse(Payment payment) {
        PaymentTransactionResponse resp = new PaymentTransactionResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getPaymentId(),
                payment.getCourse().getId(),
                payment.getCourse().getTitle(),
                payment.getStudent().getId(),
                payment.getStudent().getName(),
                payment.getStudent().getEmail(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getCreatedAt());
        resp.setInvoiceNumber(payment.getInvoiceNumber());
        resp.setRefundStatus(payment.getRefundStatus());
        resp.setRefundAmount(payment.getRefundAmount());
        resp.setRefundReason(payment.getRefundReason());
        resp.setDiscountAmount(payment.getDiscountAmount());
        resp.setWalletAmountUsed(payment.getWalletAmountUsed());
        resp.setCouponCode(payment.getCouponCode());
        resp.setRiskScore(payment.getRiskScore());
        return resp;
    }

    static void afterCommit(Runnable send) {
        Runnable safe = () -> {
            try {
                send.run();
            } catch (RuntimeException ex) {
                log.warn("Payment email delivery failed", ex);
            }
        };
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager
                    .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            safe.run();
                        }
                    });
        } else
            safe.run();
    }

    public static String resolvePaymentMethod(Payment payment, String explicitMethod) {
        if (explicitMethod != null && !explicitMethod.isBlank()) {
            return normalizePaymentMethod(explicitMethod);
        }
        if (payment != null && payment.getPaymentMethod() != null
                && !payment.getPaymentMethod().isBlank()
                && !"UPI".equalsIgnoreCase(payment.getPaymentMethod())
                && !"QR Payment".equalsIgnoreCase(payment.getPaymentMethod())) {
            return normalizePaymentMethod(payment.getPaymentMethod());
        }
        return "QR Payment";
    }

    public static String normalizePaymentMethod(String method) {
        if (method == null || method.isBlank())
            return "QR Payment";
        String clean = method.trim();
        String lower = clean.toLowerCase();
        if (lower.equals("saved_card") || lower.equals("saved-card") || lower.equals("saved card")) {
            return "Saved Card";
        }
        if (lower.contains("card")) {
            return "Card";
        }
        if (lower.contains("qr")) {
            return "QR Payment";
        }
        if (lower.contains("upi")) {
            return "UPI";
        }
        if (lower.contains("netbanking") || lower.contains("net banking")) {
            return "Net Banking";
        }
        if (lower.contains("wallet")) {
            return "Wallet";
        }
        return clean;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> transactionsPage(User user, int page, int size) {
        if (user == null || !"admin".equals(user.getRole()))
            throw new AccessDeniedException("Administrator required.");
        if (page < 0 || page > 1000000 || size < 1 || size > 200)
            throw new IllegalArgumentException("Choose page >=0 and size 1–200.");
        var rows = paymentRepository
                .findAllByOrderByCreatedAtDescIdDesc(org.springframework.data.domain.PageRequest.of(page, size));
        return Map.of("content", rows.getContent().stream().map(this::mapToResponse).toList(), "page", page, "size",
                size, "totalElements", rows.getTotalElements(), "totalPages", rows.getTotalPages());
    }
}
