package com.olp.service;

import com.olp.entity.Course;
import com.olp.entity.Payment;
import com.olp.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpiQrCodeServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    private UpiQrCodeService upiQrCodeService;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        upiQrCodeService = new UpiQrCodeService(paymentRepository);

        Course course = new Course();
        course.setTitle("Full Stack Cloud Engineering");

        testPayment = new Payment();
        testPayment.setOrderId("OLP-ORDER-999");
        testPayment.setAmount(new BigDecimal("4999.00"));
        testPayment.setCourse(course);
        testPayment.setCreatedAt(java.time.LocalDateTime.now());
    }

    @Test
    void testGenerateScanAndPayPortalHtml_DefaultFrontendUrl() {
        when(paymentRepository.findByOrderId("OLP-ORDER-999")).thenReturn(Optional.of(testPayment));

        String html = upiQrCodeService.generateScanAndPayPortalHtml("OLP-ORDER-999");

        assertNotNull(html);
        assertTrue(html.contains("window.location.href = 'http://localhost:3000/#dashboard'"));
    }

    @Test
    void testGenerateScanAndPayPortalHtml_CustomFrontendUrlWithTrailingSlash() {
        when(paymentRepository.findByOrderId("OLP-ORDER-999")).thenReturn(Optional.of(testPayment));

        upiQrCodeService.setFrontendUrl("https://learn.production.com/");
        String html = upiQrCodeService.generateScanAndPayPortalHtml("OLP-ORDER-999");

        assertNotNull(html);
        assertTrue(html.contains("window.location.href = 'https://learn.production.com/#dashboard'"));
        assertFalse(html.contains("http://localhost:3000/#dashboard"));
    }
}
