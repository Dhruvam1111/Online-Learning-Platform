package com.olp.service;

import com.olp.dto.CertificateResponse;
import com.olp.dto.CompletionEvaluationResult;
import com.olp.dto.CourseCompletionCriteriaRequest;
import com.olp.dto.CourseCompletionCriteriaResponse;
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
 * Comprehensive Unit and Integration Test Suite for FR-I56: Certificate
 * Completion Criteria.
 * Tests completion rule configuration, reusable evaluation engine
 * (evaluate(student, course)),
 * rule immutability lock after certificate issuance, and prerequisite checks
 * (DR-03).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseCompletionServiceTest {

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

    private CourseCompletionService service;

    private User owner;
    private User coInstructor;
    private User strangerInstructor;
    private User student;

    private Course course;
    private Lecture lecture1;
    private Lecture lecture2;
    private Quiz quiz1;

    @BeforeEach
    void setUp() {
        owner = new User("Owner Instructor", "owner@olp.com", "hash", "instructor", "active");
        owner.setId(1);

        coInstructor = new User("Co Instructor", "co@olp.com", "hash", "instructor", "active");
        coInstructor.setId(2);

        strangerInstructor = new User("Stranger Instructor", "stranger@olp.com", "hash", "instructor", "active");
        strangerInstructor.setId(3);

        student = new User("Alice Student", "student@olp.com", "hash", "student", "active");
        student.setId(10);

        course = new Course(owner, "Full Stack Web Development", "React and Spring Boot", CourseStatus.PUBLISHED);
        course.setId(100);

        lecture1 = new Lecture(course, "Introduction to React", "https://youtube.com/watch?v=111", 1);
        lecture1.setId(201);

        lecture2 = new Lecture(course, "Spring Boot Security", "https://youtube.com/watch?v=222", 2);
        lecture2.setId(202);

        quiz1 = new Quiz(course, null, null, owner, "Midterm Assessment", "Comprehensive quiz", "Answer all",
                QuizStatus.PUBLISHED);
        quiz1.setId(301);
        quiz1.setTotalMarks(BigDecimal.valueOf(100));
        quiz1.setPassingMarks(BigDecimal.valueOf(70));

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
                courseAuditService);

        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(userRepository.findById(10)).thenReturn(Optional.of(student));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 2)).thenReturn(true);
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 3)).thenReturn(false);
        when(enrollmentRepository.existsByStudentIdAndCourseId(10, 100)).thenReturn(true);
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(100)).thenReturn(List.of(lecture1, lecture2));
        when(quizRepository.findByCourseIdAndStatusOrderByCreatedAtDesc(100, QuizStatus.PUBLISHED))
                .thenReturn(List.of(quiz1));
        when(quizRepository.findByCourseIdOrderByCreatedAtDesc(100)).thenReturn(List.of(quiz1));
    }

    @Nested
    @DisplayName("1. Completion Rules Configuration Tests")
    class RuleConfigurationTests {

        @Test
        @DisplayName("Course owner can successfully configure completion criteria")
        void testConfigureCriteria_OwnerSuccess() {
            CourseCompletionCriteriaRequest request = new CourseCompletionCriteriaRequest();
            request.setMinLessonCompletionPercentage(BigDecimal.valueOf(80.0));
            request.setMinQuizPassingMarks(BigDecimal.valueOf(75.0));
            request.setRequireAllQuizzesPassed(true);
            request.setMandatoryLessonIds(List.of(201));
            request.setMandatoryQuizIds(List.of(301));

            when(completionCriteriaRepository.findByCourseId(100)).thenReturn(Optional.empty());
            when(completionCriteriaRepository.save(any(CourseCompletionCriteria.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CourseCompletionCriteriaResponse response = service.configureCompletionCriteria(owner, 100, request);

            assertNotNull(response);
            assertEquals(BigDecimal.valueOf(80.0), response.getMinLessonCompletionPercentage());
            assertEquals(BigDecimal.valueOf(75.0), response.getMinQuizPassingMarks());
            assertTrue(response.isRequireAllQuizzesPassed());
            assertEquals(List.of(201), response.getMandatoryLessonIds());
            assertEquals(List.of(301), response.getMandatoryQuizIds());
            assertFalse(response.isLocked());

            verify(completionCriteriaRepository).save(any(CourseCompletionCriteria.class));
            verify(courseAuditService).recordAudit(eq(course), eq(owner), eq("COURSE_COMPLETION_CRITERIA_UPDATE"),
                    anyString(), anyMap(), anyMap());
        }

        @Test
        @DisplayName("Associated co-instructor can also configure completion criteria")
        void testConfigureCriteria_CoInstructorSuccess() {
            CourseCompletionCriteriaRequest request = new CourseCompletionCriteriaRequest();
            request.setMinLessonCompletionPercentage(BigDecimal.valueOf(90.0));

            when(completionCriteriaRepository.findByCourseId(100)).thenReturn(Optional.empty());
            when(completionCriteriaRepository.save(any(CourseCompletionCriteria.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CourseCompletionCriteriaResponse response = service.configureCompletionCriteria(coInstructor, 100, request);
            assertNotNull(response);
            assertEquals(BigDecimal.valueOf(90.0), response.getMinLessonCompletionPercentage());
        }

        @Test
        @DisplayName("Stranger instructor is rejected with AccessDeniedException")
        void testConfigureCriteria_StrangerInstructorDenied() {
            CourseCompletionCriteriaRequest request = new CourseCompletionCriteriaRequest();
            request.setMinLessonCompletionPercentage(BigDecimal.valueOf(80.0));

            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                    () -> service.configureCompletionCriteria(strangerInstructor, 100, request));
            assertTrue(ex.getMessage().contains("not authorized"));
            verify(completionCriteriaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rule configuration is locked and throws IllegalStateException after first certificate is issued")
        void testConfigureCriteria_WhenLocked_ThrowsIllegalState() {
            when(certificateRepository.countByCourseId(100)).thenReturn(1L);

            CourseCompletionCriteriaRequest request = new CourseCompletionCriteriaRequest();
            request.setMinLessonCompletionPercentage(BigDecimal.valueOf(50.0));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.configureCompletionCriteria(owner, 100, request));
            assertTrue(ex.getMessage().contains("locked"));
            assertTrue(ex.getMessage().contains("DR-03"));
            verify(completionCriteriaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Criteria request with invalid percentage (<0 or >100) throws IllegalArgumentException")
        void testConfigureCriteria_InvalidPercentage_ThrowsIllegalArgument() {
            CourseCompletionCriteriaRequest req1 = new CourseCompletionCriteriaRequest();
            req1.setMinLessonCompletionPercentage(BigDecimal.valueOf(105.0));

            assertThrows(IllegalArgumentException.class,
                    () -> service.configureCompletionCriteria(owner, 100, req1));

            CourseCompletionCriteriaRequest req2 = new CourseCompletionCriteriaRequest();
            req2.setMinLessonCompletionPercentage(BigDecimal.valueOf(-5.0));

            assertThrows(IllegalArgumentException.class,
                    () -> service.configureCompletionCriteria(owner, 100, req2));
        }
    }

    @Nested
    @DisplayName("2. Reusable Completion Evaluation Tests")
    class CompletionEvaluationTests {

        @Test
        @DisplayName("Unenrolled student fails evaluation with descriptive unmet reason")
        void testEvaluate_UnenrolledStudent_FailsEvaluation() {
            when(enrollmentRepository.existsByStudentIdAndCourseId(10, 100)).thenReturn(false);

            CompletionEvaluationResult result = service.evaluate(student, course);

            assertNotNull(result);
            assertFalse(result.isEligible());
            assertTrue(result.getUnmetCriteriaReasons().stream().anyMatch(r -> r.contains("not enrolled")));
        }

        @Test
        @DisplayName("Incomplete lessons percentage triggers unmet reason")
        void testEvaluate_IncompleteLessons_FailsEvaluation() {
            CourseCompletionCriteria criteria = new CourseCompletionCriteria(course);
            criteria.setMinLessonCompletionPercentage(BigDecimal.valueOf(100.0));
            when(completionCriteriaRepository.findByCourseId(100)).thenReturn(Optional.of(criteria));

            // Student completed only 1 of 2 lectures (50%)
            when(lectureProgressRepository.countCompletedByStudentIdAndLectureIds(eq(10), anyCollection()))
                    .thenReturn(1L);

            CompletionEvaluationResult result = service.evaluate(student, course);

            assertNotNull(result);
            assertFalse(result.isEligible());
            assertFalse(result.isLessonCriteriaSatisfied());
            assertEquals(50.0, result.getLessonCompletionPercentage());
            assertTrue(result.getUnmetCriteriaReasons().stream().anyMatch(r -> r.contains("50.0%")));
        }

        @Test
        @DisplayName("Missing mandatory lesson triggers unmet reason")
        void testEvaluate_MissingMandatoryLesson_FailsEvaluation() {
            CourseCompletionCriteria criteria = new CourseCompletionCriteria(course);
            criteria.setMinLessonCompletionPercentage(BigDecimal.valueOf(50.0));
            criteria.setMandatoryLessonIdList(List.of(202)); // lecture2 is mandatory
            when(completionCriteriaRepository.findByCourseId(100)).thenReturn(Optional.of(criteria));

            // Student completed lecture1 (201) but not lecture2 (202)
            when(lectureProgressRepository.countCompletedByStudentIdAndLectureIds(eq(10), anyCollection()))
                    .thenReturn(1L);
            when(lectureProgressRepository.findCompletedLectureIdsByStudentId(eq(10), anyCollection()))
                    .thenReturn(List.of(201));

            CompletionEvaluationResult result = service.evaluate(student, course);

            assertNotNull(result);
            assertFalse(result.isEligible());
            assertFalse(result.isMandatoryLessonsSatisfied());
            assertTrue(result.getMissingMandatoryLessonIds().contains(202));
            assertTrue(result.getUnmetCriteriaReasons().stream().anyMatch(r -> r.contains("Spring Boot Security")));
        }

        @Test
        @DisplayName("Quiz score below required passing marks triggers unmet reason")
        void testEvaluate_FailingQuiz_FailsEvaluation() {
            CourseCompletionCriteria criteria = new CourseCompletionCriteria(course);
            criteria.setMinLessonCompletionPercentage(BigDecimal.valueOf(0.0));
            criteria.setMinQuizPassingMarks(BigDecimal.valueOf(80.0));
            when(completionCriteriaRepository.findByCourseId(100)).thenReturn(Optional.of(criteria));

            QuizAttempt attempt = new QuizAttempt(quiz1, student, 1, LocalDateTime.now(),
                    LocalDateTime.now().plusHours(1));
            attempt.setStatus(QuizAttemptStatus.SUBMITTED);
            attempt.setObtainedMarks(BigDecimal.valueOf(72.0)); // < 80.0

            when(quizAttemptRepository.findByQuizIdAndStudentIdAndStatusOrderByAttemptNumberDesc(eq(301), eq(10),
                    eq(QuizAttemptStatus.SUBMITTED)))
                    .thenReturn(List.of(attempt));

            CompletionEvaluationResult result = service.evaluate(student, course);

            assertNotNull(result);
            assertFalse(result.isEligible());
            assertTrue(result.getUnmetCriteriaReasons().stream().anyMatch(r -> r.contains("72.00")));
        }

        @Test
        @DisplayName("All criteria satisfied returns eligible=true with 0 unmet reasons")
        void testEvaluate_AllCriteriaSatisfied_ReturnsEligible() {
            CourseCompletionCriteria criteria = new CourseCompletionCriteria(course);
            criteria.setMinLessonCompletionPercentage(BigDecimal.valueOf(100.0));
            criteria.setMinQuizPassingMarks(BigDecimal.valueOf(70.0));
            criteria.setMandatoryLessonIdList(List.of(201));
            criteria.setMandatoryQuizIdList(List.of(301));
            when(completionCriteriaRepository.findByCourseId(100)).thenReturn(Optional.of(criteria));

            // Completed both lectures
            when(lectureProgressRepository.countCompletedByStudentIdAndLectureIds(eq(10), anyCollection()))
                    .thenReturn(2L);
            when(lectureProgressRepository.findCompletedLectureIdsByStudentId(eq(10), anyCollection()))
                    .thenReturn(List.of(201, 202));

            // Passed quiz with score 85.0 >= 70.0
            QuizAttempt attempt = new QuizAttempt(quiz1, student, 1, LocalDateTime.now(),
                    LocalDateTime.now().plusHours(1));
            attempt.setStatus(QuizAttemptStatus.SUBMITTED);
            attempt.setObtainedMarks(BigDecimal.valueOf(85.0));

            when(quizAttemptRepository.findByQuizIdAndStudentIdAndStatusOrderByAttemptNumberDesc(eq(301), eq(10),
                    eq(QuizAttemptStatus.SUBMITTED)))
                    .thenReturn(List.of(attempt));

            CompletionEvaluationResult result = service.evaluate(student, course);

            assertNotNull(result);
            assertTrue(result.isEligible(), "Student should be eligible for certificate");
            assertTrue(result.getUnmetCriteriaReasons().isEmpty(), "There should be no unmet criteria reasons");
            assertEquals(100.0, result.getLessonCompletionPercentage());
            assertTrue(result.isLessonCriteriaSatisfied());
            assertTrue(result.isQuizCriteriaSatisfied());
            assertTrue(result.isMandatoryLessonsSatisfied());
            assertTrue(result.isMandatoryQuizzesSatisfied());
        }
    }

    @Nested
    @DisplayName("3. Certificate Eligibility & Issuance Tests (DR-03)")
    class CertificateIssuanceTests {

        @Test
        @DisplayName("Issuing certificate fails with IllegalStateException when criteria are unmet")
        void testIssueCertificate_UnmetCriteria_ThrowsIllegalState() {
            // No lectures completed
            when(lectureProgressRepository.countCompletedByStudentIdAndLectureIds(eq(10), anyCollection()))
                    .thenReturn(0L);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.issueCertificate(student, course));
            assertTrue(ex.getMessage().contains("criteria are not satisfied"));
            verify(certificateRepository, never()).save(any());
        }

        @Test
        @DisplayName("Issuing certificate succeeds when all criteria are satisfied; locks criteria and records audit")
        void testIssueCertificate_CriteriaSatisfied_IssuesAndLocks() {
            CourseCompletionCriteria criteria = new CourseCompletionCriteria(course);
            criteria.setMinLessonCompletionPercentage(BigDecimal.valueOf(100.0));
            when(completionCriteriaRepository.findByCourseId(100)).thenReturn(Optional.of(criteria));

            when(lectureProgressRepository.countCompletedByStudentIdAndLectureIds(eq(10), anyCollection()))
                    .thenReturn(2L);
            when(lectureProgressRepository.findCompletedLectureIdsByStudentId(eq(10), anyCollection()))
                    .thenReturn(List.of(201, 202));

            QuizAttempt attempt = new QuizAttempt(quiz1, student, 1, LocalDateTime.now(),
                    LocalDateTime.now().plusHours(1));
            attempt.setStatus(QuizAttemptStatus.SUBMITTED);
            attempt.setObtainedMarks(BigDecimal.valueOf(90.0));
            when(quizAttemptRepository.findByQuizIdAndStudentIdAndStatusOrderByAttemptNumberDesc(eq(301), eq(10),
                    eq(QuizAttemptStatus.SUBMITTED)))
                    .thenReturn(List.of(attempt));

            when(certificateRepository.findByCourseId(100)).thenReturn(Collections.emptyList());
            when(certificateRepository.save(any(Certificate.class))).thenAnswer(inv -> inv.getArgument(0));

            CertificateResponse cert = service.issueCertificate(student, course);

            assertNotNull(cert);
            assertNotNull(cert.getCertificateCode());
            assertTrue(cert.getCertificateCode().startsWith("OLP-100-10-"));
            assertEquals(100, cert.getCourseId());
            assertEquals(10, cert.getStudentId());

            assertTrue(criteria.isLocked(), "Completion criteria must lock after first certificate issuance (DR-03)");
            verify(completionCriteriaRepository).save(criteria);
            verify(certificateRepository).save(any(Certificate.class));
            verify(courseAuditService).recordAudit(eq(course), eq(student), eq("CERTIFICATE_ISSUED"), anyString(),
                    anyMap(), anyMap());
        }
    }
}
