package com.olp.service;

import com.olp.entity.Course;
import com.olp.entity.CourseStatus;
import com.olp.entity.User;
import com.olp.repository.CourseRepository;
import com.olp.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentSubscriptionServiceTest {

    private PaymentAuditService audit;
    private WalletRepository locks;
    private SimulatedPaymentMethods methods;
    private PaymentService payments;
    private CourseRepository courses;
    private PaymentPolicyService policies;
    private EmailService emailService;
    private PaymentSubscriptionService service;

    private User student;
    private Course course;

    @BeforeEach
    void setUp() {
        audit = mock(PaymentAuditService.class);
        locks = mock(WalletRepository.class);
        methods = mock(SimulatedPaymentMethods.class);
        payments = mock(PaymentService.class);
        courses = mock(CourseRepository.class);
        policies = mock(PaymentPolicyService.class);
        emailService = mock(EmailService.class);

        service = new PaymentSubscriptionService(audit, locks, methods, payments, courses, policies, emailService);

        student = new User("Alice Learner", "alice@example.com", "hash", "student");
        student.setId(101);

        course = new Course();
        course.setId(10);
        course.setTitle("Full Stack Cloud Mastery");
        course.setStatus(CourseStatus.PUBLISHED);

        when(courses.findById(10)).thenReturn(Optional.of(course));
        when(policies.price(course)).thenReturn(new BigDecimal("499.00"));
        when(methods.list(student)).thenReturn(List.of(Map.of(
                "token", "simtok_abc123",
                "method", "card",
                "displayName", "Visa •••• 4242 (Exp 12/28)"
        )));
    }

    @Test
    void create_successfulWithPlanDetailsAndConfirmationEmail() {
        when(audit.list("subscription.created")).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.create(
                student,
                10,
                "simtok_abc123",
                true,
                3,
                "Quarterly Plan",
                new BigDecimal("499.00"),
                new BigDecimal("1497.00")
        );

        assertNotNull(result);
        assertEquals("active", result.get("status"));
        assertEquals(10, result.get("courseId"));
        assertEquals("Full Stack Cloud Mastery", result.get("courseTitle"));
        assertEquals(3, result.get("durationMonths"));
        assertEquals("Quarterly Plan", result.get("planName"));
        assertEquals(new BigDecimal("499.00"), result.get("monthlyPrice"));
        assertEquals(new BigDecimal("1497.00"), result.get("totalPlanCost"));
        assertNotNull(result.get("nextRenewalAt"));
        assertNotNull(result.get("successMessage"));
        assertTrue(result.get("successMessage").toString().contains("Quarterly Plan"));

        verify(audit).record(startsWith("sub_"), eq("subscription.created"), anyMap());
        verify(emailService).sendSubscriptionConfirmationEmail(
                eq("alice@example.com"),
                eq("Alice Learner"),
                eq("Full Stack Cloud Mastery"),
                eq("Quarterly Plan"),
                eq(3),
                eq(new BigDecimal("499.00")),
                eq(new BigDecimal("1497.00")),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    void create_withoutConsentThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.create(student, 10, "simtok_abc123", false, 1, "Monthly", null, null)
        );
        assertTrue(ex.getMessage().contains("Explicit subscription consent is required"));
    }

    @Test
    void create_byNonStudentThrowsAccessDenied() {
        User instructor = new User("Bob", "bob@example.com", "hash", "instructor");
        instructor.setId(202);

        assertThrows(AccessDeniedException.class, () ->
                service.create(instructor, 10, "simtok_abc123", true)
        );
    }

    @Test
    void create_forUnpublishedCourseThrowsException() {
        Course draftCourse = new Course();
        draftCourse.setId(11);
        draftCourse.setStatus(CourseStatus.DRAFT);
        when(courses.findById(11)).thenReturn(Optional.of(draftCourse));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.create(student, 11, "simtok_abc123", true)
        );
        assertTrue(ex.getMessage().contains("Choose a published course"));
    }

    @Test
    void create_whenActiveSubscriptionAlreadyExistsThrowsException() {
        Map<String, Object> existing = Map.of(
                "id", "sub_existing",
                "owner", 101,
                "courseId", 10,
                "status", "active",
                "nextRenewalAt", "2026-12-15T00:00:00Z"
        );
        when(audit.list("subscription.created")).thenReturn(List.of(existing));
        when(audit.get("sub_existing")).thenReturn(existing);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.create(student, 10, "simtok_abc123", true)
        );
        assertTrue(ex.getMessage().contains("An active subscription already exists"));
    }

    @Test
    void list_enrichesCourseTitleAndPayableAmount() {
        Map<String, Object> stored = new HashMap<>();
        stored.put("id", "sub_1001");
        stored.put("owner", 101);
        stored.put("courseId", 10);
        stored.put("status", "active");
        stored.put("nextRenewalAt", "2026-12-15T00:00:00Z");

        when(audit.list("subscription.created")).thenReturn(List.of(stored));
        when(audit.get("sub_1001")).thenReturn(stored);
        when(audit.list("subscription.renewed")).thenReturn(Collections.emptyList());

        List<Map<String, Object>> subs = service.list(student);
        assertEquals(1, subs.size());
        Map<String, Object> sub = subs.get(0);
        assertEquals("Full Stack Cloud Mastery", sub.get("courseTitle"));
        assertEquals(new BigDecimal("499.00"), sub.get("payableAmount"));
        assertEquals(new BigDecimal("499.00"), sub.get("monthlyPrice"));
    }

    @Test
    void cancel_recordsCancellationInAudit() {
        Map<String, Object> stored = Map.of(
                "id", "sub_1001",
                "owner", 101,
                "courseId", 10,
                "status", "active",
                "nextRenewalAt", "2026-12-15T00:00:00Z"
        );
        when(audit.get("sub_1001")).thenReturn(stored);
        when(audit.list("subscription.renewed")).thenReturn(Collections.emptyList());

        service.cancel(student, "sub_1001");
        verify(audit).record(eq("cancel-sub_1001"), eq("subscription.cancelled"), anyMap());
    }

    @Test
    void quote_returnsPricingAndPolicy() {
        Map<String, Object> stored = Map.of(
                "id", "sub_1001",
                "owner", 101,
                "courseId", 10,
                "status", "active",
                "nextRenewalAt", "2026-12-15T00:00:00Z"
        );
        when(audit.get("sub_1001")).thenReturn(stored);
        when(audit.list("subscription.renewed")).thenReturn(Collections.emptyList());

        Map<String, Object> quote = service.quote(student, "sub_1001");
        assertEquals(new BigDecimal("499.00"), quote.get("amount"));
        assertEquals("INR", quote.get("currency"));
        assertEquals(true, quote.get("taxInclusive"));
        assertNotNull(quote.get("refundPolicy"));
    }
}

