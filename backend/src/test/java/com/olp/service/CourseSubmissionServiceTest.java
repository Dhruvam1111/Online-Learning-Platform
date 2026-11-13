package com.olp.service;

import com.olp.dto.CourseSubmissionRequest;
import com.olp.dto.CourseSubmissionResponse;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseSubmissionServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseSubmissionRepository courseSubmissionRepository;
    @Mock
    private CourseModuleRepository courseModuleRepository;
    @Mock
    private CourseSectionRepository courseSectionRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LectureRepository lectureRepository;
    @Mock
    private CourseInstructorRepository courseInstructorRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CourseSubmissionService submissionService;

    private User ownerInstructor;
    private User coInstructor;
    private User otherInstructor;
    private User pendingInstructor;
    private User studentUser;
    private User adminUser;
    private Course validDraftCourse;
    private CourseModule sampleModule;
    private CourseSection sampleSection;
    private Lesson sampleLesson;

    @BeforeEach
    void setUp() {
        ownerInstructor = new User("Dr. Jane Smith", "jane@olp.com", "hash", "instructor", "active");
        ownerInstructor.setId(1);

        coInstructor = new User("Prof. Alan Co", "alan@olp.com", "hash", "instructor", "active");
        coInstructor.setId(2);

        otherInstructor = new User("Dr. Unrelated", "unrelated@olp.com", "hash", "instructor", "active");
        otherInstructor.setId(3);

        pendingInstructor = new User("Dr. Pending", "pending@olp.com", "hash", "instructor", "pending");
        pendingInstructor.setId(4);

        studentUser = new User("Sam Student", "student@olp.com", "hash", "student", "active");
        studentUser.setId(5);

        adminUser = new User("Admin Root", "admin@olp.com", "hash", "admin", "active");
        adminUser.setId(6);

        validDraftCourse = new Course(
                ownerInstructor,
                "Spring Boot Masterclass",
                "Comprehensive guide to modern Spring Boot 3 enterprise systems.",
                "Engineering",
                CourseStatus.DRAFT
        );
        validDraftCourse.setId(100);
        validDraftCourse.setPrice(new BigDecimal("499.00"));

        sampleModule = new CourseModule(validDraftCourse, "Module 1: Introduction", "Overview", 1);
        sampleModule.setId(10);

        sampleSection = new CourseSection(sampleModule, "Section 1.1: Setup", "Env setup", 1);
        sampleSection.setId(20);

        sampleLesson = new Lesson(sampleSection, "Lesson 1: Getting Started", LessonType.VIDEO, "Content", "https://video.mp4", 15, 1, null);
        sampleLesson.setId(30);
    }

    private void stubFullCourseStructure() {
        when(courseModuleRepository.findByCourseIdOrderByOrderIndexAsc(100))
                .thenReturn(List.of(sampleModule));
        when(courseSectionRepository.findByModuleIdOrderByOrderIndexAsc(10))
                .thenReturn(List.of(sampleSection));
        when(lessonRepository.findBySectionIdOrderByOrderIndexAsc(20))
                .thenReturn(List.of(sampleLesson));
    }

    @Test
    @DisplayName("Submit course: successful submission transitions status to PENDING_REVIEW and saves submission record with snapshot")
    void testSubmitCourse_Success() {
        when(courseRepository.findById(100)).thenReturn(Optional.of(validDraftCourse));
        stubFullCourseStructure();

        when(courseSubmissionRepository.save(any(CourseSubmission.class))).thenAnswer(inv -> {
            CourseSubmission s = inv.getArgument(0);
            s.setId(1);
            s.setSubmittedAt(LocalDateTime.now());
            return s;
        });
        when(courseSubmissionRepository.countByCourseId(100)).thenReturn(1L);

        CourseSubmissionRequest request = new CourseSubmissionRequest("Ready for review, all modules completed.");
        CourseSubmissionResponse response = submissionService.submitCourseForReview(ownerInstructor, 100, request);

        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals(100, response.getCourseId());
        assertEquals("Spring Boot Masterclass", response.getCourseTitle());
        assertEquals(1, response.getInstructorId());
        assertEquals("Dr. Jane Smith", response.getInstructorName());
        assertEquals(CourseStatus.PENDING_REVIEW.name(), response.getStatus());
        assertEquals("Ready for review, all modules completed.", response.getComments());
        assertEquals(1L, response.getSubmissionNumber());
        assertNotNull(response.getSnapshotData());
        assertTrue(response.getSnapshotData().contains("Spring Boot Masterclass"));
        assertTrue(response.getSnapshotData().contains("499.00"));

        // Verify status change and repository interaction
        assertEquals(CourseStatus.PENDING_REVIEW, validDraftCourse.getStatus());
        assertNull(validDraftCourse.getRejectionReason());
        verify(courseRepository).save(validDraftCourse);
        verify(courseSubmissionRepository).save(any(CourseSubmission.class));
    }

    @Test
    @DisplayName("Submit course: free course with price 0.00 is allowed and successfully submitted")
    void testSubmitCourse_FreeCourse_Success() {
        validDraftCourse.setPrice(BigDecimal.ZERO);
        when(courseRepository.findById(100)).thenReturn(Optional.of(validDraftCourse));
        stubFullCourseStructure();

        when(courseSubmissionRepository.save(any(CourseSubmission.class))).thenAnswer(inv -> {
            CourseSubmission s = inv.getArgument(0);
            s.setId(2);
            s.setSubmittedAt(LocalDateTime.now());
            return s;
        });
        when(courseSubmissionRepository.countByCourseId(100)).thenReturn(1L);

        CourseSubmissionResponse response = submissionService.submitCourseForReview(ownerInstructor, 100, null);

        assertNotNull(response);
        assertEquals(CourseStatus.PENDING_REVIEW.name(), response.getStatus());
        assertEquals(CourseStatus.PENDING_REVIEW, validDraftCourse.getStatus());
        verify(courseRepository).save(validDraftCourse);
    }

    @Test
    @DisplayName("Submit course: course with legacy lectures (no lesson sub-hierarchy) qualifies as valid content")
    void testSubmitCourse_WithLectures_Success() {
        when(courseRepository.findById(100)).thenReturn(Optional.of(validDraftCourse));
        when(courseModuleRepository.findByCourseIdOrderByOrderIndexAsc(100)).thenReturn(List.of(sampleModule));
        when(courseSectionRepository.findByModuleIdOrderByOrderIndexAsc(10)).thenReturn(Collections.emptyList());

        Lecture lecture = new Lecture(validDraftCourse, "Lecture 1: Intro", "https://video.mp4", "vid123", 1);
        lecture.setId(50);
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(100)).thenReturn(List.of(lecture));

        when(courseSubmissionRepository.save(any(CourseSubmission.class))).thenAnswer(inv -> {
            CourseSubmission s = inv.getArgument(0);
            s.setId(3);
            s.setSubmittedAt(LocalDateTime.now());
            return s;
        });
        when(courseSubmissionRepository.countByCourseId(100)).thenReturn(1L);

        CourseSubmissionResponse response = submissionService.submitCourseForReview(ownerInstructor, 100, null);

        assertNotNull(response);
        assertEquals(CourseStatus.PENDING_REVIEW.name(), response.getStatus());
        verify(courseRepository).save(validDraftCourse);
    }

    @Test
    @DisplayName("Submit course: co-instructor is authorized to submit course for review")
    void testSubmitCourse_CoInstructor_Success() {
        when(courseRepository.findById(100)).thenReturn(Optional.of(validDraftCourse));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 2)).thenReturn(true);
        stubFullCourseStructure();

        when(courseSubmissionRepository.save(any(CourseSubmission.class))).thenAnswer(inv -> {
            CourseSubmission s = inv.getArgument(0);
            s.setId(4);
            s.setSubmittedAt(LocalDateTime.now());
            return s;
        });
        when(courseSubmissionRepository.countByCourseId(100)).thenReturn(1L);

        CourseSubmissionResponse response = submissionService.submitCourseForReview(coInstructor, 100, null);

        assertNotNull(response);
        assertEquals(CourseStatus.PENDING_REVIEW.name(), response.getStatus());
        verify(courseRepository).save(validDraftCourse);
    }

    @Test
    @DisplayName("Submit course: admin user is authorized to submit any course for review")
    void testSubmitCourse_Admin_Success() {
        when(courseRepository.findById(100)).thenReturn(Optional.of(validDraftCourse));
        stubFullCourseStructure();

        when(courseSubmissionRepository.save(any(CourseSubmission.class))).thenAnswer(inv -> {
            CourseSubmission s = inv.getArgument(0);
            s.setId(5);
            s.setSubmittedAt(LocalDateTime.now());
            return s;
        });
        when(courseSubmissionRepository.countByCourseId(100)).thenReturn(1L);

        CourseSubmissionResponse response = submissionService.submitCourseForReview(adminUser, 100, null);

        assertNotNull(response);
        assertEquals(CourseStatus.PENDING_REVIEW.name(), response.getStatus());
        verify(courseRepository).save(validDraftCourse);
    }

    @Test
    @DisplayName("Submit course: resubmission of previously rejected course preserves history and clears rejection reason")
    void testResubmission_PreservesHistoryAndClearsRejectionReason() {
        validDraftCourse.setStatus(CourseStatus.REJECTED);
        validDraftCourse.setRejectionReason("Needs more clear prerequisites and lesson overview.");

        when(courseRepository.findById(100)).thenReturn(Optional.of(validDraftCourse));
        stubFullCourseStructure();

        when(courseSubmissionRepository.save(any(CourseSubmission.class))).thenAnswer(inv -> {
            CourseSubmission s = inv.getArgument(0);
            s.setId(6);
            s.setSubmittedAt(LocalDateTime.now());
            return s;
        });
        // Now there are 2 submissions in total
        when(courseSubmissionRepository.countByCourseId(100)).thenReturn(2L);

        CourseSubmissionRequest request = new CourseSubmissionRequest("Fixed requested prerequisite details.");
        CourseSubmissionResponse response = submissionService.submitCourseForReview(ownerInstructor, 100, request);

        assertNotNull(response);
        assertEquals(2L, response.getSubmissionNumber());
        assertEquals(CourseStatus.PENDING_REVIEW, validDraftCourse.getStatus());
        assertNull(validDraftCourse.getRejectionReason(), "Rejection reason should be cleared upon resubmission");
        verify(courseSubmissionRepository).save(any(CourseSubmission.class));
    }

    @Test
    @DisplayName("Submit course: missing or blank title throws IllegalArgumentException")
    void testSubmitCourse_MissingTitle_ThrowsException() {
        validDraftCourse.setTitle("   ");
        when(courseRepository.findById(100)).thenReturn(Optional.of(validDraftCourse));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> submissionService.submitCourseForReview(ownerInstructor, 100, null));
        assertTrue(ex.getMessage().contains("Course title must be provided"));
        assertEquals(CourseStatus.DRAFT, validDraftCourse.getStatus());
        verify(courseSubmissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Submit course: untitled draft throws IllegalArgumentException")
    void testSubmitCourse_UntitledDraft_ThrowsException() {
        validDraftCourse.setTitle("Untitled Draft");
        when(courseRepository.findById(100)).thenReturn(Optional.of(validDraftCourse));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> submissionService.submitCourseForReview(ownerInstructor, 100, null));
        assertTrue(ex.getMessage().contains("Course title must be provided"));
        verify(courseSubmissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Submit course: missing or blank description throws IllegalArgumentException")
    void testSubmitCourse_MissingDescription_ThrowsException() {
        validDraftCourse.setDescription(null);
        when(courseRepository.findById(100)).thenReturn(Optional.of(validDraftCourse));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> submissionService.submitCourseForReview(ownerInstructor, 100, null));
        assertTrue(ex.getMessage().contains("Course description must be provided"));
        verify(courseSubmissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Submit course: null price throws IllegalStateException")
    void testSubmitCourse_NullPrice_ThrowsException() {
        Course mockCourse = mock(Course.class);
        when(mockCourse.getId()).thenReturn(100);
        when(mockCourse.getTitle()).thenReturn("Valid Title");
        when(mockCourse.getDescription()).thenReturn("Valid Description");
        when(mockCourse.getPrice()).thenReturn(null);
        when(mockCourse.getInstructor()).thenReturn(ownerInstructor);
        when(courseRepository.findById(100)).thenReturn(Optional.of(mockCourse));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> submissionService.submitCourseForReview(ownerInstructor, 100, null));
        assertTrue(ex.getMessage().contains("Course price must be configured"));
        verify(courseSubmissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Submit course: negative price throws IllegalArgumentException")
    void testSubmitCourse_NegativePrice_ThrowsException() {
        validDraftCourse.setPrice(new BigDecimal("-10.00"));
        when(courseRepository.findById(100)).thenReturn(Optional.of(validDraftCourse));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> submissionService.submitCourseForReview(ownerInstructor, 100, null));
        assertTrue(ex.getMessage().contains("Course price cannot be negative"));
        verify(courseSubmissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Submit course: already PENDING_REVIEW throws IllegalStateException")
    void testSubmitCourse_AlreadyPendingReview_ThrowsException() {
        validDraftCourse.setStatus(CourseStatus.PENDING_REVIEW);
        when(courseRepository.findById(100)).thenReturn(Optional.of(validDraftCourse));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> submissionService.submitCourseForReview(ownerInstructor, 100, null));
        assertTrue(ex.getMessage().contains("already under review"));
        verify(courseSubmissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Submit course: already PUBLISHED throws IllegalStateException")
    void testSubmitCourse_AlreadyPublished_ThrowsException() {
        validDraftCourse.setStatus(CourseStatus.PUBLISHED);
        when(courseRepository.findById(100)).thenReturn(Optional.of(validDraftCourse));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> submissionService.submitCourseForReview(ownerInstructor, 100, null));
        assertTrue(ex.getMessage().contains("already published"));
        verify(courseSubmissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Submit course: ARCHIVED course throws IllegalStateException")
    void testSubmitCourse_Archived_ThrowsException() {
        validDraftCourse.setStatus(CourseStatus.ARCHIVED);
        when(courseRepository.findById(100)).thenReturn(Optional.of(validDraftCourse));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> submissionService.submitCourseForReview(ownerInstructor, 100, null));
        assertTrue(ex.getMessage().contains("archived"));
        verify(courseSubmissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Submit course: course with zero modules throws IllegalStateException")
    void testSubmitCourse_NoModules_ThrowsException() {
        when(courseRepository.findById(100)).thenReturn(Optional.of(validDraftCourse));
        when(courseModuleRepository.findByCourseIdOrderByOrderIndexAsc(100)).thenReturn(Collections.emptyList());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> submissionService.submitCourseForReview(ownerInstructor, 100, null));
        assertTrue(ex.getMessage().contains("at least one module"));
        verify(courseSubmissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Submit course: module exists but contains zero lessons/lectures throws IllegalStateException")
    void testSubmitCourse_ModuleWithoutContent_ThrowsException() {
        when(courseRepository.findById(100)).thenReturn(Optional.of(validDraftCourse));
        when(courseModuleRepository.findByCourseIdOrderByOrderIndexAsc(100)).thenReturn(List.of(sampleModule));
        when(courseSectionRepository.findByModuleIdOrderByOrderIndexAsc(10)).thenReturn(List.of(sampleSection));
        when(lessonRepository.findBySectionIdOrderByOrderIndexAsc(20)).thenReturn(Collections.emptyList());
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(100)).thenReturn(Collections.emptyList());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> submissionService.submitCourseForReview(ownerInstructor, 100, null));
        assertTrue(ex.getMessage().contains("actual lesson content"));
        verify(courseSubmissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Submit course: unauthorized instructor throws AccessDeniedException")
    void testSubmitCourse_UnauthorizedInstructor_ThrowsException() {
        when(courseRepository.findById(100)).thenReturn(Optional.of(validDraftCourse));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 3)).thenReturn(false);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> submissionService.submitCourseForReview(otherInstructor, 100, null));
        assertTrue(ex.getMessage().contains("Cannot submit another instructor's course"));
        verify(courseSubmissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Submit course: student user throws AccessDeniedException")
    void testSubmitCourse_StudentUser_ThrowsException() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> submissionService.submitCourseForReview(studentUser, 100, null));
        assertTrue(ex.getMessage().contains("Only instructors or admins"));
        verify(courseRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Submit course: unapproved/pending instructor throws AccessDeniedException")
    void testSubmitCourse_PendingInstructor_ThrowsException() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> submissionService.submitCourseForReview(pendingInstructor, 100, null));
        assertTrue(ex.getMessage().contains("awaiting admin approval"));
        verify(courseRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Submit course: unauthenticated user (null) throws InvalidCredentialsException")
    void testSubmitCourse_NullUser_ThrowsException() {
        assertThrows(InvalidCredentialsException.class,
                () -> submissionService.submitCourseForReview(null, 100, null));
    }

    @Test
    @DisplayName("Submit course: non-existent course ID throws ResourceNotFoundException")
    void testSubmitCourse_NotFound_ThrowsException() {
        when(courseRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> submissionService.submitCourseForReview(ownerInstructor, 999, null));
    }

    @Test
    @DisplayName("Get submission history: retrieves all historical submissions in descending order")
    void testGetSubmissionHistory_Success() {
        when(courseRepository.findById(100)).thenReturn(Optional.of(validDraftCourse));

        CourseSubmission sub1 = new CourseSubmission(validDraftCourse, ownerInstructor, "REJECTED", "First try", "{}");
        sub1.setId(1);
        sub1.setSubmittedAt(LocalDateTime.now().minusDays(5));

        CourseSubmission sub2 = new CourseSubmission(validDraftCourse, ownerInstructor, "PENDING_REVIEW", "Second try", "{}");
        sub2.setId(2);
        sub2.setSubmittedAt(LocalDateTime.now().minusDays(1));

        // Repository returns desc order
        when(courseSubmissionRepository.findByCourseIdOrderBySubmittedAtDesc(100))
                .thenReturn(List.of(sub2, sub1));

        List<CourseSubmissionResponse> history = submissionService.getSubmissionHistory(ownerInstructor, 100);

        assertNotNull(history);
        assertEquals(2, history.size());
        assertEquals(2, history.get(0).getId());
        assertEquals(2L, history.get(0).getSubmissionNumber());
        assertEquals(1, history.get(1).getId());
        assertEquals(1L, history.get(1).getSubmissionNumber());
    }

    @Test
    @DisplayName("Get submission history: unauthorized instructor throws AccessDeniedException")
    void testGetSubmissionHistory_Unauthorized_ThrowsException() {
        when(courseRepository.findById(100)).thenReturn(Optional.of(validDraftCourse));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 3)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> submissionService.getSubmissionHistory(otherInstructor, 100));
    }
}
