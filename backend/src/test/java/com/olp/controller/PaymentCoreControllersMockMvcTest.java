package com.olp.controller;

import com.olp.dto.PaymentOrderResponse;
import com.olp.dto.PaymentTransactionResponse;
import com.olp.entity.Coupon;
import com.olp.entity.Course;
import com.olp.entity.Payment;
import com.olp.entity.User;
import com.olp.exception.GlobalExceptionHandler;
import com.olp.repository.*;
import com.olp.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Cross-layer MockMvc integration test wiring RefundController, CouponController, and PaymentController
 * with real CouponService, real RefundService, real ExceptionalRefundService, and GlobalExceptionHandler.
 */
@ExtendWith(MockitoExtension.class)
class PaymentCoreControllersMockMvcTest {

    @Mock private CouponRepository couponRepository;
    @Mock private CouponUsageRepository usageRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private PaymentGateway gateway;
    @Mock private EmailService emailService;
    @Mock private WalletService walletService;
    @Mock private PaymentAuditService audit;
    @Mock private PaymentService paymentService;

    private MockMvc mockMvc;
    private User student;
    private User otherStudent;
    private User admin;

    @BeforeEach
    void setUp() {
        CouponService couponService = new CouponService(couponRepository, usageRepository);
        RefundService refundService = new RefundService(
                paymentRepository, refundRepository, enrollmentRepository, gateway, emailService, walletService);
        ReflectionTestUtils.setField(refundService, "audit", audit);

        ExceptionalRefundService exceptionalRefundService = new ExceptionalRefundService(
                paymentRepository, refundRepository, audit, refundService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new CouponController(couponService),
                        new RefundController(refundService, exceptionalRefundService),
                        new PaymentController(paymentService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        student = new User("Sam", "sam@olp.test", "hash", "student");
        student.setId(10);
        otherStudent = new User("Other", "other@olp.test", "hash", "student");
        otherStudent.setId(20);
        admin = new User("Admin", "admin@olp.test", "hash", "admin");
        admin.setId(1);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @Test
    void couponController_integratesWithRealCouponServiceAndEnforcesAdminAndValidation() throws Exception {
        Coupon active = new Coupon("SAVE10", new BigDecimal("10.00"), new BigDecimal("100.00"),
                new BigDecimal("100.00"), LocalDateTime.now().plusDays(7), 50);
        when(couponRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(active));
        when(couponRepository.findByCodeIgnoreCase("MISSING")).thenReturn(Optional.empty());

        authenticate(student);
        mockMvc.perform(post("/api/payments/coupons/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SAVE10\",\"orderAmount\":500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountAmount").value(50.00))
                .andExpect(jsonPath("$.finalPayableAmount").value(450.00));

        mockMvc.perform(post("/api/payments/coupons/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"MISSING\",\"orderAmount\":500}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // Student forbidden from creating coupons
        mockMvc.perform(post("/api/payments/coupons/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"HACK50\",\"discountPercentage\":50,\"usageLimit\":10}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Admin toggles existing coupon cleanly
        active.setId(4);
        when(couponRepository.findById(4)).thenReturn(Optional.of(active));
        when(couponRepository.save(active)).thenReturn(active);
        authenticate(admin);
        mockMvc.perform(patch("/api/payments/coupons/4/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void refundController_validatesDtoEnforcesOwnershipAndRoutesRecentVsExpiredPurchases() throws Exception {
        // Bean validation rejects blank fields with 400
        authenticate(student);
        mockMvc.perform(post("/api/payments/refunds/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentId\":\"\",\"reason\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Malformed JSON maps to 400
        mockMvc.perform(post("/api/payments/refunds/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{broken-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed request body"));

        // Anonymous rejected with 403
        SecurityContextHolder.clearContext();
        mockMvc.perform(post("/api/payments/refunds/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentId\":\"ORD-1\",\"reason\":\"Need refund\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        Course course = new Course();
        course.setId(5);
        course.setTitle("Java Concurrency");

        Payment payment = new Payment();
        payment.setId(8);
        payment.setOrderId("ORD-8");
        payment.setPaymentId("PAY-8");
        payment.setStudent(student);
        payment.setCourse(course);
        payment.setAmount(new BigDecimal("499.00"));
        payment.setStatus("success");
        payment.setRefundStatus("none");
        payment.setCreatedAt(LocalDateTime.now().minusDays(3));

        when(paymentRepository.findByPaymentIdForUpdate("PAY-8")).thenReturn(Optional.of(payment));

        // Non-owner student rejected with 403
        authenticate(otherStudent);
        mockMvc.perform(post("/api/payments/refunds/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentId\":\"PAY-8\",\"reason\":\"Not mine\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Owner within 30 days executes immediate refund + enrollment revocation
        authenticate(student);
        when(refundRepository.existsByPaymentId(8)).thenReturn(false);
        when(gateway.executeRefund("PAY-8", new BigDecimal("499.00"), "Changed mind")).thenReturn("RFD-800");
        when(enrollmentRepository.existsByStudentIdAndCourseId(10, 5)).thenReturn(true);

        mockMvc.perform(post("/api/payments/refunds/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentId\":\"PAY-8\",\"reason\":\"Changed mind\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundId").value("RFD-800"))
                .andExpect(jsonPath("$.status").value("processed"))
                .andExpect(jsonPath("$.enrollmentRevoked").value(true));

        // Purchase >30 days routes automatically to pending exceptional review
        payment.setRefundStatus("none");
        payment.setCreatedAt(LocalDateTime.now().minusDays(45));
        when(paymentRepository.findByOrderIdForUpdate("ORD-8")).thenReturn(Optional.of(payment));
        when(audit.exists("exception-ORD-8")).thenReturn(false);
        when(audit.get("exception-ORD-8")).thenReturn(Map.of("eventId", "exception-ORD-8"));

        mockMvc.perform(post("/api/payments/refunds/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentId\":\"PAY-8\",\"reason\":\"Late medical request\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundId").value("exception-ORD-8"))
                .andExpect(jsonPath("$.status").value("pending_review"))
                .andExpect(jsonPath("$.enrollmentRevoked").value(false));

        // Pagination bounds enforced cleanly by GlobalExceptionHandler
        authenticate(admin);
        mockMvc.perform(get("/api/payments/refunds/page").param("page", "0").param("size", "250"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void paymentController_validatesOrderRequestAndDelegatesConfirmCancelAndStatus() throws Exception {
        authenticate(student);

        mockMvc.perform(post("/api/payments/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(paymentService);

        PaymentOrderResponse created = new PaymentOrderResponse();
        created.setOrderId("ORD-NEW");
        when(paymentService.createPaymentOrder(eq(student), any())).thenReturn(created);

        mockMvc.perform(post("/api/payments/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":12,\"paymentMethod\":\"UPI\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORD-NEW"));

        PaymentTransactionResponse tx = new PaymentTransactionResponse();
        tx.setOrderId("ORD-NEW");
        tx.setStatus("success");
        when(paymentService.confirmPayment("ORD-NEW", "CARD")).thenReturn(tx);

        mockMvc.perform(post("/api/payments/confirm/ORD-NEW")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"CARD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        PaymentTransactionResponse cancelled = new PaymentTransactionResponse();
        cancelled.setOrderId("ORD-NEW");
        cancelled.setStatus("cancelled");
        when(paymentService.cancelPayment("ORD-NEW")).thenReturn(cancelled);

        mockMvc.perform(post("/api/payments/cancel/ORD-NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("cancelled"));
    }
}
