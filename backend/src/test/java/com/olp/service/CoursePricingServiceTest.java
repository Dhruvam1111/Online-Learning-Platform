package com.olp.service;

import com.olp.dto.CoursePricingRequest;
import com.olp.dto.CoursePricingResponse;
import com.olp.entity.Course;
import com.olp.entity.CourseStatus;
import com.olp.entity.DiscountType;
import com.olp.entity.User;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoursePricingServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private PaymentPolicyService paymentPolicyService;

    private CoursePricingService pricingService;

    private User instructor;
    private Course course;

    @BeforeEach
    void setUp() {
        pricingService = new CoursePricingService(
                courseRepository,
                enrollmentRepository,
                courseInstructorRepository,
                paymentPolicyService
        );

        instructor = new User("Prof. Smith", "smith@olp.com", "hash", "instructor", "active");
        instructor.setId(10);

        course = new Course(instructor, "Java Masterclass", "Complete Java course", CourseStatus.PUBLISHED);
        course.setId(101);
        course.setPrice(new BigDecimal("1000.00"));

        lenient().when(paymentPolicyService.getTaxRateForCourse(anyInt())).thenReturn(new BigDecimal("0.18"));
    }

    // =========================================================================
    // 1. FREE COURSE TESTS
    // =========================================================================

    @Test
    @DisplayName("Free Course: Zero price yields isFree=true, 0 tax and 0 final payable amount")
    void testCalculatePricing_FreeCourse_ZeroPrice() {
        course.setPrice(BigDecimal.ZERO);

        CoursePricingResponse response = pricingService.calculatePricing(course);

        assertNotNull(response);
        assertTrue(response.isFree());
        assertEquals(new BigDecimal("0.00"), response.getBasePrice());
        assertEquals(new BigDecimal("0.00"), response.getDiscountAmount());
        assertEquals(new BigDecimal("0.00"), response.getTaxAmount());
        assertEquals(new BigDecimal("0.00"), response.getFinalPayableAmount());
    }

    @Test
    @DisplayName("Free Course: Null price defaults to 0.00 free course")
    void testCalculatePricing_FreeCourse_NullPrice() {
        course.setPrice(null);

        CoursePricingResponse response = pricingService.calculatePricing(course);

        assertNotNull(response);
        assertTrue(response.isFree());
        assertEquals(new BigDecimal("0.00"), response.getBasePrice());
        assertEquals(new BigDecimal("0.00"), response.getFinalPayableAmount());
    }

    // =========================================================================
    // 2. PAID COURSE WITHOUT DISCOUNT
    // =========================================================================

    @Test
    @DisplayName("Paid Course: Price 1000.00 with 18% tax yields 1180.00 final payable")
    void testCalculatePricing_PaidCourse_NoDiscount() {
        course.setPrice(new BigDecimal("1000.00"));

        CoursePricingResponse response = pricingService.calculatePricing(course);

        assertNotNull(response);
        assertFalse(response.isFree());
        assertEquals(new BigDecimal("1000.00"), response.getBasePrice());
        assertEquals(new BigDecimal("0.00"), response.getDiscountAmount());
        assertEquals(new BigDecimal("1000.00"), response.getDiscountedPrice());
        assertEquals(new BigDecimal("180.00"), response.getTaxAmount());
        assertEquals(new BigDecimal("1180.00"), response.getFinalPayableAmount());
    }

    // =========================================================================
    // 3. PERCENTAGE DISCOUNT TESTS
    // =========================================================================

    @Test
    @DisplayName("Percentage Discount: 20% on 1000.00 -> 200.00 discount, 800.00 net, 144.00 tax, 944.00 final")
    void testCalculatePricing_PercentageDiscount_Active() {
        course.setPrice(new BigDecimal("1000.00"));
        course.setDiscountType(DiscountType.PERCENTAGE);
        course.setDiscountValue(new BigDecimal("20.00"));

        CoursePricingResponse response = pricingService.calculatePricing(course);

        assertNotNull(response);
        assertTrue(response.isDiscountActive());
        assertEquals(new BigDecimal("200.00"), response.getDiscountAmount());
        assertEquals(new BigDecimal("800.00"), response.getDiscountedPrice());
        assertEquals(new BigDecimal("144.00"), response.getTaxAmount());
        assertEquals(new BigDecimal("944.00"), response.getFinalPayableAmount());
    }

    // =========================================================================
    // 4. FIXED DISCOUNT TESTS
    // =========================================================================

    @Test
    @DisplayName("Fixed Discount: 150.00 on 500.00 -> 350.00 net, 63.00 tax, 413.00 final")
    void testCalculatePricing_FixedDiscount_Active() {
        course.setPrice(new BigDecimal("500.00"));
        course.setDiscountType(DiscountType.FIXED);
        course.setDiscountValue(new BigDecimal("150.00"));

        CoursePricingResponse response = pricingService.calculatePricing(course);

        assertNotNull(response);
        assertTrue(response.isDiscountActive());
        assertEquals(new BigDecimal("150.00"), response.getDiscountAmount());
        assertEquals(new BigDecimal("350.00"), response.getDiscountedPrice());
        assertEquals(new BigDecimal("63.00"), response.getTaxAmount());
        assertEquals(new BigDecimal("413.00"), response.getFinalPayableAmount());
    }

    @Test
    @DisplayName("Fixed Discount: Discount exceeding price is capped at price (net=0, payable=0)")
    void testCalculatePricing_FixedDiscount_CappedAtPrice() {
        course.setPrice(new BigDecimal("200.00"));
        course.setDiscountType(DiscountType.FIXED);
        course.setDiscountValue(new BigDecimal("500.00"));

        CoursePricingResponse response = pricingService.calculatePricing(course);

        assertNotNull(response);
        assertEquals(new BigDecimal("200.00"), response.getDiscountAmount());
        assertEquals(new BigDecimal("0.00"), response.getDiscountedPrice());
        assertEquals(new BigDecimal("0.00"), response.getTaxAmount());
        assertEquals(new BigDecimal("0.00"), response.getFinalPayableAmount());
    }

    // =========================================================================
    // 5. DISCOUNT VALIDITY DATES TESTS
    // =========================================================================

    @Test
    @DisplayName("Validity Dates: Discount active when current time is within [start, end]")
    void testCalculatePricing_DiscountWithinValidityWindow() {
        LocalDateTime now = LocalDateTime.now();
        course.setPrice(new BigDecimal("1000.00"));
        course.setDiscountType(DiscountType.PERCENTAGE);
        course.setDiscountValue(new BigDecimal("10.00"));
        course.setDiscountStartDate(now.minusDays(2));
        course.setDiscountEndDate(now.plusDays(2));

        CoursePricingResponse response = pricingService.calculatePricing(course, now);

        assertTrue(response.isDiscountActive());
        assertEquals(new BigDecimal("100.00"), response.getDiscountAmount());
        assertEquals(new BigDecimal("900.00"), response.getDiscountedPrice());
    }

    @Test
    @DisplayName("Validity Dates: Discount inactive before start date")
    void testCalculatePricing_DiscountBeforeStartDate() {
        LocalDateTime now = LocalDateTime.now();
        course.setPrice(new BigDecimal("1000.00"));
        course.setDiscountType(DiscountType.PERCENTAGE);
        course.setDiscountValue(new BigDecimal("25.00"));
        course.setDiscountStartDate(now.plusDays(1)); // starts tomorrow
        course.setDiscountEndDate(now.plusDays(5));

        CoursePricingResponse response = pricingService.calculatePricing(course, now);

        assertFalse(response.isDiscountActive());
        assertEquals(new BigDecimal("0.00"), response.getDiscountAmount());
        assertEquals(new BigDecimal("1000.00"), response.getDiscountedPrice());
    }

    @Test
    @DisplayName("Validity Dates: Discount inactive after end date")
    void testCalculatePricing_DiscountAfterEndDate() {
        LocalDateTime now = LocalDateTime.now();
        course.setPrice(new BigDecimal("1000.00"));
        course.setDiscountType(DiscountType.PERCENTAGE);
        course.setDiscountValue(new BigDecimal("25.00"));
        course.setDiscountStartDate(now.minusDays(5));
        course.setDiscountEndDate(now.minusDays(1)); // ended yesterday

        CoursePricingResponse response = pricingService.calculatePricing(course, now);

        assertFalse(response.isDiscountActive());
        assertEquals(new BigDecimal("0.00"), response.getDiscountAmount());
        assertEquals(new BigDecimal("1000.00"), response.getDiscountedPrice());
    }

    // =========================================================================
    // 6. ADMIN PRICING & TAX OVERRIDES (FR-A25)
    // =========================================================================

    @Test
    @DisplayName("Admin Override: Admin price rule overrides instructor-defined price")
    void testCalculatePricing_AdminPriceOverrideTakesPrecedence() {
        course.setPrice(new BigDecimal("800.00"));
        when(paymentPolicyService.getAdminOverridePrice(101)).thenReturn(Optional.of(new BigDecimal("1200.00")));

        CoursePricingResponse response = pricingService.calculatePricing(course);

        assertTrue(response.isHasAdminOverride());
        assertEquals(new BigDecimal("1200.00"), response.getBasePrice());
        assertEquals(new BigDecimal("800.00"), response.getInstructorPrice());
        assertEquals(new BigDecimal("1200.00"), response.getAdminOverridePrice());
        // Tax 18% of 1200 = 216.00, total = 1416.00
        assertEquals(new BigDecimal("216.00"), response.getTaxAmount());
        assertEquals(new BigDecimal("1416.00"), response.getFinalPayableAmount());
    }

    @Test
    @DisplayName("Admin Override: Admin custom tax rate overrides default tax rate")
    void testCalculatePricing_AdminTaxOverrideTakesPrecedence() {
        course.setPrice(new BigDecimal("1000.00"));
        when(paymentPolicyService.getTaxRateForCourse(101)).thenReturn(new BigDecimal("0.10")); // 10% tax override

        CoursePricingResponse response = pricingService.calculatePricing(course);

        assertEquals(new BigDecimal("0.10"), response.getTaxRate());
        assertEquals(new BigDecimal("100.00"), response.getTaxAmount());
        assertEquals(new BigDecimal("1100.00"), response.getFinalPayableAmount());
    }

    // =========================================================================
    // 7. ENROLLMENT LIMITS TESTS
    // =========================================================================

    @Test
    @DisplayName("Enrollment Limit: Returns limitReached=false when enrollment is below limit")
    void testEnrollmentLimit_NotReached() {
        course.setEnrollmentLimit(50);
        when(enrollmentRepository.countByCourseId(101)).thenReturn(25L);

        CoursePricingResponse response = pricingService.calculatePricing(course);

        assertEquals(50, response.getEnrollmentLimit());
        assertEquals(25L, response.getCurrentEnrollmentCount());
        assertFalse(response.isEnrollmentLimitReached());
        assertFalse(pricingService.isEnrollmentLimitReached(course));
    }

    @Test
    @DisplayName("Enrollment Limit: Returns limitReached=true when enrollment reaches limit")
    void testEnrollmentLimit_Reached() {
        course.setEnrollmentLimit(50);
        when(enrollmentRepository.countByCourseId(101)).thenReturn(50L);

        CoursePricingResponse response = pricingService.calculatePricing(course);

        assertEquals(50, response.getEnrollmentLimit());
        assertEquals(50L, response.getCurrentEnrollmentCount());
        assertTrue(response.isEnrollmentLimitReached());
        assertTrue(pricingService.isEnrollmentLimitReached(course));
    }

    @Test
    @DisplayName("Enrollment Limit: Null limit means unlimited enrollment")
    void testEnrollmentLimit_Unlimited() {
        course.setEnrollmentLimit(null);
        when(enrollmentRepository.countByCourseId(101)).thenReturn(500L);

        CoursePricingResponse response = pricingService.calculatePricing(course);

        assertNull(response.getEnrollmentLimit());
        assertFalse(response.isEnrollmentLimitReached());
        assertFalse(pricingService.isEnrollmentLimitReached(course));
    }

    // =========================================================================
    // 8. UPDATE PRICING & VALIDATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Update Pricing: Successfully updates course pricing and returns breakdown")
    void testUpdateCoursePricing_Success() {
        when(courseRepository.findById(101)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));

        CoursePricingRequest request = new CoursePricingRequest(
                new BigDecimal("750.00"),
                DiscountType.PERCENTAGE,
                new BigDecimal("15.00"),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7),
                100
        );

        CoursePricingResponse response = pricingService.updateCoursePricing(instructor, 101, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("750.00"), response.getBasePrice());
        assertEquals(DiscountType.PERCENTAGE, response.getDiscountType());
        assertEquals(new BigDecimal("15.00"), response.getDiscountValue());
        assertEquals(100, response.getEnrollmentLimit());
        verify(courseRepository).save(course);
    }

    @Test
    @DisplayName("Update Pricing: Unassociated instructor is denied access")
    void testUpdateCoursePricing_AccessDenied() {
        when(courseRepository.findById(101)).thenReturn(Optional.of(course));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(101, 99)).thenReturn(false);

        User stranger = new User("Stranger", "stranger@olp.com", "hash", "instructor", "active");
        stranger.setId(99);

        CoursePricingRequest request = new CoursePricingRequest(
                new BigDecimal("500.00"), null, null, null, null, null
        );

        assertThrows(AccessDeniedException.class, () ->
                pricingService.updateCoursePricing(stranger, 101, request));
    }

    @Test
    @DisplayName("Validation: Negative price throws IllegalArgumentException")
    void testValidation_NegativePrice() {
        CoursePricingRequest request = new CoursePricingRequest(
                new BigDecimal("-50.00"), null, null, null, null, null
        );
        assertThrows(IllegalArgumentException.class, () -> pricingService.validatePricingRequest(request));
    }

    @Test
    @DisplayName("Validation: Percentage discount > 100 throws IllegalArgumentException")
    void testValidation_PercentageExceeding100() {
        CoursePricingRequest request = new CoursePricingRequest(
                new BigDecimal("500.00"),
                DiscountType.PERCENTAGE,
                new BigDecimal("105.00"),
                null, null, null
        );
        assertThrows(IllegalArgumentException.class, () -> pricingService.validatePricingRequest(request));
    }

    @Test
    @DisplayName("Validation: Fixed discount exceeding price throws IllegalArgumentException")
    void testValidation_FixedDiscountExceedingPrice() {
        CoursePricingRequest request = new CoursePricingRequest(
                new BigDecimal("300.00"),
                DiscountType.FIXED,
                new BigDecimal("400.00"),
                null, null, null
        );
        assertThrows(IllegalArgumentException.class, () -> pricingService.validatePricingRequest(request));
    }

    @Test
    @DisplayName("Validation: End date before start date throws IllegalArgumentException")
    void testValidation_EndDateBeforeStartDate() {
        LocalDateTime now = LocalDateTime.now();
        CoursePricingRequest request = new CoursePricingRequest(
                new BigDecimal("300.00"),
                DiscountType.FIXED,
                new BigDecimal("50.00"),
                now.plusDays(5),
                now.plusDays(1), // before start date
                null
        );
        assertThrows(IllegalArgumentException.class, () -> pricingService.validatePricingRequest(request));
    }

    @Test
    @DisplayName("Validation: Enrollment limit <= 0 throws IllegalArgumentException")
    void testValidation_InvalidEnrollmentLimit() {
        CoursePricingRequest request = new CoursePricingRequest(
                new BigDecimal("300.00"), null, null, null, null, 0
        );
        assertThrows(IllegalArgumentException.class, () -> pricingService.validatePricingRequest(request));
    }
}

