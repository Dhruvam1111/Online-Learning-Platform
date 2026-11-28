package com.olp.service;

import com.olp.entity.Payment;
import com.olp.entity.User;
import com.olp.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    private FraudDetectionService service;
    private User student;

    @BeforeEach
    void setUp() {
        service = new FraudDetectionService(paymentRepository);
        student = new User("Sam", "sam@olp.com", "hash", "student", "active");
        student.setId(42);
    }

    @Test
    void evaluateRisk_RejectsNullStudentOrMissingId() {
        assertThrows(IllegalArgumentException.class, () -> service.evaluateRisk(null, BigDecimal.TEN));
        User noId = new User();
        assertThrows(IllegalArgumentException.class, () -> service.evaluateRisk(noId, BigDecimal.TEN));
    }

    @Test
    void evaluateRisk_HandlesNullPaymentsAndPaymentsWithNullCreatedAt() {
        Payment missingTimestamp = mock(Payment.class);
        when(missingTimestamp.getCreatedAt()).thenReturn(null);
        when(missingTimestamp.getStatus()).thenReturn("failed");

        when(paymentRepository.findByStudentIdOrderByCreatedAtDesc(42))
                .thenReturn(java.util.Arrays.asList(null, missingTimestamp));

        var res = service.evaluateRisk(student, BigDecimal.valueOf(100));
        assertNotNull(res);
        assertEquals("LOW", res.getLevel());
    }

    @Test
    void evaluateRisk_FlagsHighVelocityFailuresAndHighValue() {
        Payment failed1 = mock(Payment.class);
        when(failed1.getCreatedAt()).thenReturn(LocalDateTime.now().minusMinutes(1));
        when(failed1.getStatus()).thenReturn("failed");

        Payment failed2 = mock(Payment.class);
        when(failed2.getCreatedAt()).thenReturn(LocalDateTime.now().minusMinutes(2));
        when(failed2.getStatus()).thenReturn("failed");

        Payment ok = mock(Payment.class);
        when(ok.getCreatedAt()).thenReturn(LocalDateTime.now().minusMinutes(3));
        when(ok.getStatus()).thenReturn("success");

        when(paymentRepository.findByStudentIdOrderByCreatedAtDesc(42))
                .thenReturn(List.of(failed1, failed2, ok));

        var res = service.evaluateRisk(student, BigDecimal.valueOf(6000));
        assertEquals("HIGH", res.getLevel());
        assertEquals(90, res.getScore());
        assertTrue(res.getFlaggedFactors().size() >= 3);
    }
}
