package com.olp.service;

import com.olp.dto.AvailableCourseAssociationResponse;
import com.olp.dto.CourseAssociationRequestDto;
import com.olp.dto.CourseAssociationRequestResponse;
import com.olp.dto.CourseAssociationReviewRequest;
import com.olp.dto.CourseInstructorResponse;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseAssociationRequestRepository;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.InstructorProfileRepository;
import com.olp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseAssociationServiceTest {

    @Mock
    private CourseAssociationRequestRepository requestRepository;
    @Mock
    private CourseInstructorRepository courseInstructorRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private InstructorProfileRepository instructorProfileRepository;

    @InjectMocks
    private CourseAssociationService service;

    private User instructor(Integer id, String name) {
        User u = new User(name, "inst" + id + "@olp.com", "hash", "instructor", "active");
        u.setId(id);
        return u;
    }

    private User pendingInstructor(Integer id) {
        User u = new User("Pending Inst", "pending" + id + "@olp.com", "hash", "instructor", "pending");
        u.setId(id);
        return u;
    }

    private User student(Integer id) {
        User u = new User("Sam Student", "sam" + id + "@olp.com", "hash", "student", "active");
        u.setId(id);
        return u;
    }

    private User admin(Integer id) {
        User u = new User("Platform Admin", "admin" + id + "@olp.com", "hash", "admin", "active");
        u.setId(id);
        return u;
    }

    private Course course(Integer id, User owner, String title, String category, CourseStatus status) {
        Course c = new Course(owner, title, "Description for " + title, category, status);
        c.setId(id);
        return c;
    }

    private CourseAssociationRequest pendingRequest(Integer reqId, Course course, User requester) {
        CourseAssociationRequest req = new CourseAssociationRequest(course, requester, "co-instructor", "I can help teach labs");
        req.setId(reqId);
        return req;
    }

    // =========================================================================
    // 1. SEARCH & VIEW AVAILABLE COURSES
    // =========================================================================

    @Test
    void testSearchAvailableCourses_ReturnsCoursesWithAssociationStatuses() {
        User owner = instructor(1, "Dr. Sarah");
        User requester = instructor(2, "Prof. James");

        Course ownedCourse = course(10, requester, "Python Intro", "Programming", CourseStatus.PUBLISHED);
        Course coCourse = course(11, owner, "Web Dev", "Web", CourseStatus.PUBLISHED);
        Course pendingCourse = course(12, owner, "Databases", "Databases", CourseStatus.PUBLISHED);
        Course openCourse = course(13, owner, "Cloud Native", "Cloud", CourseStatus.PUBLISHED);

        when(courseRepository.findByCourseStatusOrderByIdAsc(CourseStatus.PUBLISHED))
                .thenReturn(List.of(ownedCourse, coCourse, pendingCourse, openCourse));
        when(courseInstructorRepository.findByInstructorId(2))
                .thenReturn(List.of(new CourseInstructor(coCourse, requester, "co-instructor")));

        CourseAssociationRequest pending = pendingRequest(50, pendingCourse, requester);
        when(requestRepository.findByInstructorIdOrderByCreatedAtDesc(2)).thenReturn(List.of(pending));

        List<AvailableCourseAssociationResponse> results = service.searchAvailableCourses(requester, null, null);

        assertEquals(4, results.size());
        assertEquals("OWNER", results.get(0).getAssociationStatus());
        assertTrue(results.get(0).isAlreadyAssociated());

        assertEquals("ASSOCIATED", results.get(1).getAssociationStatus());
        assertTrue(results.get(1).isAlreadyAssociated());

        assertEquals("PENDING", results.get(2).getAssociationStatus());
        assertFalse(results.get(2).isAlreadyAssociated());
        assertEquals(50, results.get(2).getActiveRequestId());

        assertEquals("AVAILABLE", results.get(3).getAssociationStatus());
        assertFalse(results.get(3).isAlreadyAssociated());
    }

    @Test
    void testSearchAvailableCourses_WithKeywordAndCategoryFilter() {
        User owner = instructor(1, "Dr. Sarah");
        User requester = instructor(2, "Prof. James");

        Course c1 = course(10, owner, "Spring Boot APIs", "Backend", CourseStatus.PUBLISHED);
        Course c2 = course(11, owner, "Spring Cloud", "Cloud", CourseStatus.PUBLISHED);

        when(courseRepository.searchByCourseStatusAndKeyword(CourseStatus.PUBLISHED, "Spring"))
                .thenReturn(List.of(c1, c2));
        when(courseInstructorRepository.findByInstructorId(2)).thenReturn(List.of());
        when(requestRepository.findByInstructorIdOrderByCreatedAtDesc(2)).thenReturn(List.of());

        List<AvailableCourseAssociationResponse> results =
                service.searchAvailableCourses(requester, "  Spring  ", "Backend");

        assertEquals(1, results.size());
        assertEquals(10, results.get(0).getCourseId());
    }

    // =========================================================================
    // 2. SUBMIT REQUEST, DUPLICATE PREVENTION & VALIDATION
    // =========================================================================

    @Test
    void testSubmitAssociationRequest_Success_StoredWithPendingStatus() {
        User owner = instructor(1, "Dr. Sarah");
        User requester = instructor(2, "Prof. James");
        Course target = course(10, owner, "Databases", "Databases", CourseStatus.PUBLISHED);

        when(courseRepository.findById(10)).thenReturn(Optional.of(target));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 2)).thenReturn(false);
        when(requestRepository.existsByCourseIdAndInstructorIdAndStatus(10, 2, AssociationRequestStatus.PENDING))
                .thenReturn(false);
        when(requestRepository.save(any(CourseAssociationRequest.class))).thenAnswer(inv -> {
            CourseAssociationRequest r = inv.getArgument(0);
            r.setId(100);
            return r;
        });

        CourseAssociationRequestDto dto = new CourseAssociationRequestDto(10, "Happy to co-teach SQL module");
        CourseAssociationRequestResponse response = service.submitAssociationRequest(requester, null, dto);

        assertEquals(100, response.getId());
        assertEquals(10, response.getCourseId());
        assertEquals(2, response.getInstructorId());
        assertEquals("co-instructor", response.getRequestedRole());
        assertEquals("Happy to co-teach SQL module", response.getMessage());
        assertEquals(AssociationRequestStatus.PENDING, response.getStatus());
    }

    @Test
    void testSubmitAssociationRequest_CourseCreatorAutoAssociated_ThrowsConflict() {
        User owner = instructor(1, "Dr. Sarah");
        Course ownCourse = course(10, owner, "My Course", "General", CourseStatus.PUBLISHED);

        when(courseRepository.findById(10)).thenReturn(Optional.of(ownCourse));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.submitAssociationRequest(owner, 10, new CourseAssociationRequestDto()));

        assertTrue(ex.getMessage().contains("already the creator and owner"));
        verify(requestRepository, never()).save(any());
    }

    @Test
    void testSubmitAssociationRequest_AlreadyAssociatedCoInstructor_ThrowsConflict() {
        User owner = instructor(1, "Dr. Sarah");
        User coInstructor = instructor(2, "Prof. James");
        Course target = course(10, owner, "Databases", "Databases", CourseStatus.PUBLISHED);

        when(courseRepository.findById(10)).thenReturn(Optional.of(target));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 2)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.submitAssociationRequest(coInstructor, 10, null));

        assertTrue(ex.getMessage().contains("already associated"));
        verify(requestRepository, never()).save(any());
    }

    @Test
    void testSubmitAssociationRequest_DuplicatePendingRequest_ThrowsConflict() {
        User owner = instructor(1, "Dr. Sarah");
        User requester = instructor(2, "Prof. James");
        Course target = course(10, owner, "Databases", "Databases", CourseStatus.PUBLISHED);

        when(courseRepository.findById(10)).thenReturn(Optional.of(target));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 2)).thenReturn(false);
        when(requestRepository.existsByCourseIdAndInstructorIdAndStatus(10, 2, AssociationRequestStatus.PENDING))
                .thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.submitAssociationRequest(requester, 10, null));

        assertTrue(ex.getMessage().contains("pending association request already exists"));
        verify(requestRepository, never()).save(any());
    }

    @Test
    void testSubmitAssociationRequest_ArchivedCourse_ThrowsConflict() {
        User owner = instructor(1, "Dr. Sarah");
        User requester = instructor(2, "Prof. James");
        Course archived = course(10, owner, "Legacy Course", "General", CourseStatus.ARCHIVED);

        when(courseRepository.findById(10)).thenReturn(Optional.of(archived));

        assertThrows(IllegalStateException.class, () -> service.submitAssociationRequest(requester, 10, null));
        verify(requestRepository, never()).save(any());
    }

    @Test
    void testSubmitAssociationRequest_OwnerRoleEscalation_Rejected() {
        User requester = instructor(2, "Prof. James");
        CourseAssociationRequestDto dto = new CourseAssociationRequestDto(10, "owner", "Let me own it");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.submitAssociationRequest(requester, 10, dto));

        assertTrue(ex.getMessage().contains("co-instructor"));
        verifyNoInteractions(courseRepository);
    }

    @Test
    void testSubmitAssociationRequest_PathBodyMismatch_Rejected() {
        User requester = instructor(2, "Prof. James");
        CourseAssociationRequestDto dto = new CourseAssociationRequestDto(99, "Message");

        assertThrows(IllegalArgumentException.class, () -> service.submitAssociationRequest(requester, 10, dto));
    }

    @Test
    void testSubmitAssociationRequest_CourseNotFound_ThrowsResourceNotFound() {
        User requester = instructor(2, "Prof. James");
        when(courseRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.submitAssociationRequest(requester, 999, null));
    }

    // =========================================================================
    // 3. INSTRUCTOR REQUEST MANAGEMENT & ISOLATION
    // =========================================================================

    @Test
    void testGetMyRequests_WithStatusFilter_ReturnsOnlyOwnRequests() {
        User owner = instructor(1, "Dr. Sarah");
        User requester = instructor(2, "Prof. James");
        Course c = course(10, owner, "Databases", "Databases", CourseStatus.PUBLISHED);
        CourseAssociationRequest req = pendingRequest(50, c, requester);

        when(requestRepository.findByInstructorIdAndStatusOrderByCreatedAtDesc(2, AssociationRequestStatus.PENDING))
                .thenReturn(List.of(req));

        List<CourseAssociationRequestResponse> list = service.getMyRequests(requester, "pending");

        assertEquals(1, list.size());
        assertEquals(50, list.get(0).getId());
        assertEquals(AssociationRequestStatus.PENDING, list.get(0).getStatus());
    }

    @Test
    void testGetAndCancelMyRequest_OtherInstructorDenied() {
        User owner = instructor(1, "Dr. Sarah");
        User requester = instructor(2, "Prof. James");
        User intruder = instructor(3, "Intruder");
        Course c = course(10, owner, "Databases", "Databases", CourseStatus.PUBLISHED);
        CourseAssociationRequest req = pendingRequest(50, c, requester);

        when(requestRepository.findByIdWithDetails(50)).thenReturn(Optional.of(req));

        assertEquals(50, service.getMyRequestById(requester, 50).getId());
        assertThrows(AccessDeniedException.class, () -> service.getMyRequestById(intruder, 50));
        assertThrows(AccessDeniedException.class, () -> service.cancelMyRequest(intruder, 50));
        verify(requestRepository, never()).delete(any());
    }

    @Test
    void testCancelMyRequest_PendingDeleted_ProcessedRejected() {
        User owner = instructor(1, "Dr. Sarah");
        User requester = instructor(2, "Prof. James");
        Course c = course(10, owner, "Databases", "Databases", CourseStatus.PUBLISHED);
        CourseAssociationRequest req = pendingRequest(50, c, requester);

        when(requestRepository.findByIdWithDetails(50)).thenReturn(Optional.of(req));

        service.cancelMyRequest(requester, 50);
        verify(requestRepository).delete(req);

        req.setStatus(AssociationRequestStatus.APPROVED);
        assertThrows(IllegalStateException.class, () -> service.cancelMyRequest(requester, 50));
    }

    // =========================================================================
    // 4. ADMIN APPROVAL, REJECTION & NOTIFICATIONS
    // =========================================================================

    @Test
    void testApproveRequest_CreatesCourseInstructorAndSendsNotification() {
        User owner = instructor(1, "Dr. Sarah");
        User requester = instructor(2, "Prof. James");
        User platformAdmin = admin(99);
        Course c = course(10, owner, "Databases", "Databases", CourseStatus.PUBLISHED);
        CourseAssociationRequest req = pendingRequest(50, c, requester);

        when(requestRepository.findByIdWithDetails(50)).thenReturn(Optional.of(req));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 2)).thenReturn(false);
        when(requestRepository.save(any(CourseAssociationRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseAssociationRequestResponse response = service.approveRequest(
                platformAdmin, 50, new CourseAssociationReviewRequest("Approved for co-teaching"));

        assertEquals(AssociationRequestStatus.APPROVED, response.getStatus());
        assertEquals(99, response.getReviewedById());
        assertEquals("Approved for co-teaching", response.getAdminNotes());
        assertNotNull(response.getReviewedAt());

        ArgumentCaptor<CourseInstructor> ciCaptor = ArgumentCaptor.forClass(CourseInstructor.class);
        verify(courseInstructorRepository).save(ciCaptor.capture());
        assertEquals(10, ciCaptor.getValue().getCourse().getId());
        assertEquals(2, ciCaptor.getValue().getInstructor().getId());
        assertEquals(CourseInstructorRole.CO_INSTRUCTOR, ciCaptor.getValue().getRole());
        assertTrue(ciCaptor.getValue().isCoInstructor());

        verify(emailService).sendCourseAssociationDecisionEmail(
                "inst2@olp.com", "Prof. James", "Databases",
                AssociationRequestStatus.APPROVED, "Approved for co-teaching");
    }

    @Test
    void testRejectRequest_DoesNotCreateAssociationAndSendsNotification() {
        User owner = instructor(1, "Dr. Sarah");
        User requester = instructor(2, "Prof. James");
        User platformAdmin = admin(99);
        Course c = course(10, owner, "Databases", "Databases", CourseStatus.PUBLISHED);
        CourseAssociationRequest req = pendingRequest(50, c, requester);

        when(requestRepository.findByIdWithDetails(50)).thenReturn(Optional.of(req));
        when(requestRepository.save(any(CourseAssociationRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseAssociationReviewRequest review = new CourseAssociationReviewRequest();
        review.setReason("Course already has maximum co-instructors");

        CourseAssociationRequestResponse response = service.rejectRequest(platformAdmin, 50, review);

        assertEquals(AssociationRequestStatus.REJECTED, response.getStatus());
        assertEquals("Course already has maximum co-instructors", response.getAdminNotes());
        verify(courseInstructorRepository, never()).save(any());

        verify(emailService).sendCourseAssociationDecisionEmail(
                "inst2@olp.com", "Prof. James", "Databases",
                AssociationRequestStatus.REJECTED, "Course already has maximum co-instructors");
    }

    @Test
    void testApproveOrReject_NonPendingRequest_ThrowsConflict() {
        User owner = instructor(1, "Dr. Sarah");
        User requester = instructor(2, "Prof. James");
        User platformAdmin = admin(99);
        Course c = course(10, owner, "Databases", "Databases", CourseStatus.PUBLISHED);
        CourseAssociationRequest req = pendingRequest(50, c, requester);
        req.setStatus(AssociationRequestStatus.APPROVED);

        when(requestRepository.findByIdWithDetails(50)).thenReturn(Optional.of(req));

        assertThrows(IllegalStateException.class, () -> service.approveRequest(platformAdmin, 50, null));
        assertThrows(IllegalStateException.class, () -> service.rejectRequest(platformAdmin, 50, null));
        verify(emailService, never()).sendCourseAssociationDecisionEmail(any(), any(), any(), any(), any());
    }

    @Test
    void testAdminActions_NonAdminRejected() {
        User regularInstructor = instructor(2, "Prof. James");
        User stu = student(3);

        assertThrows(AccessDeniedException.class, () -> service.getRequestsForAdmin(regularInstructor, null));
        assertThrows(AccessDeniedException.class, () -> service.approveRequest(regularInstructor, 50, null));
        assertThrows(AccessDeniedException.class, () -> service.rejectRequest(stu, 50, null));
        verifyNoInteractions(requestRepository, courseInstructorRepository, emailService);
    }

    @Test
    void testInstructorActions_StudentPendingAndNullUserRejected() {
        assertThrows(InvalidCredentialsException.class, () -> service.searchAvailableCourses(null, null, null));
        assertThrows(AccessDeniedException.class, () -> service.submitAssociationRequest(student(3), 10, null));
        assertThrows(AccessDeniedException.class, () -> service.submitAssociationRequest(pendingInstructor(4), 10, null));
        assertThrows(AccessDeniedException.class, () -> service.getMyRequests(student(3), null));
    }

    // =========================================================================
    // 5. FR-I59: MULTIPLE CO-INSTRUCTORS & ADMIN RETRIEVAL
    // =========================================================================

    @Test
    void testSubmitAndApprove_UpperCaseCoInstructorRole_MultipleInstructorsAssociated() {
        User owner = instructor(1, "Dr. Sarah");
        User co1 = instructor(2, "Prof. James");
        User co2 = instructor(3, "Dr. Priya");
        User platformAdmin = admin(99);
        Course target = course(10, owner, "Databases", "Databases", CourseStatus.PUBLISHED);

        when(courseRepository.findById(10)).thenReturn(Optional.of(target));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 2)).thenReturn(false);
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 3)).thenReturn(false);
        when(requestRepository.existsByCourseIdAndInstructorIdAndStatus(10, 2, AssociationRequestStatus.PENDING))
                .thenReturn(false);
        when(requestRepository.existsByCourseIdAndInstructorIdAndStatus(10, 3, AssociationRequestStatus.PENDING))
                .thenReturn(false);
        when(requestRepository.save(any(CourseAssociationRequest.class))).thenAnswer(inv -> {
            CourseAssociationRequest r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(r.getInstructor().getId().equals(2) ? 501 : 502);
            }
            return r;
        });

        CourseAssociationRequestResponse req1 = service.submitAssociationRequest(
                co1, 10, new CourseAssociationRequestDto(null, "CO_INSTRUCTOR", "Can lead SQL"));
        CourseAssociationRequestResponse req2 = service.submitAssociationRequest(
                co2, 10, new CourseAssociationRequestDto(null, "CO_INSTRUCTOR", "Can grade labs"));

        CourseAssociationRequest entity1 = new CourseAssociationRequest(target, co1, req1.getRequestedRole(), req1.getMessage());
        entity1.setId(501);
        CourseAssociationRequest entity2 = new CourseAssociationRequest(target, co2, req2.getRequestedRole(), req2.getMessage());
        entity2.setId(502);

        when(requestRepository.findByIdWithDetails(501)).thenReturn(Optional.of(entity1));
        when(requestRepository.findByIdWithDetails(502)).thenReturn(Optional.of(entity2));

        service.approveRequest(platformAdmin, 501, null);
        service.approveRequest(platformAdmin, 502, null);

        verify(courseInstructorRepository, times(2)).save(any(CourseInstructor.class));
    }

    @Test
    void testGetCourseInstructorsForAdmin_SuccessAndNonAdminDenied() {
        User owner = instructor(1, "Dr. Sarah");
        User co1 = instructor(2, "Prof. James");
        Course target = course(10, owner, "Databases", "Databases", CourseStatus.DRAFT);

        when(courseRepository.findById(10)).thenReturn(Optional.of(target));
        when(courseInstructorRepository.findByCourseIdWithInstructor(10)).thenReturn(List.of(
                new CourseInstructor(target, owner, CourseInstructorRole.OWNER),
                new CourseInstructor(target, co1, CourseInstructorRole.CO_INSTRUCTOR)
        ));

        List<CourseInstructorResponse> list = service.getCourseInstructorsForAdmin(admin(99), 10);

        assertEquals(2, list.size());
        assertEquals(CourseInstructorRole.OWNER, list.get(0).getRole());
        assertEquals(CourseInstructorRole.CO_INSTRUCTOR, list.get(1).getRole());
        assertThrows(AccessDeniedException.class, () -> service.getCourseInstructorsForAdmin(owner, 10));
    }
}

