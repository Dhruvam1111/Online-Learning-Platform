package com.olp.service;

import com.olp.dto.CoursePricingRequest;
import com.olp.dto.CoursePricingResponse;
import com.olp.entity.Course;
import com.olp.entity.CourseStatus;
import com.olp.entity.DiscountType;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.EnrollmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CoursePricingService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final PaymentPolicyService paymentPolicyService;
    private final CourseSubmissionService courseSubmissionService;
    private final CourseAuditService courseAuditService;

    public CoursePricingService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            @Autowired(required = false) CourseInstructorRepository courseInstructorRepository,
            @Autowired(required = false) PaymentPolicyService paymentPolicyService) {
        this(courseRepository, enrollmentRepository, courseInstructorRepository, paymentPolicyService, null, null);
    }

    @Autowired
    public CoursePricingService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            @Autowired(required = false) CourseInstructorRepository courseInstructorRepository,
            @Autowired(required = false) PaymentPolicyService paymentPolicyService,
            @Autowired(required = false) CourseSubmissionService courseSubmissionService,
            @Autowired(required = false) CourseAuditService courseAuditService) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.paymentPolicyService = paymentPolicyService;
        this.courseSubmissionService = courseSubmissionService;
        this.courseAuditService = courseAuditService;
    }

    /**
     * Compute pricing breakdown, discount, tax, and final payable amount (DR-PY03).
     * Admin pricing and commission rules (FR-A25) override instructor-defined prices.
     */
    @Transactional(readOnly = true)
    public CoursePricingResponse calculatePricing(Integer courseId) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        return calculatePricing(course, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public CoursePricingResponse calculatePricing(Course course) {
        return calculatePricing(course, LocalDateTime.now());
    }

    public CoursePricingResponse calculatePricing(Course course, LocalDateTime evaluationTime) {
        if (course == null) {
            return null;
        }
        LocalDateTime now = evaluationTime != null ? evaluationTime : LocalDateTime.now();

        BigDecimal instructorPrice = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;
        BigDecimal basePrice = instructorPrice;
        boolean hasAdminOverride = false;
        BigDecimal adminOverridePrice = null;

        // Check if Admin has configured a pricing rule overriding instructor values (FR-A25)
        if (paymentPolicyService != null && course.getId() != null) {
            Optional<BigDecimal> adminOverrideOpt = paymentPolicyService.getAdminOverridePrice(course.getId());
            if (adminOverrideOpt.isPresent()) {
                basePrice = adminOverrideOpt.get();
                hasAdminOverride = true;
                adminOverridePrice = basePrice;
            }
        }

        boolean isFree = basePrice.compareTo(BigDecimal.ZERO) == 0;
        BigDecimal discountAmount = BigDecimal.ZERO;
        boolean isDiscountActive = false;

        if (!isFree && course.getDiscountType() != null && course.getDiscountValue() != null &&
                course.getDiscountValue().compareTo(BigDecimal.ZERO) > 0) {
            boolean validStart = course.getDiscountStartDate() == null || !now.isBefore(course.getDiscountStartDate());
            boolean validEnd = course.getDiscountEndDate() == null || !now.isAfter(course.getDiscountEndDate());

            if (validStart && validEnd) {
                isDiscountActive = true;
                if (course.getDiscountType() == DiscountType.PERCENTAGE) {
                    discountAmount = basePrice.multiply(course.getDiscountValue())
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                            .min(basePrice)
                            .max(BigDecimal.ZERO);
                } else if (course.getDiscountType() == DiscountType.FIXED) {
                    discountAmount = course.getDiscountValue()
                            .min(basePrice)
                            .max(BigDecimal.ZERO);
                }
            }
        }

        BigDecimal discountedPrice = basePrice.subtract(discountAmount).max(BigDecimal.ZERO);

        // Tax calculation according to DR-PY03: price - discount + tax
        BigDecimal taxRate = paymentPolicyService != null && course.getId() != null
                ? paymentPolicyService.getTaxRateForCourse(course.getId())
                : new BigDecimal("0.18");

        BigDecimal taxAmount = isFree ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : discountedPrice.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalPayableAmount = isFree ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : discountedPrice.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        long currentEnrollmentCount = 0L;
        if (enrollmentRepository != null && course.getId() != null) {
            currentEnrollmentCount = enrollmentRepository.countByCourseId(course.getId());
        }

        boolean isLimitReached = course.getEnrollmentLimit() != null &&
                course.getEnrollmentLimit() > 0 &&
                currentEnrollmentCount >= course.getEnrollmentLimit();

        CoursePricingResponse response = new CoursePricingResponse();
        response.setCourseId(course.getId());
        response.setCourseTitle(course.getTitle());
        response.setBasePrice(basePrice.setScale(2, RoundingMode.HALF_UP));
        response.setInstructorPrice(instructorPrice.setScale(2, RoundingMode.HALF_UP));
        response.setDiscountType(course.getDiscountType());
        response.setDiscountValue(course.getDiscountValue() != null ? course.getDiscountValue().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2));
        response.setDiscountStartDate(course.getDiscountStartDate());
        response.setDiscountEndDate(course.getDiscountEndDate());
        response.setDiscountActive(isDiscountActive);
        response.setDiscountAmount(discountAmount.setScale(2, RoundingMode.HALF_UP));
        response.setDiscountedPrice(discountedPrice.setScale(2, RoundingMode.HALF_UP));
        response.setTaxRate(taxRate);
        response.setTaxAmount(taxAmount.setScale(2, RoundingMode.HALF_UP));
        response.setFinalPayableAmount(finalPayableAmount.setScale(2, RoundingMode.HALF_UP));
        response.setFree(isFree);
        response.setEnrollmentLimit(course.getEnrollmentLimit());
        response.setCurrentEnrollmentCount(currentEnrollmentCount);
        response.setEnrollmentLimitReached(isLimitReached);
        response.setHasAdminOverride(hasAdminOverride);
        response.setAdminOverridePrice(adminOverridePrice != null ? adminOverridePrice.setScale(2, RoundingMode.HALF_UP) : null);

        return response;
    }

    /**
     * Checks whether a course has reached its configured enrollment limit.
     */
    @Transactional(readOnly = true)
    public boolean isEnrollmentLimitReached(Integer courseId) {
        if (courseId == null) {
            return false;
        }
        Course course = courseRepository.findById(courseId).orElse(null);
        return isEnrollmentLimitReached(course);
    }

    @Transactional(readOnly = true)
    public boolean isEnrollmentLimitReached(Course course) {
        if (course == null || course.getEnrollmentLimit() == null || course.getEnrollmentLimit() <= 0) {
            return false;
        }
        if (enrollmentRepository == null || course.getId() == null) {
            return false;
        }
        long enrolled = enrollmentRepository.countByCourseId(course.getId());
        return enrolled >= course.getEnrollmentLimit();
    }

    /**
     * Update pricing settings on a course with authorization check.
     */
    @Transactional
    public CoursePricingResponse updateCoursePricing(User currentUser, Integer courseId, CoursePricingRequest request) {
        if (currentUser == null) {
            throw new AccessDeniedException("User must be authenticated");
        }
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Pricing request cannot be null");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        verifyInstructorAccess(currentUser, course);
        if (course.getStatus() == CourseStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Cannot update course pricing while it is under review (status is PENDING_REVIEW)");
        }
        validatePricingRequest(request);

        CourseStatus prevStatus = course.getStatus();
        BigDecimal prevPrice = course.getPrice();
        applyPricing(course, request);

        Course saved;
        if (prevStatus == CourseStatus.PUBLISHED && courseSubmissionService != null) {
            courseSubmissionService.triggerReReview(
                    currentUser,
                    course,
                    "Course pricing update on published course triggered re-review");
            saved = courseRepository.findById(courseId).orElse(course);
        } else {
            saved = courseRepository.save(course);
        }

        if (courseAuditService != null) {
            courseAuditService.recordAudit(
                    saved,
                    currentUser,
                    prevStatus == CourseStatus.PUBLISHED ? "COURSE_EDIT_MAJOR" : "COURSE_EDIT",
                    "pricing",
                    "price: " + prevPrice + ", status: " + prevStatus,
                    "price: " + saved.getPrice() + ", status: " + saved.getStatus());
        }

        return calculatePricing(saved);
    }

    /**
     * Apply validated pricing fields onto a course entity.
     */
    public void applyPricing(Course course, CoursePricingRequest request) {
        if (course == null || request == null) {
            return;
        }
        if (request.getPrice() != null) {
            course.setPrice(request.getPrice().setScale(2, RoundingMode.HALF_UP));
        }
        course.setDiscountType(request.getDiscountType());
        if (request.getDiscountValue() != null) {
            course.setDiscountValue(request.getDiscountValue().setScale(2, RoundingMode.HALF_UP));
        } else {
            course.setDiscountValue(BigDecimal.ZERO);
        }
        course.setDiscountStartDate(request.getDiscountStartDate());
        course.setDiscountEndDate(request.getDiscountEndDate());
        course.setEnrollmentLimit(request.getEnrollmentLimit());
    }

    /**
     * Validates pricing values, percentages, fixed discounts, and validity ranges.
     */
    public void validatePricingRequest(CoursePricingRequest request) {
        if (request == null) {
            return;
        }
        BigDecimal price = request.getPrice();
        if (price != null) {
            if (price.signum() < 0) {
                throw new IllegalArgumentException("Price cannot be negative");
            }
            if (price.compareTo(new BigDecimal("1000000")) > 0) {
                throw new IllegalArgumentException("Price cannot exceed 1,000,000 INR");
            }
            if (price.stripTrailingZeros().scale() > 2) {
                throw new IllegalArgumentException("Price cannot have more than 2 decimal places");
            }
        }

        DiscountType discountType = request.getDiscountType();
        BigDecimal discountValue = request.getDiscountValue();
        if (discountValue != null && discountValue.compareTo(BigDecimal.ZERO) > 0) {
            if (discountType == null) {
                throw new IllegalArgumentException("Discount type must be specified when discount value is provided");
            }
            if (discountValue.signum() < 0) {
                throw new IllegalArgumentException("Discount value cannot be negative");
            }
            if (discountType == DiscountType.PERCENTAGE) {
                if (discountValue.compareTo(new BigDecimal("100")) > 0) {
                    throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
                }
            } else if (discountType == DiscountType.FIXED) {
                if (price != null && discountValue.compareTo(price) > 0) {
                    throw new IllegalArgumentException("Fixed discount amount cannot exceed the course price");
                }
            }
        }

        if (request.getDiscountStartDate() != null && request.getDiscountEndDate() != null) {
            if (request.getDiscountEndDate().isBefore(request.getDiscountStartDate())) {
                throw new IllegalArgumentException("Discount end date cannot be before discount start date");
            }
        }

        if (request.getEnrollmentLimit() != null && request.getEnrollmentLimit() <= 0) {
            throw new IllegalArgumentException("Enrollment limit must be greater than zero");
        }
    }

    private void verifyInstructorAccess(User currentUser, Course course) {
        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            return;
        }
        boolean isOwner = course.getInstructor() != null && currentUser.getId().equals(course.getInstructor().getId());
        if (isOwner) {
            return;
        }
        if (courseInstructorRepository != null && course.getId() != null) {
            if (courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), currentUser.getId())) {
                return;
            }
        }
        throw new AccessDeniedException("Not authorized to manage pricing for this course");
    }
}

