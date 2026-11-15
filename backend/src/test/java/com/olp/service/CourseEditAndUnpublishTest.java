package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Unit and Integration Test Suite for FR-I09: Edit and Unpublish Course.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseEditAndUnpublishTest {

    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private CourseInstructorRepository courseInstructorRepository;
    @Mock private CoursePrerequisiteRepository coursePrerequisiteRepository;
    @Mock private InstructorProfileRepository instructorProfileRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private LectureRepository lectureRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private BookmarkRepository bookmarkRepository;
    @Mock private CourseModuleRepository courseModuleRepository;
    @Mock private CourseSectionRepository courseSectionRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private CourseSubmissionRepository courseSubmissionRepository;
    @Mock private CourseAuditRepository courseAuditRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentPolicyService paymentPolicyService;

    private CourseSubmissionService courseSubmissionService;
    private CourseAuditService courseAuditService;
    private CoursePricingService coursePricingService;
    private InstructorCourseService instructorCourseService;
    private CourseCatalogService courseCatalogService;
    private CourseStructureService courseStructureService;
    private PaymentService paymentService;

    private User owner;
    private User coInstructor;
    private User strangerInstructor;
    private User enrolledStudent;
    private User unenrolledStudent;
    private Course publishedCourse;

    @BeforeEach
    void setUp() {
        owner = new User("Owner Instructor", "owner@olp.com", "hash", "instructor", "active");
        owner.setId(1);

        coInstructor = new User("Co Instructor", "co@olp.com", "hash", "instructor", "active");
        coInstructor.setId(2);

        strangerInstructor = new User("Stranger Instructor", "stranger@olp.com", "hash", "instructor", "active");
        strangerInstructor.setId(3);

        enrolledStudent = new User("Enrolled Student", "enrolled@olp.com", "hash", "student", "active");
        enrolledStudent.setId(10);

        unenrolledStudent = new User("Unenrolled Student", "unenrolled@olp.com", "hash", "student", "active");
        unenrolledStudent.setId(20);

        publishedCourse = new Course(owner, "Spring Boot Masterclass", "Complete guide to Spring Boot", CourseStatus.PUBLISHED);
        publishedCourse.setId(100);
        publishedCourse.setPrice(new BigDecimal("1999.00"));
        publishedCourse.setEnrollmentLimit(500);

        // CourseSubmissionService
        courseSubmissionService = new CourseSubmissionService(
                courseRepository,
                courseSubmissionRepository,
                courseModuleRepository,
                courseSectionRepository,
                lessonRepository,
                lectureRepository,
                courseInstructorRepository,
                userRepository
        );

        // CourseAuditService
        courseAuditService = new CourseAuditService(
                courseAuditRepository,
                courseRepository,
                courseInstructorRepository
        );

        // CoursePricingService
        coursePricingService = new CoursePricingService(
                courseRepository,
                enrollmentRepository,
                courseInstructorRepository,
                paymentPolicyService,
                courseSubmissionService,
                courseAuditService
        );

        // InstructorCourseService
        instructorCourseService = new InstructorCourseService(
                courseRepository,
                userRepository,
                courseInstructorRepository,
                coursePrerequisiteRepository,
                instructorProfileRepository,
                enrollmentRepository,
                coursePricingService,
                null,
                courseSubmissionService,
                courseAuditService,
                courseAuditRepository
        );

        // CourseCatalogService
        courseCatalogService = new CourseCatalogService(
                courseRepository,
                lectureRepository,
                reviewRepository,
                bookmarkRepository,
                enrollmentRepository,
                userRepository,
                courseInstructorRepository,
                instructorProfileRepository,
                coursePricingService
        );

        // CourseStructureService
        courseStructureService = new CourseStructureService(
                courseRepository,
                courseModuleRepository,
                courseSectionRepository,
                lessonRepository,
                courseInstructorRepository,
                userRepository,
                courseSubmissionService,
                courseAuditService
        );

        // PaymentService
        paymentService = new PaymentService(
                paymentRepository,
                courseRepository,
                enrollmentRepository,
                null,
                null,
                null,
                null,
                userRepository,
                null
        );

        // Common mocks
        when(courseRepository.findById(100)).thenReturn(Optional.of(publishedCourse));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        when(courseAuditRepository.save(any(CourseAudit.class))).thenAnswer(inv -> {
            CourseAudit ca = inv.getArgument(0);
            ca.setId(1);
            return ca;
        });
        when(courseSubmissionRepository.save(any(CourseSubmission.class))).thenAnswer(inv -> {
            CourseSubmission cs = inv.getArgument(0);
            cs.setId(1);
            return cs;
        });

        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 1)).thenReturn(true);
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 2)).thenReturn(true);
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 3)).thenReturn(false);

        when(paymentPolicyService.getTaxRateForCourse(anyInt())).thenReturn(new BigDecimal("0.18"));

        when(enrollmentRepository.existsByStudentIdAndCourseId(10, 100)).thenReturn(true);
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 100)).thenReturn(false);
    }

    // =========================================================================
    // 1. EDIT PUBLISHED COURSES — MINOR EDITS
    // =========================================================================
    @Nested
    @DisplayName("1. Minor Edits to Published Courses")
    class MinorEditsTests {

        @Test
        @DisplayName("Minor changes (title and description) are applied directly while remaining PUBLISHED")
        void testMinorEdit_AppliesDirectly_PreservesPublishedStatus() {
            UpdateCourseRequest request = new UpdateCourseRequest("Updated Spring Boot Masterclass", "Updated Description");

            CourseResponse response = instructorCourseService.updateCourse(owner, 100, request);

            assertNotNull(response);
            assertEquals("Updated Spring Boot Masterclass", response.getTitle());
            assertEquals(CourseStatus.PUBLISHED, response.getStatus());
            assertEquals(CourseStatus.PUBLISHED, publishedCourse.getStatus());

            // Verify audit history was recorded with COURSE_EDIT_MINOR
            verify(courseAuditRepository).save(argThat(audit ->
                    audit.getCourse().getId().equals(100) &&
                    audit.getActor().getId().equals(owner.getId()) &&
                    "COURSE_EDIT_MINOR".equals(audit.getActionType()) &&
                    audit.getChangedFields().contains("title") &&
                    audit.getChangedFields().contains("description")
            ));

            // Verify re-review was NOT triggered
            verify(courseSubmissionRepository, never()).save(any(CourseSubmission.class));
        }

        @Test
        @DisplayName("Co-instructor can perform minor edits on published course")
        void testMinorEdit_CoInstructor_Allowed() {
            UpdateCourseRequest request = new UpdateCourseRequest("Co-edited Title", "Co-edited Description");

            CourseResponse response = instructorCourseService.updateCourse(coInstructor, 100, request);

            assertNotNull(response);
            assertEquals("Co-edited Title", response.getTitle());
            assertEquals(CourseStatus.PUBLISHED, response.getStatus());
            verify(courseAuditRepository).save(any(CourseAudit.class));
        }
    }

    // =========================================================================
    // 2. EDIT PUBLISHED COURSES — MAJOR EDITS (PRICING & RE-REVIEW)
    // =========================================================================
    @Nested
    @DisplayName("2. Major Changes Triggering Re-Review")
    class MajorChangesTests {

        @Test
        @DisplayName("Pricing alterations on published course trigger re-review and status becomes PENDING_REVIEW")
        void testMajorPricingEdit_TriggersReReview() {
            UpdateCourseRequest request = new UpdateCourseRequest("Spring Boot Masterclass", "Complete guide to Spring Boot");
            request.setPrice(new BigDecimal("2999.00"));

            CourseResponse response = instructorCourseService.updateCourse(owner, 100, request);

            assertNotNull(response);
            assertEquals(CourseStatus.PENDING_REVIEW, response.getStatus());
            assertEquals(CourseStatus.PENDING_REVIEW, publishedCourse.getStatus());

            // Verify submission record created for review
            verify(courseSubmissionRepository).save(argThat(sub ->
                    sub.getCourse().getId().equals(100) &&
                    sub.getInstructor().getId().equals(owner.getId()) &&
                    "PENDING_REVIEW".equals(sub.getStatus()) &&
                    sub.getComments().contains("re-review")
            ));

            // Verify audit recorded with COURSE_EDIT_MAJOR
            verify(courseAuditRepository).save(argThat(audit ->
                    audit.getCourse().getId().equals(100) &&
                    "COURSE_EDIT_MAJOR".equals(audit.getActionType()) &&
                    audit.getChangedFields().contains("price")
            ));
        }

        @Test
        @DisplayName("Discount changes on published course trigger re-review")
        void testMajorDiscountEdit_TriggersReReview() {
            UpdateCourseRequest request = new UpdateCourseRequest("Spring Boot Masterclass", "Complete guide to Spring Boot");
            request.setDiscountType(DiscountType.PERCENTAGE);
            request.setDiscountValue(new BigDecimal("20.00"));

            CourseResponse response = instructorCourseService.updateCourse(owner, 100, request);

            assertNotNull(response);
            assertEquals(CourseStatus.PENDING_REVIEW, response.getStatus());
            assertEquals(CourseStatus.PENDING_REVIEW, publishedCourse.getStatus());
            verify(courseSubmissionRepository).save(any(CourseSubmission.class));
        }

        @Test
        @DisplayName("Direct updateCoursePricing on published course triggers re-review")
        void testDirectUpdateCoursePricing_TriggersReReview() {
            CoursePricingRequest pricingReq = new CoursePricingRequest(
                    new BigDecimal("3499.00"),
                    DiscountType.FIXED,
                    new BigDecimal("500.00"),
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(10),
                    200
            );

            CoursePricingResponse pricingResp = instructorCourseService.updateCoursePricing(owner, 100, pricingReq);

            assertNotNull(pricingResp);
            assertEquals(CourseStatus.PENDING_REVIEW, publishedCourse.getStatus());
            verify(courseSubmissionRepository).save(any(CourseSubmission.class));
            verify(courseAuditRepository).save(argThat(audit ->
                    "COURSE_EDIT_MAJOR".equals(audit.getActionType())
            ));
        }

        @Test
        @DisplayName("Course structure changes (module creation) on published course trigger re-review")
        void testCourseStructureModification_TriggersReReview() {
            CourseModuleRequest moduleReq = new CourseModuleRequest("New Advanced Module", "Advanced topics", 1);
            when(courseModuleRepository.save(any(CourseModule.class))).thenAnswer(inv -> {
                CourseModule m = inv.getArgument(0);
                m.setId(10);
                return m;
            });

            CourseModuleResponse modResp = courseStructureService.createModule(owner, 100, moduleReq);

            assertNotNull(modResp);
            assertEquals("PENDING_REVIEW", publishedCourse.getStatus().name());
            verify(courseSubmissionRepository).save(any(CourseSubmission.class));
            verify(courseAuditRepository).save(argThat(audit ->
                    "COURSE_STRUCTURE_EDIT_MAJOR".equals(audit.getActionType())
            ));
        }
    }

    // =========================================================================
    // 3. UNPUBLISH COURSE
    // =========================================================================
    @Nested
    @DisplayName("3. Unpublishing Course")
    class UnpublishTests {

        @Test
        @DisplayName("Successfully unpublishes a published course and records audit history")
        void testUnpublishCourse_Success() {
            CourseResponse response = instructorCourseService.unpublishCourse(owner, 100);

            assertNotNull(response);
            assertEquals(CourseStatus.UNPUBLISHED, response.getStatus());
            assertEquals(CourseStatus.UNPUBLISHED, publishedCourse.getStatus());

            // Verify audit record
            verify(courseAuditRepository).save(argThat(audit ->
                    audit.getCourse().getId().equals(100) &&
                    audit.getActor().getId().equals(owner.getId()) &&
                    "COURSE_UNPUBLISH".equals(audit.getActionType()) &&
                    audit.getPreviousValues().contains("PUBLISHED") &&
                    audit.getNewValues().contains("UNPUBLISHED")
            ));
        }

        @Test
        @DisplayName("Unpublishing a draft or non-published course throws IllegalStateException")
        void testUnpublishCourse_NotPublished_ThrowsException() {
            publishedCourse.setStatus(CourseStatus.DRAFT);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    instructorCourseService.unpublishCourse(owner, 100));

            assertTrue(ex.getMessage().contains("Cannot unpublish course that is not currently published"));
            assertEquals(CourseStatus.DRAFT, publishedCourse.getStatus());
            verify(courseAuditRepository, never()).save(any(CourseAudit.class));
        }

        @Test
        @DisplayName("Changing status to UNPUBLISHED via updateCourseStatus routes to unpublish workflow")
        void testUpdateCourseStatus_ToUnpublished_DelegatesToUnpublish() {
            UpdateCourseStatusRequest req = new UpdateCourseStatusRequest("UNPUBLISHED");

            CourseResponse response = instructorCourseService.updateCourseStatus(owner, 100, req);

            assertNotNull(response);
            assertEquals(CourseStatus.UNPUBLISHED, response.getStatus());
            verify(courseAuditRepository).save(argThat(a -> "COURSE_UNPUBLISH".equals(a.getActionType())));
        }
    }

    // =========================================================================
    // 4. CATALOGUE VISIBILITY, RETENTION, AND ENROLLMENT PREVENTION
    // =========================================================================
    @Nested
    @DisplayName("4. Catalogue Visibility, Access Retention & Enrollment Prevention")
    class CatalogueAndAccessTests {

        @BeforeEach
        void setupUnpublished() {
            publishedCourse.setStatus(CourseStatus.UNPUBLISHED);
        }

        @Test
        @DisplayName("Unpublished course is excluded from public catalogue")
        void testUnpublishedCourse_ExcludedFromPublicCatalog() {
            when(courseRepository.findAll()).thenReturn(List.of(publishedCourse));

            List<PublicCourseResponse> catalog = courseCatalogService.getPublishedCourses(null, null);

            assertTrue(catalog.isEmpty(), "Unpublished course must not appear in public catalog");
        }

        @Test
        @DisplayName("Anonymous users cannot access unpublished course details")
        void testUnpublishedCourse_AnonymousUser_AccessDenied() {
            assertThrows(AccessDeniedException.class, () ->
                    courseCatalogService.getCourseDetail(100, null));
        }

        @Test
        @DisplayName("Unenrolled students cannot access unpublished course details")
        void testUnpublishedCourse_UnenrolledStudent_AccessDenied() {
            assertThrows(AccessDeniedException.class, () ->
                    courseCatalogService.getCourseDetail(100, unenrolledStudent));
        }

        @Test
        @DisplayName("Enrolled students retain access to unpublished course details and learning materials")
        void testUnpublishedCourse_EnrolledStudent_RetainsAccess() {
            Lecture lecture = new Lecture(publishedCourse, "Lecture 1", "https://storage.olp.com/lectures/video1.mp4", 1);
            lecture.setId(50);
            when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(100)).thenReturn(List.of(lecture));

            CourseDetailResponse detail = courseCatalogService.getCourseDetail(100, enrolledStudent);

            assertNotNull(detail);
            assertEquals("UNPUBLISHED", detail.getStatus());
            assertTrue(detail.isEnrolled());

            List<LectureResponse> lectures = courseCatalogService.getCourseLectures(100, enrolledStudent);
            assertNotNull(lectures);
            assertEquals(1, lectures.size());
            assertEquals("https://storage.olp.com/lectures/video1.mp4", lectures.get(0).getVideoUrl(),
                    "Enrolled student must receive full lecture learning content");
        }

        @Test
        @DisplayName("New student enrollment is prevented while course is unpublished")
        void testUnpublishedCourse_PreventNewEnrollment() {
            AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                    courseCatalogService.enrollCourse(unenrolledStudent, 100));

            assertTrue(ex.getMessage().contains("Cannot enroll in a course that is not published"));
            verify(enrollmentRepository, never()).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("Payment order initialization is prevented while course is unpublished")
        void testUnpublishedCourse_PreventPaymentOrder() {
            CreatePaymentOrderRequest orderReq = new CreatePaymentOrderRequest(100, new BigDecimal("1999.00"), "INR");

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    paymentService.createPaymentOrder(unenrolledStudent, orderReq));

            assertTrue(ex.getMessage().contains("This course is not currently available for purchase"));
        }
    }

    // =========================================================================
    // 5. AUDIT HISTORY PRESERVATION & RESTRICTIONS
    // =========================================================================
    @Nested
    @DisplayName("5. Audit History Creation, Preservation & Access Control")
    class AuditHistoryTests {

        @Test
        @DisplayName("Audit history records multiple actions and preserves them without overwriting")
        void testAuditHistory_PreservesMultipleRecords() {
            CourseAudit audit1 = new CourseAudit(publishedCourse, owner, "COURSE_EDIT_MINOR", "title", "{}", "{}");
            audit1.setId(1);
            CourseAudit audit2 = new CourseAudit(publishedCourse, owner, "COURSE_UNPUBLISH", "status", "{}", "{}");
            audit2.setId(2);

            when(courseAuditRepository.findByCourseIdOrderByCreatedAtDesc(100))
                    .thenReturn(List.of(audit2, audit1));

            List<CourseAuditResponse> audits = instructorCourseService.getCourseAudits(owner, 100);

            assertNotNull(audits);
            assertEquals(2, audits.size());
            assertEquals("COURSE_UNPUBLISH", audits.get(0).getActionType());
            assertEquals("COURSE_EDIT_MINOR", audits.get(1).getActionType());
        }

        @Test
        @DisplayName("Stranger instructor cannot view course audit history")
        void testAuditHistory_StrangerInstructor_AccessDenied() {
            assertThrows(AccessDeniedException.class, () ->
                    instructorCourseService.getCourseAudits(strangerInstructor, 100));
        }

        @Test
        @DisplayName("Stranger instructor cannot edit or unpublish another instructor's course")
        void testStrangerInstructor_CannotEditOrUnpublish() {
            UpdateCourseRequest req = new UpdateCourseRequest("Hacked Title", "Hacked Desc");

            assertThrows(AccessDeniedException.class, () ->
                    instructorCourseService.updateCourse(strangerInstructor, 100, req));

            assertThrows(AccessDeniedException.class, () ->
                    instructorCourseService.unpublishCourse(strangerInstructor, 100));
        }

        @Test
        @DisplayName("Student cannot access instructor course edit or unpublish operations")
        void testStudentRole_CannotAccessInstructorOperations() {
            UpdateCourseRequest req = new UpdateCourseRequest("Student Edit", "Desc");

            assertThrows(AccessDeniedException.class, () ->
                    instructorCourseService.updateCourse(enrolledStudent, 100, req));

            assertThrows(AccessDeniedException.class, () ->
                    instructorCourseService.unpublishCourse(enrolledStudent, 100));
        }
    }
}
