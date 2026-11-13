package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service providing student preview of courses for authorized instructors (FR-I73).
 * Renders complete course content (including draft data, modules, lessons, and pricing)
 * in a strictly read-only mode with enrollment and progress tracking disabled.
 */
@Service
public class CoursePreviewService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final CourseSectionRepository courseSectionRepository;
    private final LessonRepository lessonRepository;
    private final LectureRepository lectureRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final InstructorProfileRepository instructorProfileRepository;
    private final CoursePrerequisiteRepository coursePrerequisiteRepository;
    private final QuizRepository quizRepository;
    private final CoursePricingService coursePricingService;
    private final UserRepository userRepository;

    public CoursePreviewService(
            CourseRepository courseRepository,
            CourseModuleRepository courseModuleRepository,
            CourseSectionRepository courseSectionRepository,
            LessonRepository lessonRepository,
            LectureRepository lectureRepository,
            UserRepository userRepository) {
        this(courseRepository, courseModuleRepository, courseSectionRepository, lessonRepository,
                lectureRepository, null, null, null, null, null, userRepository);
    }

    @Autowired
    public CoursePreviewService(
            CourseRepository courseRepository,
            @Autowired(required = false) CourseModuleRepository courseModuleRepository,
            @Autowired(required = false) CourseSectionRepository courseSectionRepository,
            @Autowired(required = false) LessonRepository lessonRepository,
            @Autowired(required = false) LectureRepository lectureRepository,
            @Autowired(required = false) CourseInstructorRepository courseInstructorRepository,
            @Autowired(required = false) InstructorProfileRepository instructorProfileRepository,
            @Autowired(required = false) CoursePrerequisiteRepository coursePrerequisiteRepository,
            @Autowired(required = false) QuizRepository quizRepository,
            @Autowired(required = false) CoursePricingService coursePricingService,
            UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.courseSectionRepository = courseSectionRepository;
        this.lessonRepository = lessonRepository;
        this.lectureRepository = lectureRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.instructorProfileRepository = instructorProfileRepository;
        this.coursePrerequisiteRepository = coursePrerequisiteRepository;
        this.quizRepository = quizRepository;
        this.coursePricingService = coursePricingService;
        this.userRepository = userRepository;
    }

    /**
     * Resolves the current User entity from Spring Security Authentication.
     */
    public User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
    }

    /**
     * FR-I73: Previews a course exactly as a student would see it, using draft data.
     * Enforces strict read-only mode: enrollments and progress tracking are disabled,
     * and a clear preview indicator banner is returned in the response.
     */
    @Transactional(readOnly = true)
    public CoursePreviewResponse previewCourseAsStudent(User currentUser, Integer courseId) {
        validateInstructorOrAdmin(currentUser);

        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        verifyCourseAccess(course, currentUser);

        CoursePreviewResponse response = new CoursePreviewResponse();

        // 1. Read-only banner and action disabling
        response.setPreview(true);
        response.setReadOnly(true);
        response.setBannerMessage("You are previewing this course as an instructor. Enrollments and progress tracking are disabled.");
        response.setCanEnroll(false);
        response.setEnrolled(false);
        response.setProgressTrackingEnabled(false);

        // 2. Core course attributes (including draft status)
        response.setId(course.getId());
        response.setTitle(course.getTitle());
        response.setDescription(course.getDescription());
        response.setCategory(course.getCategory());
        response.setStatus(course.getStatus() != null ? course.getStatus().name() : "DRAFT");
        response.setCreatedAt(course.getCreatedAt());

        if (course.getInstructor() != null) {
            response.setInstructorId(course.getInstructor().getId());
            response.setInstructorName(course.getInstructor().getName());
        }

        // 3. Instructors (owner and co-instructors)
        List<CourseInstructorResponse> instructors = CourseInstructorResponse.resolveInstructorsForCourse(
                course, courseInstructorRepository, instructorProfileRepository);
        response.setInstructors(instructors);

        // 4. Learning outcomes & Prerequisites
        response.setLearningOutcomes(course.getLearningOutcomesList() != null ? course.getLearningOutcomesList() : new ArrayList<>());
        if (coursePrerequisiteRepository != null) {
            List<CoursePrerequisite> prereqs = coursePrerequisiteRepository.findByCourseIdOrderByIdAsc(courseId);
            response.setPrerequisites(prereqs != null ? prereqs.stream().map(CoursePrerequisiteResponse::fromEntity).toList() : new ArrayList<>());
        }

        // 5. Structure: Modules -> Sections -> Lessons
        List<CourseModuleResponse> moduleResponses = new ArrayList<>();
        int totalSectionsCount = 0;
        int totalLessonsCount = 0;

        List<CourseModule> modules = null;
        if (courseModuleRepository != null) {
            modules = courseModuleRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        }
        if ((modules == null || modules.isEmpty()) && course.getModules() != null) {
            modules = course.getModules();
        }
        if (modules == null) {
            modules = Collections.emptyList();
        }

        for (CourseModule mod : modules) {
            List<CourseSection> sections = null;
            if (courseSectionRepository != null && mod.getId() != null) {
                sections = courseSectionRepository.findByModuleIdOrderByOrderIndexAsc(mod.getId());
            }
            if ((sections == null || sections.isEmpty()) && mod.getSections() != null) {
                sections = mod.getSections();
            }
            if (sections == null) {
                sections = Collections.emptyList();
            }

            List<CourseSectionResponse> sectionResponses = new ArrayList<>();
            for (CourseSection sec : sections) {
                List<Lesson> lessons = null;
                if (lessonRepository != null && sec.getId() != null) {
                    lessons = lessonRepository.findBySectionIdOrderByOrderIndexAsc(sec.getId());
                }
                if ((lessons == null || lessons.isEmpty()) && sec.getLessons() != null) {
                    lessons = sec.getLessons();
                }
                if (lessons == null) {
                    lessons = Collections.emptyList();
                }

                List<LessonResponse> lessonResponses = lessons.stream()
                        .map(LessonResponse::fromEntity)
                        .toList();

                CourseSectionResponse secResp = CourseSectionResponse.fromEntity(sec, false);
                if (secResp != null) {
                    secResp.setLessons(lessonResponses);
                    sectionResponses.add(secResp);
                }
                totalLessonsCount += lessons.size();
            }

            CourseModuleResponse modResp = CourseModuleResponse.fromEntity(mod, false);
            if (modResp != null) {
                modResp.setSections(sectionResponses);
                moduleResponses.add(modResp);
            }
            totalSectionsCount += sectionResponses.size();
        }

        response.setModules(moduleResponses);
        response.setTotalModules(moduleResponses.size());
        response.setTotalSections(totalSectionsCount);
        response.setTotalLessons(totalLessonsCount);

        // 6. Lectures
        List<LectureResponse> lectureResponses = new ArrayList<>();
        if (lectureRepository != null) {
            List<Lecture> lectures = lectureRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
            if (lectures != null) {
                lectureResponses = lectures.stream().map(LectureResponse::fromEntity).toList();
            }
        }
        response.setLectures(lectureResponses);
        response.setTotalLectures(lectureResponses.size());

        // 7. Quizzes
        if (quizRepository != null) {
            List<Quiz> quizzes = quizRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
            if (quizzes != null) {
                List<QuizResponse> quizResponses = quizzes.stream().map(q -> {
                    QuizResponse qr = new QuizResponse();
                    qr.setId(q.getId());
                    qr.setCourseId(courseId);
                    qr.setTitle(q.getTitle());
                    qr.setDescription(q.getDescription());
                    qr.setStatus(q.getStatus() != null ? q.getStatus().name() : "DRAFT");
                    qr.setTotalMarks(q.getTotalMarks());
                    qr.setPassingMarks(q.getPassingMarks());
                    qr.setTimeLimitMinutes(q.getTimeLimitMinutes());
                    qr.setNumberOfQuestions(q.getNumberOfQuestions());
                    qr.setMaxAttempts(q.getMaxAttempts());
                    qr.setAvailableFrom(q.getAvailableFrom());
                    qr.setAvailableUntil(q.getAvailableUntil());
                    return qr;
                }).toList();
                response.setQuizzes(quizResponses);
            }
        }

        // 8. Pricing & Discounts
        if (coursePricingService != null) {
            CoursePricingResponse pricing = coursePricingService.calculatePricing(course);
            response.setPricing(pricing);
            if (pricing != null) {
                response.setPrice(pricing.getBasePrice());
                response.setDiscountType(pricing.getDiscountType());
                response.setDiscountValue(pricing.getDiscountValue());
                response.setDiscountStartDate(pricing.getDiscountStartDate());
                response.setDiscountEndDate(pricing.getDiscountEndDate());
                response.setDiscountAmount(pricing.getDiscountAmount());
                response.setDiscountedPrice(pricing.getDiscountedPrice());
                response.setTaxRate(pricing.getTaxRate());
                response.setTaxAmount(pricing.getTaxAmount());
                response.setFinalPayableAmount(pricing.getFinalPayableAmount());
                response.setFree(pricing.isFree());
                response.setEnrollmentLimit(pricing.getEnrollmentLimit());
                response.setEnrollmentLimitReached(pricing.isEnrollmentLimitReached());
            }
        } else {
            BigDecimal basePrice = course.getPrice() != null ? course.getPrice().setScale(2, RoundingMode.HALF_UP) : new BigDecimal("0.00");
            response.setPrice(basePrice);
            response.setFree(course.isFree());
            response.setDiscountType(course.getDiscountType());
            response.setDiscountValue(course.getDiscountValue() != null ? course.getDiscountValue().setScale(2, RoundingMode.HALF_UP) : new BigDecimal("0.00"));
            response.setDiscountStartDate(course.getDiscountStartDate());
            response.setDiscountEndDate(course.getDiscountEndDate());
            response.setFinalPayableAmount(basePrice);
        }

        // 9. Incomplete Course Content Warnings (helps instructors catch missing content before review)
        List<String> warnings = new ArrayList<>();
        if (course.getDescription() == null || course.getDescription().trim().isEmpty()) {
            warnings.add("Course description is missing");
        }
        if (course.getCategory() == null || course.getCategory().trim().isEmpty() || "General".equalsIgnoreCase(course.getCategory().trim())) {
            warnings.add("Course category is not set (defaulting to General)");
        }
        if (moduleResponses.isEmpty() && lectureResponses.isEmpty()) {
            warnings.add("Course has no modules or lectures configured");
        } else {
            for (CourseModuleResponse modResp : moduleResponses) {
                if (modResp.getSections() == null || modResp.getSections().isEmpty()) {
                    warnings.add("Module '" + modResp.getTitle() + "' has no sections");
                } else {
                    for (CourseSectionResponse secResp : modResp.getSections()) {
                        if (secResp.getLessons() == null || secResp.getLessons().isEmpty()) {
                            warnings.add("Section '" + secResp.getTitle() + "' in module '" + modResp.getTitle() + "' has no lessons");
                        }
                    }
                }
            }
        }
        if (course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) == 0) {
            warnings.add("Course price is not configured (defaults to Free)");
        }
        response.setMissingContentWarnings(warnings);

        return response;
    }

    /**
     * Validates that the user is an active instructor or an admin.
     */
    private void validateInstructorOrAdmin(User currentUser) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        String role = currentUser.getRole();
        if (!"instructor".equalsIgnoreCase(role) && !"admin".equalsIgnoreCase(role)) {
            throw new AccessDeniedException("Access denied: Only instructors or admins can preview courses");
        }
        if ("instructor".equalsIgnoreCase(role) && !"active".equalsIgnoreCase(currentUser.getAccountStatus())) {
            throw new AccessDeniedException("Access denied: Instructor account is awaiting admin approval");
        }
    }

    /**
     * Verifies that the instructor is associated with the course (owner or co-instructor) or an admin.
     */
    private void verifyCourseAccess(Course course, User currentUser) {
        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            return;
        }
        boolean isOwner = course.getInstructor() != null
                && currentUser.getId().equals(course.getInstructor().getId());
        boolean isCoInstructor = courseInstructorRepository != null
                && courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), currentUser.getId());

        if (!isOwner && !isCoInstructor) {
            throw new AccessDeniedException("Cannot access preview for a course you are not associated with");
        }
    }
}

