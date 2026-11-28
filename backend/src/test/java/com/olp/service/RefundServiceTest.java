package com.olp.service;

import com.olp.dto.RefundRequest;
import com.olp.dto.RefundResponse;
import com.olp.entity.Course;
import com.olp.entity.Payment;
import com.olp.entity.Refund;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.EnrollmentRepository;
import com.olp.repository.PaymentRepository;
import com.olp.repository.RefundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private RefundRepository refundRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private PaymentGateway gateway;
    @Mock
    private EmailService emailService;
    @Mock
    private WalletService walletService;
    @Mock
    private PaymentAuditService audit;

    private RefundService refundService;

    @BeforeEach
    void setUp() {
        refundService = new RefundService(
                paymentRepository, refundRepository, enrollmentRepository, gateway, emailService, walletService);
        ReflectionTestUtils.setField(refundService, "audit", audit);
    }

    private User user(Integer id, String role) {
        User u = new User("User-" + id, "u" + id + "@olp.test", "hash", role);
        u.setId(id);
        return u;
    }

    private Payment successfulPayment(User owner, LocalDateTime createdAt) {
        Course c = new Course();
        c.setId(50);
        c.setTitle("Spring Boot Mastery");
        Payment p = new Payment();
        p.setId(9);
        p.setStudent(owner);
        p.setCourse(c);
        p.setOrderId("ORD-9");
        p.setPaymentId("PAY-9");
        p.setAmount(new BigDecimal("300.00"));
        p.setWalletAmountUsed(new BigDecimal("200.00"));
        p.setStatus("success");
        p.setRefundStatus("none");
        p.setCreatedAt(createdAt);
        return p;
    }

    @Test
    void processRefund_rejectsAnonymousMissingNonOwnerNonSuccessDoubleRefundAndExpiredWindow() {
        assertThrows(AccessDeniedException.class,
                () -> refundService.processRefund(null, new RefundRequest("PAY-9", null, "reason")));

        User owner = user(10, "student");
        User intruder = user(20, "student");
        Payment p = successfulPayment(owner, LocalDateTime.now().minusDays(2));

        when(paymentRepository.findByPaymentIdForUpdate("MISSING")).thenReturn(Optional.empty());
        when(paymentRepository.findByOrderIdForUpdate("MISSING")).thenReturn(Optional.empty());
        when(paymentRepository.findByPaymentId("MISSING")).thenReturn(Optional.empty());
        when(paymentRepository.findByOrderId("MISSING")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> refundService.processRefund(owner, new RefundRequest("MISSING", null, "reason")));

        when(paymentRepository.findByPaymentIdForUpdate("PAY-9")).thenReturn(Optional.of(p));
        assertThrows(AccessDeniedException.class,
                () -> refundService.processRefund(intruder, new RefundRequest("PAY-9", null, "reason")));

        p.setStatus("created");
        assertThrows(IllegalArgumentException.class,
                () -> refundService.processRefund(owner, new RefundRequest("PAY-9", null, "reason")));

        p.setStatus("success");
        p.setRefundStatus("refunded");
        assertThrows(IllegalArgumentException.class,
                () -> refundService.processRefund(owner, new RefundRequest("PAY-9", null, "reason")));

        p.setRefundStatus("none");
        p.setCreatedAt(LocalDateTime.now().minusDays(35));
        assertThrows(IllegalArgumentException.class,
                () -> refundService.processRefund(owner, new RefundRequest("PAY-9", null, "reason")));
    }

    @Test
    void processRefund_splitsGatewayAndWalletRefundsRevokesEnrollmentAndSwallowsEmailFailure() {
        User owner = user(10, "student");
        Payment p = successfulPayment(owner, LocalDateTime.now().minusDays(3));
        when(paymentRepository.findByPaymentIdForUpdate("PAY-9")).thenReturn(Optional.of(p));
        when(refundRepository.existsByPaymentId(9)).thenReturn(false);
        when(gateway.executeRefund("PAY-9", new BigDecimal("300.00"), "Duplicate purchase")).thenReturn("RFD-123");
        when(enrollmentRepository.existsByStudentIdAndCourseId(10, 50)).thenReturn(true);
        doThrow(new RuntimeException("SMTP offline")).when(emailService)
                .sendRefundConfirmationEmail(any(), any(), any(), any(), any(), any(), any());

        RefundResponse response = refundService.processRefund(
                owner, new RefundRequest("PAY-9", null, "Duplicate purchase"));

        assertEquals("RFD-123", response.getRefundId());
        assertEquals(new BigDecimal("500.00"), response.getAmount());
        assertEquals("processed", response.getStatus());
        assertTrue(response.isEnrollmentRevoked());
        assertEquals("refunded", p.getRefundStatus());

        verify(walletService).creditWallet(eq(owner), eq(new BigDecimal("200.00")), contains("ORD-9"));
        verify(gateway).executeRefund("PAY-9", new BigDecimal("300.00"), "Duplicate purchase");
        verify(audit).record(eq("gateway-refund-RFD-123"), eq("gateway.refund"), any());
        verify(refundRepository).save(any(Refund.class));
        verify(enrollmentRepository).deleteByStudentIdAndCourseId(10, 50);
    }

    @Test
    void processApprovedException_requiresAdminAndBypasses30DayWindowForWalletOnlyPayment() {
        User owner = user(10, "student");
        User admin = user(1, "admin");
        Payment p = successfulPayment(owner, LocalDateTime.now().minusDays(90));
        p.setPaymentId("TXN-WAL-999");
        p.setAmount(BigDecimal.ZERO);
        p.setWalletAmountUsed(new BigDecimal("200.00"));

        assertThrows(AccessDeniedException.class,
                () -> refundService.processApprovedException(owner, new RefundRequest("ORD-9", null, "Late exception")));

        when(paymentRepository.findByPaymentIdForUpdate("ORD-9")).thenReturn(Optional.empty());
        when(paymentRepository.findByOrderIdForUpdate("ORD-9")).thenReturn(Optional.of(p));
        when(refundRepository.existsByPaymentId(9)).thenReturn(false);
        when(enrollmentRepository.existsByStudentIdAndCourseId(10, 50)).thenReturn(false);

        RefundResponse response = refundService.processApprovedException(
                admin, new RefundRequest("ORD-9", new BigDecimal("200.00"), "Medical hardship"));

        assertTrue(response.getRefundId().startsWith("RFD-WAL-"));
        assertFalse(response.isEnrollmentRevoked());
        verify(gateway, never()).executeRefund(any(), any(), any());
        verify(walletService).creditWallet(eq(owner), eq(new BigDecimal("200.00")), any());
    }

    @Test
    void refundsPage_enforcesAdminRolePaginationBoundsAndMapsContent() {
        User student = user(10, "student");
        User admin = user(1, "admin");

        assertThrows(AccessDeniedException.class, () -> refundService.refundsPage(student, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> refundService.refundsPage(admin, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> refundService.refundsPage(admin, 0, 201));

        Payment p = successfulPayment(student, LocalDateTime.now());
        Refund refund = new Refund(p, "RFD-7", new BigDecimal("100.00"), "processed", "Reason");
        when(refundRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(refund)));

        Map<String, Object> page = refundService.refundsPage(admin, 0, 10);
        assertEquals(1L, page.get("totalElements"));
        @SuppressWarnings("unchecked")
        List<RefundResponse> content = (List<RefundResponse>) page.get("content");
        assertEquals("RFD-7", content.get(0).getRefundId());
        assertTrue(content.get(0).isEnrollmentRevoked());
    }
}
