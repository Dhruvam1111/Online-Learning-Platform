package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Unit and Integration Test Suite for FR-I12: Archive or Delete
 * Course.
 * Validates course archiving, catalog hiding, enrolled student access
 * preservation,
 * certificate retention, soft-deletion constraints under DR-ED09, and audit
 * history.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseArchiveAndDeleteTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseInstructorRepository courseInstructorRepository;
    @Mock
    private CoursePrerequisiteRepository coursePrerequisiteRepository;
    @Mock
    private InstructorProfileRepository instructorProfileRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private LectureRepository lectureRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private BookmarkRepository bookmarkRepository;
    @Mock
    private CertificateRepository certificateRepository;
    @Mock
    private CourseAuditRepository courseAuditRepository;
    @Mock
    private CourseSubmissionRepository courseSubmissionRepository;
    @Mock
    private CourseModuleRepository courseModuleRepository;
    @Mock
    private CourseSectionRepository courseSectionRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private PaymentPolicyService paymentPolicyService;

    private CourseAuditService courseAuditService;
    private CoursePricingService coursePricingService;
    private CourseSubmissionService courseSubmissionService;
    private InstructorCourseService instructorCourseService;
    private CourseCatalogService courseCatalogService;

    private User owner;
    private User coInstructor;
    private User strangerInstructor;
    private User enrolledStudent;
    private User unenrolledStudent;

    private Course publishedCourse;
    private Course draftCourse;

    @BeforeEach
    void setUp() {
        owner = new User("Owner Instructor", "owner@olp.com", "hash", "instructor", "active");
        owner.setId(1);

        coInstructor = new User("Co Instructor", "co@olp.com", "hash", "instructor", "active");
        coInstructor.setId(2);

        strangerInstructor = new User("Stranger Instructor", "stranger@olp.com", "hash", "instructor", "active");
        strangerInstructor.setId(3);

        enrolledStudent = new User("Enrolled Student", "student@olp.com", "hash", "student", "active");
        enrolledStudent.setId(10);

        unenrolledStudent = new User("Unenrolled Student", "otherstudent@olp.com", "hash", "student", "active");
        unenrolledStudent.setId(20);

        publishedCourse = new Course(owner, "Cloud Architecture", "Comprehensive cloud design course",
                CourseStatus.PUBLISHED);
        publishedCourse.setId(101);
        publishedCourse.setPrice(new BigDecimal("2999.00"));
        publishedCourse.setEnrollmentLimit(100);

        draftCourse = new Course(owner, "Draft Microservices", "Draft course on microservices", CourseStatus.DRAFT);
        draftCourse.setId(102);

        // CourseSubmissionService
        courseSubmissionService = new CourseSubmissionService(
                courseRepository,
                courseSubmissionRepository,
                courseModuleRepository,
                courseSectionRepository,
                lessonRepository,
                lectureRepository,
                courseInstructorRepository,
                userRepository);

        // CourseAuditService
        courseAuditService = new CourseAuditService(
                courseAuditRepository,
                courseRepository,
                courseInstructorRepository);

        // CoursePricingService
        coursePricingService = new CoursePricingService(
                courseRepository,
                enrollmentRepository,
                courseInstructorRepository,
                paymentPolicyService,
                courseSubmissionService,
                courseAuditService);

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
                courseAuditRepository,
                certificateRepository);

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
                coursePricingService,
                certificateRepository);

        // Default mock behaviors
        when(courseRepository.findById(101)).thenReturn(Optional.of(publishedCourse));
        when(courseRepository.findById(102)).thenReturn(Optional.of(draftCourse));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        when(courseInstructorRepository.existsByCourseIdAndInstructorIdAndRole(101, 1, CourseInstructorRole.OWNER))
                .thenReturn(true);
        when(courseInstructorRepository.existsByCourseIdAndInstructorIdAndRole(101, 2, CourseInstructorRole.OWNER))
                .thenReturn(false);
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(101, 2)).thenReturn(true);

        when(courseInstructorRepository.existsByCourseIdAndInstructorId(101, 3)).thenReturn(false);
        when(courseInstructorRepository.existsByCourseIdAndInstructorIdAndRole(101, 3, CourseInstructorRole.OWNER))
                .thenReturn(false);

        when(enrollmentRepository.existsByStudentIdAndCourseId(10, 101)).thenReturn(true);
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 101)).thenReturn(false);
    }

    @Nested
    @DisplayName("1. Archive Course Tests")
    class ArchiveCourseTests {

        @Test
        @DisplayName("Course owner can archive published course; status becomes ARCHIVED and audit is logged")
        void testArchiveCourse_OwnerSuccess_StatusArchivedAndAuditLogged() {
            CourseResponse response = instructorCourseService.archiveCourse(owner, 101);

            assertNotNull(response);
            assertEquals(CourseStatus.ARCHIVED, response.getStatus());
            assertEquals(CourseStatus.ARCHIVED, publishedCourse.getStatus());

            ArgumentCaptor<CourseAudit> auditCaptor = ArgumentCaptor.forClass(CourseAudit.class);
            verify(courseAuditRepository, atLeastOnce()).save(auditCaptor.capture());

            CourseAudit audit = auditCaptor.getValue();
            assertEquals("COURSE_ARCHIVE", audit.getActionType());
            assertEquals("status", audit.getChangedFields());
            assertTrue(audit.getPreviousValues().contains("PUBLISHED"));
            assertTrue(audit.getNewValues().contains("ARCHIVED"));
        }

        @Test
        @DisplayName("Co-instructor cannot archive course; AccessDeniedException thrown")
        void testArchiveCourse_CoInstructorDenied() {
            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                    () -> instructorCourseService.archiveCourse(coInstructor, 101));
            assertTrue(ex.getMessage().contains("OWNER"));
            assertNotEquals(CourseStatus.ARCHIVED, publishedCourse.getStatus());
        }

        @Test
        @DisplayName("Stranger instructor cannot archive course; AccessDeniedException thrown")
        void testArchiveCourse_StrangerInstructorDenied() {
            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                    () -> instructorCourseService.archiveCourse(strangerInstructor, 101));
            assertTrue(ex.getMessage().contains("OWNER"));
            assertNotEquals(CourseStatus.ARCHIVED, publishedCourse.getStatus());
        }

        @Test
        @DisplayName("Cannot archive course while under PENDING_REVIEW")
        void testArchiveCourse_PendingReviewDenied() {
            publishedCourse.setStatus(CourseStatus.PENDING_REVIEW);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> instructorCourseService.archiveCourse(owner, 101));
            assertTrue(ex.getMessage().contains("PENDING_REVIEW"));
        }

        @Test
        @DisplayName("Archiving an already soft-deleted course throws ResourceNotFoundException")
        void testArchiveCourse_DeletedCourse_ThrowsResourceNotFound() {
            publishedCourse.setDeleted(true);

            assertThrows(Exception.class,
                    () -> instructorCourseService.archiveCourse(owner, 101));
        }

        @Test
        @DisplayName("Archived course is hidden from public catalog")
        void testArchivedCourse_HiddenFromPublicCatalog() {
            publishedCourse.setStatus(CourseStatus.ARCHIVED);
            when(courseRepository.findAll()).thenReturn(List.of(publishedCourse));

            List<PublicCourseResponse> catalog = courseCatalogService.getPublishedCourses(null, null);
            assertTrue(catalog.isEmpty(), "Archived course must not appear in public catalog");
        }

        @Test
        @DisplayName("Enrolled student retains full access to archived course details and lectures")
        void testArchivedCourse_EnrolledStudentRetainsAccess() {
            publishedCourse.setStatus(CourseStatus.ARCHIVED);

            Lecture lecture = new Lecture(publishedCourse, "Cloud Virtual Networks",
                    "https://youtube.com/watch?v=vnet123", 1);
            lecture.setId(501);
            when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(101)).thenReturn(List.of(lecture));

            CourseDetailResponse detail = courseCatalogService.getCourseDetail(101, enrolledStudent);
            assertNotNull(detail);
            assertEquals("Cloud Architecture", detail.getTitle());
            assertTrue(detail.isEnrolled());
            assertEquals(1, detail.getLectures().size());
            assertEquals("https://youtube.com/watch?v=vnet123", detail.getLectures().get(0).getYoutubeUrl(),
                    "Enrolled student must see full video content for archived course");
        }

        @Test
        @DisplayName("Unenrolled student denied access to archived course")
        void testArchivedCourse_UnenrolledStudentDenied() {
            publishedCourse.setStatus(CourseStatus.ARCHIVED);

            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                    () -> courseCatalogService.getCourseDetail(101, unenrolledStudent));
            assertTrue(ex.getMessage().contains("not publicly available"));
        }

        @Test
        @DisplayName("New enrollments on archived course are blocked")
        void testArchivedCourse_NewEnrollmentsBlocked() {
            publishedCourse.setStatus(CourseStatus.ARCHIVED);

            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                    () -> courseCatalogService.enrollCourse(unenrolledStudent, 101));
            assertTrue(ex.getMessage().contains("not published"));
        }

        @Test
        @DisplayName("Archived course preserves issued certificates for student")
        void testArchivedCourse_PreservesCertificates() {
            publishedCourse.setStatus(CourseStatus.ARCHIVED);

            Certificate cert = new Certificate(publishedCourse, enrolledStudent, "CERT-CLOUD-101");
            cert.setId(1);
            when(certificateRepository.findByStudentId(10)).thenReturn(List.of(cert));

            List<CertificateResponse> certs = courseCatalogService.getStudentCertificates(enrolledStudent);
            assertEquals(1, certs.size());
            assertEquals("CERT-CLOUD-101", certs.get(0).getCertificateCode());
            assertEquals(101, certs.get(0).getCourseId());
        }
    }

    @Nested
    @DisplayName("2. Delete Course Tests (Soft Deletion & DR-ED09)")
    class DeleteCourseTests {

        @Test
        @DisplayName("Course owner can soft-delete course when 0 enrollments and 0 certificates exist")
        void testDeleteCourse_OwnerSuccess_ZeroEnrollmentsZeroCertificates() {
            when(enrollmentRepository.countByCourseId(102)).thenReturn(0L);
            when(certificateRepository.countByCourseId(102)).thenReturn(0L);

            instructorCourseService.deleteCourse(owner, 102, true, "Retiring outdated draft course");

            verify(courseRepository, never()).delete(any());
            verify(courseRepository, atLeastOnce()).save(draftCourse);

            assertTrue(draftCourse.isDeleted(), "Course must be marked as soft-deleted");
            assertNotNull(draftCourse.getDeletedAt(), "deletedAt timestamp must be recorded");

            ArgumentCaptor<CourseAudit> auditCaptor = ArgumentCaptor.forClass(CourseAudit.class);
            verify(courseAuditRepository, atLeastOnce()).save(auditCaptor.capture());

            CourseAudit audit = auditCaptor.getValue();
            assertEquals("COURSE_DELETE", audit.getActionType());
            assertEquals("is_deleted", audit.getChangedFields());
            assertTrue(audit.getNewValues().contains("true"));
            assertTrue(audit.getNewValues().contains("Retiring outdated draft course"));
        }

        @Test
        @DisplayName("Co-instructor cannot delete course; AccessDeniedException thrown")
        void testDeleteCourse_CoInstructorDenied() {
            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                    () -> instructorCourseService.deleteCourse(coInstructor, 101));
            assertTrue(ex.getMessage().contains("OWNER"));
            verify(courseRepository, never()).delete(any());
            assertFalse(publishedCourse.isDeleted());
        }

        @Test
        @DisplayName("Stranger instructor cannot delete course; AccessDeniedException thrown")
        void testDeleteCourse_StrangerInstructorDenied() {
            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                    () -> instructorCourseService.deleteCourse(strangerInstructor, 101));
            assertTrue(ex.getMessage().contains("OWNER"));
            verify(courseRepository, never()).delete(any());
            assertFalse(publishedCourse.isDeleted());
        }

        @Test
        @DisplayName("Deletion rejected when active enrollments exist (>0); clear explanatory message returned")
        void testDeleteCourse_WithActiveEnrollments_ThrowsClearExplanation() {
            when(enrollmentRepository.countByCourseId(101)).thenReturn(15L);
            when(certificateRepository.countByCourseId(101)).thenReturn(0L);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> instructorCourseService.deleteCourse(owner, 101));

            assertTrue(ex.getMessage().contains("15 active enrollment(s)"));
            assertTrue(ex.getMessage().contains("DR-ED09"));
            assertTrue(ex.getMessage().contains("archive the course instead"));
            assertFalse(publishedCourse.isDeleted());
            verify(courseRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Deletion rejected when issued certificates exist (>0); clear explanatory message returned")
        void testDeleteCourse_WithIssuedCertificates_ThrowsClearExplanation() {
            when(enrollmentRepository.countByCourseId(101)).thenReturn(0L);
            when(certificateRepository.countByCourseId(101)).thenReturn(5L);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> instructorCourseService.deleteCourse(owner, 101));

            assertTrue(ex.getMessage().contains("5 issued certificate(s)"));
            assertTrue(ex.getMessage().contains("DR-ED09"));
            assertTrue(ex.getMessage().contains("credential verification"));
            assertFalse(publishedCourse.isDeleted());
            verify(courseRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Cannot delete course while under PENDING_REVIEW")
        void testDeleteCourse_PendingReviewDenied() {
            draftCourse.setStatus(CourseStatus.PENDING_REVIEW);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> instructorCourseService.deleteCourse(owner, 102));

            assertTrue(ex.getMessage().contains("PENDING_REVIEW"));
            assertFalse(draftCourse.isDeleted());
        }

        @Test
        @DisplayName("Deletion fails when confirmation is explicitly false")
        void testDeleteCourse_ConfirmationFalse_ThrowsIllegalArgument() {
            when(enrollmentRepository.countByCourseId(102)).thenReturn(0L);
            when(certificateRepository.countByCourseId(102)).thenReturn(0L);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> instructorCourseService.deleteCourse(owner, 102, false));

            assertTrue(ex.getMessage().contains("confirmation"));
            assertFalse(draftCourse.isDeleted());
        }

        @Test
        @DisplayName("Deleting an already soft-deleted course throws ResourceNotFoundException")
        void testDeleteCourse_AlreadyDeletedCourse_ThrowsResourceNotFound() {
            draftCourse.setDeleted(true);

            assertThrows(Exception.class,
                    () -> instructorCourseService.deleteCourse(owner, 102));
        }

        @Test
        @DisplayName("Soft-deleted course is excluded from public course catalog and student access")
        void testDeletedCourse_ExcludedFromPublicCatalogAndStudentAccess() {
            publishedCourse.setDeleted(true);
            when(courseRepository.findAll()).thenReturn(List.of(publishedCourse));

            List<PublicCourseResponse> catalog = courseCatalogService.getPublishedCourses(null, null);
            assertTrue(catalog.isEmpty(), "Deleted course must never appear in public catalog");

            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                    () -> courseCatalogService.getCourseDetail(101, enrolledStudent));
            assertTrue(ex.getMessage().contains("deleted"));
        }

        @Test
        @DisplayName("Soft-deleted course is excluded from instructor course listings")
        void testDeletedCourse_ExcludedFromInstructorListings() {
            Course activeCourse = new Course(owner, "Active", "desc", CourseStatus.PUBLISHED);
            activeCourse.setId(103);
            activeCourse.setDeleted(false);

            publishedCourse.setDeleted(true);

            when(courseRepository.findByInstructorIdOrderByIdAsc(1)).thenReturn(List.of(publishedCourse, activeCourse));

            List<CourseResponse> instructorCourses = instructorCourseService.getInstructorCourses(owner);
            assertEquals(1, instructorCourses.size());
            assertEquals(103, instructorCourses.get(0).getId());
        }
    }
}
