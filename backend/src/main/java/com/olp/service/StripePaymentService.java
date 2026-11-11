package com.olp.service;

import com.olp.dto.PaymentTransactionResponse;
import com.olp.entity.*;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseRepository;
import com.olp.repository.EnrollmentRepository;
import com.olp.repository.InstructorEarningRepository;
import com.olp.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Legacy Stripe payment service stub — replaced by QR-based payment flow.
 * Retained for compilation compatibility; no Stripe SDK calls remain.
 * All new payments go through PaymentService → QR confirmation flow.
 */
@Service
public class StripePaymentService {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentService.class);

    @org.springframework.beans.factory.annotation.Autowired(required=false) private PaymentService lifecycle;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final InstructorEarningRepository instructorEarningRepository;

    public StripePaymentService(CourseRepository courseRepository,
                                EnrollmentRepository enrollmentRepository,
                                PaymentRepository paymentRepository,
                                InstructorEarningRepository instructorEarningRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.paymentRepository = paymentRepository;
        this.instructorEarningRepository = instructorEarningRepository;
    }

    /**
     * Creates a local payment record for QR-based checkout.
     * Returns order details compatible with the QR payment portal.
     */
    @Transactional
    public PaymentTransactionResponse createLocalPayment(User student, Integer courseId, BigDecimal amount) {
        if(lifecycle!=null){
            var order=lifecycle.createPaymentOrder(student,new com.olp.dto.CreatePaymentOrderRequest(courseId,null,"INR"));
            var payment=paymentRepository.findByOrderId(order.getOrderId()).orElseThrow();
            return mapToResponse(payment,payment.getCourse(),student);
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        // In viva/demo sandbox, allow repeat orders for testing
        if (enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            log.info("[Stripe Sandbox] Student {} already has an active enrollment in course ID {}. Allowing demo re-checkout.",
                    student.getId(), course.getId());
        }

        BigDecimal chargeAmount = PaymentService.catalogPrice(course);

        String orderId = "OLP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();


        Payment payment = new Payment(orderId, student, course, chargeAmount, "INR");
        payment.setStatus("created");

        paymentRepository.save(payment);
        payment.setInvoiceNumber("INV-"+String.format(java.util.Locale.ROOT,"%010d",payment.getId()));

        log.info("[Payment] Local QR order created: {} for course '{}'", orderId, course.getTitle());

        return mapToResponse(payment, course, student);
    }

    private PaymentTransactionResponse mapToResponse(Payment p, Course course, User student) {
        PaymentTransactionResponse resp = new PaymentTransactionResponse(
                p.getId(),
                p.getOrderId(),
                p.getPaymentId(),
                course.getId(),
                course.getTitle(),
                student.getId(),
                student.getName(),
                student.getEmail(),
                p.getAmount(),
                p.getCurrency(),
                p.getPaymentMethod(),
                p.getStatus(),
                p.getCreatedAt() != null ? p.getCreatedAt() : LocalDateTime.now()
        );
        resp.setInvoiceNumber(p.getInvoiceNumber());
        resp.setRefundStatus(p.getRefundStatus());
        resp.setDiscountAmount(p.getDiscountAmount());
        resp.setWalletAmountUsed(p.getWalletAmountUsed());
        resp.setRiskScore(p.getRiskScore());
        return resp;
    }
}
