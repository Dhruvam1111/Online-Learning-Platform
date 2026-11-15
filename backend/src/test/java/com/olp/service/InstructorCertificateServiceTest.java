package com.olp.service;

import com.olp.dto.CertificateResponse;
import com.olp.dto.PageResponse;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test Suite for FR-I57: View Certificates Issued.
 * Tests certificate viewing for associated instructors (owner & co-instructor),
 * access control (DR-05), course and issue-date filtering, pagination, and
 * public certificate verification (DR-02).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InstructorCertificateServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseCompletionCriteriaRepository completionCriteriaRepository;
    @Mock
    private CertificateRepository certificateRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LessonProgressRepository lessonProgressRepository;
    @Mock
    private LectureRepository lectureRepository;
    @Mock
    private LectureProgressRepository lectureProgressRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private CourseInstructorRepository courseInstructorRepository;
    @Mock
    private CourseAuditService courseAuditService;
    @Mock
    private StudentProfileRepository studentProfileRepository;

    private CourseCompletionService service;

    private User instructor;
    private User coInstructor;
    private User otherInstructor;
    private User student;
    private User admin;
    private Course course1;
    private Course course2;
    private Certificate cert1;
    private Certificate cert2;

    @BeforeEach
    void setUp() {
        service = new CourseCompletionService(
                courseRepository,
                userRepository,
                completionCriteriaRepository,
                certificateRepository,
                enrollmentRepository,
                lessonRepository,
                lessonProgressRepository,
                lectureRepository,
                lectureProgressRepository,
                quizRepository,
                quizAttemptRepository,
                courseInstructorRepository,
                courseAuditService
        );
        service.setStudentProfileRepository(studentProfileRepository);

        instructor = new User("Prof. Smith", "smith@olp.com", "hash", "instructor", "active");
        instructor.setId(10);

        coInstructor = new User("Dr. Taylor", "taylor@olp.com", "hash", "instructor", "active");
        coInstructor.setId(20);

        otherInstructor = new User("Dr. Stranger", "stranger@olp.com", "hash", "instructor", "active");
        otherInstructor.setId(99);

        student = new User("John Doe", "john@olp.com", "hash", "student", "active");
        student.setId(50);

        admin = new User("Admin", "admin@olp.com", "hash", "admin", "active");
        admin.setId(1);

        course1 = new Course();
        course1.setId(100);
        course1.setTitle("Full Stack Java Mastery");
        course1.setInstructor(instructor);
        course1.setStatus(CourseStatus.PUBLISHED);

        course2 = new Course();
        course2.setId(200);
        course2.setTitle("Spring Boot Cloud Native");
        course2.setInstructor(instructor);
        course2.setStatus(CourseStatus.PUBLISHED);

        cert1 = new Certificate(course1, student, "OLP-100-50-ABCDEF12");
        cert1.setId(1);
        cert1.setIssuedAt(LocalDateTime.of(2026, 3, 15, 10, 0));

        cert2 = new Certificate(course2, student, "OLP-200-50-FEDCBA21");
        cert2.setId(2);
        cert2.setIssuedAt(LocalDateTime.of(2026, 4, 20, 14, 30));

        when(courseRepository.findById(100)).thenReturn(Optional.of(course1));
        when(courseRepository.findById(200)).thenReturn(Optional.of(course2));
        when(courseRepository.findByInstructorIdOrderByIdAsc(10)).thenReturn(List.of(course1, course2));
    }

    @Nested
    @DisplayName("1. Access Control & Association Tests (DR-05)")
    class AccessControlTests {

        @Test
        @DisplayName("Course owner can view issued certificates for their courses")
        void testCourseOwner_CanViewCertificates() {
            Page<Certificate> mockPage = new PageImpl<>(List.of(cert1, cert2));
            when(certificateRepository.findIssuedCertificates(anySet(), any(), any(), any(Pageable.class)))
                    .thenReturn(mockPage);

            PageResponse<CertificateResponse> response = service.getIssuedCertificates(
                    instructor, null, null, null, 0, 10);

            assertNotNull(response);
            assertEquals(2, response.getContent().size());
            assertEquals("Full Stack Java Mastery", response.getContent().get(0).getCourseName());
            assertEquals("John Doe", response.getContent().get(0).getLearnerName());
            assertEquals("john@olp.com", response.getContent().get(0).getLearnerEmail());
            assertEquals("/api/certificates/verify/OLP-100-50-ABCDEF12", response.getContent().get(0).getVerificationUrl());
        }

        @Test
        @DisplayName("Co-instructor can view issued certificates for course they are associated with")
        void testCoInstructor_CanViewCertificates() {
            CourseInstructor mapping = new CourseInstructor(course1, coInstructor, CourseInstructorRole.CO_INSTRUCTOR);
            when(courseInstructorRepository.findByInstructorId(20)).thenReturn(List.of(mapping));
            when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 20)).thenReturn(true);

            Page<Certificate> mockPage = new PageImpl<>(List.of(cert1));
            when(certificateRepository.findIssuedCertificates(eq(Set.of(100)), any(), any(), any(Pageable.class)))
                    .thenReturn(mockPage);

            PageResponse<CertificateResponse> response = service.getIssuedCertificates(
                    coInstructor, 100, null, null, 0, 10);

            assertNotNull(response);
            assertEquals(1, response.getContent().size());
            assertEquals(100, response.getContent().get(0).getCourseId());
        }

        @Test
        @DisplayName("Unassociated instructor cannot view certificates for another instructor's course (DR-05)")
        void testUnassociatedInstructor_ThrowsAccessDenied() {
            when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 99)).thenReturn(false);

            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                    () -> service.getIssuedCertificates(otherInstructor, 100, null, null, 0, 10));

            assertTrue(ex.getMessage().contains("DR-05") || ex.getMessage().contains("not associated"));
            verify(certificateRepository, never()).findIssuedCertificates(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Student user cannot access instructor certificate viewing API")
        void testStudentRole_ThrowsAccessDenied() {
            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                    () -> service.getIssuedCertificates(student, 100, null, null, 0, 10));

            assertTrue(ex.getMessage().contains("only instructors or admins"));
        }

        @Test
        @DisplayName("Pending/inactive instructor cannot access certificate viewing API")
        void testPendingInstructor_ThrowsAccessDenied() {
            User pending = new User("Pending", "pending@olp.com", "hash", "instructor", "pending");
            pending.setId(30);

            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                    () -> service.getIssuedCertificates(pending, null, null, null, 0, 10));

            assertTrue(ex.getMessage().contains("awaiting admin approval"));
        }

        @Test
        @DisplayName("Admin can view certificates for any course without association restrictions")
        void testAdmin_CanViewCertificates() {
            Page<Certificate> mockPage = new PageImpl<>(List.of(cert1));
            when(certificateRepository.findIssuedCertificates(eq(Set.of(100)), any(), any(), any(Pageable.class)))
                    .thenReturn(mockPage);

            PageResponse<CertificateResponse> response = service.getIssuedCertificates(
                    admin, 100, null, null, 0, 10);

            assertNotNull(response);
            assertEquals(1, response.getContent().size());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when course does not exist")
        void testCourseNotFound_ThrowsResourceNotFound() {
            when(courseRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.getIssuedCertificates(instructor, 999, null, null, 0, 10));
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when course is soft-deleted")
        void testSoftDeletedCourse_ThrowsResourceNotFound() {
            Course deleted = new Course();
            deleted.setId(300);
            deleted.setDeleted(true);
            when(courseRepository.findById(300)).thenReturn(Optional.of(deleted));

            assertThrows(ResourceNotFoundException.class,
                    () -> service.getIssuedCertificates(instructor, 300, null, null, 0, 10));
        }
    }

    @Nested
    @DisplayName("2. Filtering & Date Range Tests")
    class FilteringTests {

        @Test
        @DisplayName("Filter certificates by specific courseId")
        void testFilterByCourseId() {
            Page<Certificate> mockPage = new PageImpl<>(List.of(cert1));
            when(certificateRepository.findIssuedCertificates(eq(Set.of(100)), any(), any(), any(Pageable.class)))
                    .thenReturn(mockPage);

            PageResponse<CertificateResponse> response = service.getIssuedCertificates(
                    instructor, 100, null, null, 0, 10);

            assertNotNull(response);
            assertEquals(1, response.getContent().size());
            assertEquals(100, response.getContent().get(0).getCourseId());
        }

        @Test
        @DisplayName("Filter certificates by issue-date range (startDate and endDate)")
        void testFilterByDateRange_StartAndEnd() {
            Page<Certificate> mockPage = new PageImpl<>(List.of(cert1));
            when(certificateRepository.findIssuedCertificates(anySet(), notNull(), notNull(), any(Pageable.class)))
                    .thenReturn(mockPage);

            PageResponse<CertificateResponse> response = service.getIssuedCertificates(
                    instructor, null, "2026-03-01", "2026-03-31", 0, 10);

            assertNotNull(response);
            assertEquals(1, response.getContent().size());
            verify(certificateRepository).findIssuedCertificates(
                    anySet(),
                    eq(LocalDateTime.of(2026, 3, 1, 0, 0, 0)),
                    eq(LocalDateTime.of(2026, 3, 31, 23, 59, 59, 999999999)),
                    any(Pageable.class));
        }

        @Test
        @DisplayName("Filter certificates by startDate only")
        void testFilterByDateRange_StartDateOnly() {
            Page<Certificate> mockPage = new PageImpl<>(List.of(cert1, cert2));
            when(certificateRepository.findIssuedCertificates(anySet(), notNull(), isNull(), any(Pageable.class)))
                    .thenReturn(mockPage);

            PageResponse<CertificateResponse> response = service.getIssuedCertificates(
                    instructor, null, "2026-03-01", null, 0, 10);

            assertNotNull(response);
            verify(certificateRepository).findIssuedCertificates(
                    anySet(),
                    eq(LocalDateTime.of(2026, 3, 1, 0, 0, 0)),
                    isNull(),
                    any(Pageable.class));
        }

        @Test
        @DisplayName("Filter certificates by endDate only")
        void testFilterByDateRange_EndDateOnly() {
            Page<Certificate> mockPage = new PageImpl<>(List.of(cert1));
            when(certificateRepository.findIssuedCertificates(anySet(), isNull(), notNull(), any(Pageable.class)))
                    .thenReturn(mockPage);

            PageResponse<CertificateResponse> response = service.getIssuedCertificates(
                    instructor, null, null, "2026-03-31", 0, 10);

            assertNotNull(response);
            verify(certificateRepository).findIssuedCertificates(
                    anySet(),
                    isNull(),
                    eq(LocalDateTime.of(2026, 3, 31, 23, 59, 59, 999999999)),
                    any(Pageable.class));
        }

        @Test
        @DisplayName("StartDate after EndDate throws IllegalArgumentException")
        void testInvalidDateRange_ThrowsIllegalArgument() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.getIssuedCertificates(instructor, null, "2026-05-01", "2026-03-01", 0, 10));

            assertTrue(ex.getMessage().contains("must be before or equal to"));
        }

        @Test
        @DisplayName("Instructor with no associated courses returns empty page cleanly")
        void testInstructorWithNoCourses_ReturnsEmpty() {
            User newInstructor = new User("Fresh", "fresh@olp.com", "hash", "instructor", "active");
            newInstructor.setId(77);
            when(courseRepository.findByInstructorIdOrderByIdAsc(77)).thenReturn(Collections.emptyList());
            when(courseInstructorRepository.findByInstructorId(77)).thenReturn(Collections.emptyList());

            PageResponse<CertificateResponse> response = service.getIssuedCertificates(
                    newInstructor, null, null, null, 0, 10);

            assertNotNull(response);
            assertTrue(response.getContent().isEmpty());
            assertEquals(0, response.getTotalElements());
            verify(certificateRepository, never()).findIssuedCertificates(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Unpaginated list query returns all matching certificates")
        void testGetIssuedCertificatesList_Success() {
            when(certificateRepository.findIssuedCertificatesList(anySet(), any(), any()))
                    .thenReturn(List.of(cert1, cert2));

            List<CertificateResponse> list = service.getIssuedCertificatesList(
                    instructor, null, null, null);

            assertEquals(2, list.size());
            assertEquals(1, list.get(0).getId());
            assertEquals(1, list.get(0).getCertificateId());
            assertEquals(2, list.get(1).getId());
        }
    }

    @Nested
    @DisplayName("3. Public Certificate Verification Tests (DR-02)")
    class PublicVerificationTests {

        @Test
        @DisplayName("Verifying valid certificate code returns CertificateResponse with verified status")
        void testVerifyCertificate_ValidCode_Success() {
            when(certificateRepository.findByCertificateCode("OLP-100-50-ABCDEF12"))
                    .thenReturn(Optional.of(cert1));

            CertificateResponse response = service.verifyCertificate("OLP-100-50-ABCDEF12");

            assertNotNull(response);
            assertTrue(response.isValid());
            assertEquals("VERIFIED", response.getStatus());
            assertEquals("OLP-100-50-ABCDEF12", response.getCertificateCode());
            assertEquals(100, response.getCourseId());
            assertEquals("Full Stack Java Mastery", response.getCourseName());
            assertEquals("John Doe", response.getLearnerName());
            assertEquals("/api/certificates/verify/OLP-100-50-ABCDEF12", response.getVerificationUrl());
        }

        @Test
        @DisplayName("Verifying non-existent certificate code throws ResourceNotFoundException")
        void testVerifyCertificate_InvalidCode_ThrowsNotFound() {
            when(certificateRepository.findByCertificateCode("NON-EXISTENT"))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.verifyCertificate("NON-EXISTENT"));
        }

        @Test
        @DisplayName("Verifying null or empty certificate code throws IllegalArgumentException")
        void testVerifyCertificate_NullCode_ThrowsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.verifyCertificate(""));
            assertThrows(IllegalArgumentException.class,
                    () -> service.verifyCertificate(null));
        }
    }

    @Nested
    @DisplayName("4. Date Parsing Utility Tests")
    class DateParsingTests {

        @Test
        @DisplayName("Parses standard yyyy-MM-dd date correctly for start and end of day")
        void testParseLocalDate() {
            LocalDateTime start = service.parseDateTime("2026-01-15", false);
            LocalDateTime end = service.parseDateTime("2026-01-15", true);

            assertEquals(LocalDateTime.of(2026, 1, 15, 0, 0, 0), start);
            assertEquals(LocalDateTime.of(2026, 1, 15, 23, 59, 59, 999999999), end);
        }

        @Test
        @DisplayName("Parses ISO LocalDateTime correctly")
        void testParseLocalDateTime() {
            LocalDateTime dt = service.parseDateTime("2026-01-15T14:30:00", false);
            assertEquals(LocalDateTime.of(2026, 1, 15, 14, 30, 0), dt);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException on invalid date string")
        void testParseInvalidDate_ThrowsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.parseDateTime("invalid-date-format", false));
        }
    }
}
