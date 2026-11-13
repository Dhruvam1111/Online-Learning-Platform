package com.olp.service;

import com.olp.dto.CourseReviewStatusResponse;
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

/**
 * Comprehensive unit tests for FR-I71 – Review Status and Resubmit.
 * Validates review status retrieval across PENDING, APPROVED, REJECTED, and CHANGES_REQUESTED states,
 * admin review comment exposure, strict resubmission eligibility enforcement,
 * pre-resubmission validation, and immutable history preservation.
 */
@ExtendWith(MockitoExtension.class)
class CourseReviewStatusServiceTest {

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

    private Course sampleCourse;
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

        sampleCourse = new Course(
                ownerInstructor,
                "Microservices with Spring Boot 3",
                "End-to-end cloud-native microservices architecture guide.",
                "Engineering",
                CourseStatus.DRAFT
        );
        sampleCourse.setId(100);
        sampleCourse.setPrice(new BigDecimal("799.00"));

        sampleModule = new CourseModule(sampleCourse, "Module 1: Architecture", "System Design", 1);
        sampleModule.setId(10);

        sampleSection = new CourseSection(sampleModule, "Section 1.1: Services", "Design", 1);
        sampleSection.setId(20);

        sampleLesson = new Lesson(sampleSection, "Lesson 1: Intro", LessonType.VIDEO, "Content", "https://video.mp4", 15, 1, null);
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

    // =========================================================================
    // 1. REVIEW STATUS RETRIEVAL (FR-I71)
    // =========================================================================

    @Test
    @DisplayName("Review status: PENDING_REVIEW course maps to PENDING, cannot resubmit, cannot edit")
    void testGetReviewStatus_PendingReview() {
        sampleCourse.setStatus(CourseStatus.PENDING_REVIEW);
        when(courseRepository.findById(100)).thenReturn(Optional.of(sampleCourse));

        CourseSubmission sub = new CourseSubmission(sampleCourse, ownerInstructor, "PENDING_REVIEW", "Initial submit", "{}");
        sub.setId(1);
        when(courseSubmissionRepository.findByCourseIdOrderBySubmittedAtDesc(100)).thenReturn(List.of(sub));

        CourseReviewStatusResponse response = submissionService.getReviewStatus(ownerInstructor, 100);

        assertNotNull(response);
        assertEquals(100, response.getCourseId());
        assertEquals("PENDING", response.getStatus());
        assertEquals("PENDING", response.getReviewStatus());
        assertFalse(response.isCanResubmit(), "Pending courses cannot be resubmitted");
        assertFalse(response.isCanEdit(), "Pending courses cannot be edited");
        assertEquals(1, response.getSubmissionCount());
        assertEquals(1, response.getSubmissionHistory().size());
    }

    @Test
    @DisplayName("Review status: PUBLISHED course maps to APPROVED, cannot resubmit, cannot edit")
    void testGetReviewStatus_ApprovedCourse() {
        sampleCourse.setStatus(CourseStatus.PUBLISHED);
        when(courseRepository.findById(100)).thenReturn(Optional.of(sampleCourse));

        CourseSubmission sub = new CourseSubmission(sampleCourse, ownerInstructor, "APPROVED", "Approved by admin", "{}");
        sub.setId(2);
        sub.setReviewedAt(LocalDateTime.now());
        when(courseSubmissionRepository.findByCourseIdOrderBySubmittedAtDesc(100)).thenReturn(List.of(sub));

        CourseReviewStatusResponse response = submissionService.getReviewStatus(ownerInstructor, 100);

        assertNotNull(response);
        assertEquals("APPROVED", response.getStatus());
        assertEquals("APPROVED", response.getReviewStatus());
        assertFalse(response.isCanResubmit(), "Approved courses cannot be resubmitted");
        assertFalse(response.isCanEdit(), "Published courses cannot be edited directly");
        assertNotNull(response.getReviewedAt());
    }

    @Test
    @DisplayName("Review status: REJECTED course maps to REJECTED with rejection reason and admin comments")
    void testGetReviewStatus_RejectedCourse() {
        sampleCourse.setStatus(CourseStatus.REJECTED);
        sampleCourse.setRejectionReason("Course does not meet audio quality standards.");
        sampleCourse.setReviewComments("Audio is distorted in module 1.");
        when(courseRepository.findById(100)).thenReturn(Optional.of(sampleCourse));

        CourseSubmission sub = new CourseSubmission(sampleCourse, ownerInstructor, "REJECTED", "Sub 1", "{}");
        sub.setId(3);
        sub.setReviewComments("Audio is distorted in module 1.");
        sub.setReviewedAt(LocalDateTime.now().minusDays(1));
        when(courseSubmissionRepository.findByCourseIdOrderBySubmittedAtDesc(100)).thenReturn(List.of(sub));

        CourseReviewStatusResponse response = submissionService.getReviewStatus(ownerInstructor, 100);

        assertNotNull(response);
        assertEquals("REJECTED", response.getStatus());
        assertEquals("REJECTED", response.getReviewStatus());
        assertEquals("Course does not meet audio quality standards.", response.getRejectionReason());
        assertEquals("Audio is distorted in module 1.", response.getAdminComments());
        assertFalse(response.isCanResubmit(), "Rejected courses cannot be resubmitted (only CHANGES_REQUESTED)");
        assertFalse(response.isCanEdit());
    }

    @Test
    @DisplayName("Review status: CHANGES_REQUESTED course allows resubmission and editing, exposes comments")
    void testGetReviewStatus_ChangesRequestedCourse() {
        sampleCourse.setStatus(CourseStatus.CHANGES_REQUESTED);
        sampleCourse.setReviewComments("Please add practical exercises in section 1.1 and revise the discount rate.");
        sampleCourse.setRejectionReason("Need practical exercises and discount update.");
        when(courseRepository.findById(100)).thenReturn(Optional.of(sampleCourse));

        CourseSubmission sub = new CourseSubmission(sampleCourse, ownerInstructor, "CHANGES_REQUESTED", "Please review", "{}");
        sub.setId(4);
        sub.setReviewComments("Please add practical exercises in section 1.1 and revise the discount rate.");
        sub.setReviewedAt(LocalDateTime.now().minusHours(2));
        when(courseSubmissionRepository.findByCourseIdOrderBySubmittedAtDesc(100)).thenReturn(List.of(sub));

        CourseReviewStatusResponse response = submissionService.getReviewStatus(ownerInstructor, 100);

        assertNotNull(response);
        assertEquals("CHANGES_REQUESTED", response.getStatus());
        assertEquals("CHANGES_REQUESTED", response.getReviewStatus());
        assertEquals("Please add practical exercises in section 1.1 and revise the discount rate.", response.getAdminComments());
        assertEquals("Please add practical exercises in section 1.1 and revise the discount rate.", response.getChangeRequestReason());
        assertTrue(response.isCanResubmit(), "Courses in CHANGES_REQUESTED must allow resubmission");
        assertTrue(response.isCanEdit(), "Courses in CHANGES_REQUESTED must allow editing");
    }

    @Test
    @DisplayName("Review status: DRAFT course maps to DRAFT, allows editing, disallows resubmission")
    void testGetReviewStatus_DraftCourse() {
        sampleCourse.setStatus(CourseStatus.DRAFT);
        when(courseRepository.findById(100)).thenReturn(Optional.of(sampleCourse));
        when(courseSubmissionRepository.findByCourseIdOrderBySubmittedAtDesc(100)).thenReturn(Collections.emptyList());

        CourseReviewStatusResponse response = submissionService.getReviewStatus(ownerInstructor, 100);

        assertNotNull(response);
        assertEquals("DRAFT", response.getStatus());
        assertEquals("DRAFT", response.getReviewStatus());
        assertFalse(response.isCanResubmit());
        assertTrue(response.isCanEdit());
        assertEquals(0, response.getSubmissionCount());
        assertTrue(response.getSubmissionHistory().isEmpty());
    }

    @Test
    @DisplayName("Review status: includes chronological history with reviewer notes across multiple submissions")
    void testGetReviewStatus_MultiSubmissionHistory() {
        sampleCourse.setStatus(CourseStatus.CHANGES_REQUESTED);
        sampleCourse.setReviewComments("Follow-up changes required");
        when(courseRepository.findById(100)).thenReturn(Optional.of(sampleCourse));

        CourseSubmission sub1 = new CourseSubmission(sampleCourse, ownerInstructor, "CHANGES_REQUESTED", "Version 1", "{}");
        sub1.setId(10);
        sub1.setReviewComments("First review notes");
        sub1.setSubmittedAt(LocalDateTime.now().minusDays(5));
        sub1.setReviewedAt(LocalDateTime.now().minusDays(4));

        CourseSubmission sub2 = new CourseSubmission(sampleCourse, ownerInstructor, "CHANGES_REQUESTED", "Version 2", "{}");
        sub2.setId(11);
        sub2.setReviewComments("Follow-up changes required");
        sub2.setSubmittedAt(LocalDateTime.now().minusDays(2));
        sub2.setReviewedAt(LocalDateTime.now().minusDays(1));

        // findByCourseIdOrderBySubmittedAtDesc returns newest first: [sub2, sub1]
        when(courseSubmissionRepository.findByCourseIdOrderBySubmittedAtDesc(100)).thenReturn(List.of(sub2, sub1));

        CourseReviewStatusResponse response = submissionService.getReviewStatus(ownerInstructor, 100);

        assertNotNull(response);
        assertEquals(2, response.getSubmissionCount());
        assertEquals(2, response.getSubmissionHistory().size());

        CourseSubmissionResponse latest = response.getSubmissionHistory().get(0);
        assertEquals(2L, latest.getSubmissionNumber());
        assertEquals("Follow-up changes required", latest.getReviewComments());

        CourseSubmissionResponse oldest = response.getSubmissionHistory().get(1);
        assertEquals(1L, oldest.getSubmissionNumber());
        assertEquals("First review notes", oldest.getReviewComments());
    }

    @Test
    @DisplayName("Review status: Admin user can inspect review status for any course")
    void testGetReviewStatus_AdminAccess_Allowed() {
        sampleCourse.setStatus(CourseStatus.CHANGES_REQUESTED);
        when(courseRepository.findById(100)).thenReturn(Optional.of(sampleCourse));
        when(courseSubmissionRepository.findByCourseIdOrderBySubmittedAtDesc(100)).thenReturn(Collections.emptyList());

        CourseReviewStatusResponse response = submissionService.getReviewStatus(adminUser, 100);

        assertNotNull(response);
        assertEquals(100, response.getCourseId());
    }

    @Test
    @DisplayName("Review status: Unauthorized instructor cannot access course review status")
    void testGetReviewStatus_UnauthorizedInstructor_ThrowsAccessDenied() {
        when(courseRepository.findById(100)).thenReturn(Optional.of(sampleCourse));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, otherInstructor.getId())).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                submissionService.getReviewStatus(otherInstructor, 100));
    }

    @Test
    @DisplayName("Review status: Non-existent course throws ResourceNotFoundException")
    void testGetReviewStatus_CourseNotFound_ThrowsException() {
        when(courseRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                submissionService.getReviewStatus(ownerInstructor, 999));
    }

    // =========================================================================
    // 2. RESUBMISSION WORKFLOW (FR-I71)
    // =========================================================================

    @Test
    @DisplayName("Resubmit: Successfully resubmits course in CHANGES_REQUESTED, transitions to PENDING_REVIEW, clears comments")
    void testResubmitCourse_Success() {
        sampleCourse.setStatus(CourseStatus.CHANGES_REQUESTED);
        sampleCourse.setReviewComments("Please update syllabus.");
        sampleCourse.setRejectionReason("Syllabus needs work.");

        when(courseRepository.findById(100)).thenReturn(Optional.of(sampleCourse));
        stubFullCourseStructure();

        when(courseSubmissionRepository.save(any(CourseSubmission.class))).thenAnswer(inv -> {
            CourseSubmission s = inv.getArgument(0);
            s.setId(25);
            s.setSubmittedAt(LocalDateTime.now());
            return s;
        });
        when(courseSubmissionRepository.countByCourseId(100)).thenReturn(2L);

        CourseSubmissionRequest request = new CourseSubmissionRequest("Updated syllabus with detailed week-by-week plan.");
        CourseSubmissionResponse response = submissionService.resubmitCourse(ownerInstructor, 100, request);

        assertNotNull(response);
        assertEquals(2L, response.getSubmissionNumber());
        assertEquals(CourseStatus.PENDING_REVIEW.name(), response.getStatus());
        assertEquals("Updated syllabus with detailed week-by-week plan.", response.getComments());

        // Verify entity state
        assertEquals(CourseStatus.PENDING_REVIEW, sampleCourse.getStatus());
        assertNull(sampleCourse.getReviewComments(), "Review comments should be reset upon resubmission");
        assertNull(sampleCourse.getRejectionReason(), "Rejection reason should be cleared upon resubmission");

        verify(courseRepository).save(sampleCourse);
        verify(courseSubmissionRepository).save(any(CourseSubmission.class));
    }

    @Test
    @DisplayName("Resubmit: Fails when course status is DRAFT (only CHANGES_REQUESTED is permitted)")
    void testResubmitCourse_DraftStatus_ThrowsIllegalStateException() {
        sampleCourse.setStatus(CourseStatus.DRAFT);
        when(courseRepository.findById(100)).thenReturn(Optional.of(sampleCourse));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                submissionService.resubmitCourse(ownerInstructor, 100, new CourseSubmissionRequest("Resubmitting draft")));

        assertTrue(ex.getMessage().contains("CHANGES_REQUESTED"), "Error should explain that course can only be resubmitted when status is CHANGES_REQUESTED");
        verify(courseSubmissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Resubmit: Fails when course status is already PENDING_REVIEW")
    void testResubmitCourse_PendingReview_ThrowsIllegalStateException() {
        sampleCourse.setStatus(CourseStatus.PENDING_REVIEW);
        when(courseRepository.findById(100)).thenReturn(Optional.of(sampleCourse));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                submissionService.resubmitCourse(ownerInstructor, 100, new CourseSubmissionRequest("Resubmit")));

        assertTrue(ex.getMessage().contains("CHANGES_REQUESTED"));
    }

    @Test
    @DisplayName("Resubmit: Fails when course status is PUBLISHED")
    void testResubmitCourse_Published_ThrowsIllegalStateException() {
        sampleCourse.setStatus(CourseStatus.PUBLISHED);
        when(courseRepository.findById(100)).thenReturn(Optional.of(sampleCourse));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                submissionService.resubmitCourse(ownerInstructor, 100, new CourseSubmissionRequest("Resubmit")));

        assertTrue(ex.getMessage().contains("CHANGES_REQUESTED"));
    }

    @Test
    @DisplayName("Resubmit: Fails when course status is REJECTED (only CHANGES_REQUESTED allowed)")
    void testResubmitCourse_Rejected_ThrowsIllegalStateException() {
        sampleCourse.setStatus(CourseStatus.REJECTED);
        when(courseRepository.findById(100)).thenReturn(Optional.of(sampleCourse));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                submissionService.resubmitCourse(ownerInstructor, 100, new CourseSubmissionRequest("Resubmit")));

        assertTrue(ex.getMessage().contains("CHANGES_REQUESTED"));
    }

    @Test
    @DisplayName("Resubmit: Validation fails if title was deleted or blanked out")
    void testResubmitCourse_BlankTitle_ThrowsIllegalArgumentException() {
        sampleCourse.setStatus(CourseStatus.CHANGES_REQUESTED);
        sampleCourse.setTitle("   ");
        when(courseRepository.findById(100)).thenReturn(Optional.of(sampleCourse));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                submissionService.resubmitCourse(ownerInstructor, 100, new CourseSubmissionRequest()));

        assertTrue(ex.getMessage().contains("title"));
    }

    @Test
    @DisplayName("Resubmit: Validation fails if course has no modules")
    void testResubmitCourse_NoModules_ThrowsIllegalStateException() {
        sampleCourse.setStatus(CourseStatus.CHANGES_REQUESTED);
        when(courseRepository.findById(100)).thenReturn(Optional.of(sampleCourse));
        when(courseModuleRepository.findByCourseIdOrderByOrderIndexAsc(100)).thenReturn(Collections.emptyList());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                submissionService.resubmitCourse(ownerInstructor, 100, new CourseSubmissionRequest()));

        assertTrue(ex.getMessage().contains("module"));
    }

    @Test
    @DisplayName("Resubmit: Co-instructor authorized to resubmit course")
    void testResubmitCourse_CoInstructor_Success() {
        sampleCourse.setStatus(CourseStatus.CHANGES_REQUESTED);
        when(courseRepository.findById(100)).thenReturn(Optional.of(sampleCourse));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, coInstructor.getId())).thenReturn(true);
        stubFullCourseStructure();

        when(courseSubmissionRepository.save(any(CourseSubmission.class))).thenAnswer(inv -> inv.getArgument(0));
        when(courseSubmissionRepository.countByCourseId(100)).thenReturn(3L);

        CourseSubmissionResponse response = submissionService.resubmitCourse(coInstructor, 100, new CourseSubmissionRequest("Co-instructor update"));

        assertNotNull(response);
        assertEquals(CourseStatus.PENDING_REVIEW.name(), response.getStatus());
    }

    @Test
    @DisplayName("Resubmit: Unauthorized instructor cannot resubmit course")
    void testResubmitCourse_UnauthorizedInstructor_ThrowsAccessDenied() {
        sampleCourse.setStatus(CourseStatus.CHANGES_REQUESTED);
        when(courseRepository.findById(100)).thenReturn(Optional.of(sampleCourse));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, otherInstructor.getId())).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                submissionService.resubmitCourse(otherInstructor, 100, new CourseSubmissionRequest()));
    }
}

