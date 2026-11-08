package com.olp.service;

import com.olp.dto.CourseResponse;
import com.olp.dto.CreateCourseRequest;
import com.olp.dto.UpdateCourseRequest;
import com.olp.dto.UpdateCourseStatusRequest;
import com.olp.entity.Course;
import com.olp.entity.CourseStatus;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseRepository;
import com.olp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstructorCourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InstructorCourseService service;

    private User instructor(Integer id) {
        User user = new User("Dr. Chen", "chen" + id + "@olp.com", "hash", "instructor");
        user.setId(id);
        return user;
    }

    private User student(Integer id) {
        User user = new User("Sam Student", "sam" + id + "@olp.com", "hash", "student");
        user.setId(id);
        return user;
    }

    private Course course(Integer id, User owner) {
        Course course = new Course(owner, "Original Title", "Original Description", CourseStatus.DRAFT);
        course.setId(id);
        return course;
    }

    private Course givenOwnedCourse(Integer courseId, User owner) {
        Course course = course(courseId, owner);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        return course;
    }

    private void stubSaveReturnsArgument() {
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ===== resolveUser =====

    @Test
    void testResolveUser_NullAuthentication_Throws() {
        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(null));
    }

    @Test
    void testResolveUser_NullPrincipal_Throws() {
        Authentication auth = mock(Authentication.class);

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(auth));

        assertTrue(ex.getMessage().contains("not authenticated"));
    }

    @Test
    void testResolveUser_UserPrincipal_ReturnedWithoutRepositoryLookup() {
        User user = instructor(1);
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null);

        assertSame(user, service.resolveUser(auth));
        verifyNoInteractions(userRepository);
    }

    @Test
    void testResolveUser_EmailPrincipal_LooksUpByEmail() {
        User user = instructor(1);
        Authentication auth = new UsernamePasswordAuthenticationToken("chen1@olp.com", null);
        when(userRepository.findByEmail("chen1@olp.com")).thenReturn(Optional.of(user));

        assertSame(user, service.resolveUser(auth));
    }

    @Test
    void testResolveUser_EmailPrincipalNotFound_Throws() {
        Authentication auth = new UsernamePasswordAuthenticationToken("ghost@olp.com", null);
        when(userRepository.findByEmail("ghost@olp.com")).thenReturn(Optional.empty());

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(auth));

        assertTrue(ex.getMessage().contains("User not found"));
    }

    // ===== getInstructorCourses =====

    @Test
    void testGetInstructorCourses_NullUser_Throws() {
        assertThrows(InvalidCredentialsException.class, () -> service.getInstructorCourses(null));
        verifyNoInteractions(courseRepository);
    }

    @Test
    void testGetInstructorCourses_NonInstructor_ThrowsAccessDenied() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> service.getInstructorCourses(student(2)));

        assertTrue(ex.getMessage().contains("only instructors"));
        verifyNoInteractions(courseRepository);
    }

    @Test
    void testGetInstructorCourses_ReturnsOwnCoursesMapped() {
        User owner = instructor(1);
        when(courseRepository.findByInstructorIdOrderByIdAsc(1))
                .thenReturn(List.of(course(10, owner), course(11, owner)));

        List<CourseResponse> result = service.getInstructorCourses(owner);

        assertEquals(2, result.size());
        assertEquals(10, result.get(0).getId());
        assertEquals(11, result.get(1).getId());
        assertEquals(1, result.get(0).getInstructorId());
        assertEquals("Original Title", result.get(0).getTitle());
    }

    @Test
    void testGetInstructorCourses_NoCourses_ReturnsEmptyList() {
        when(courseRepository.findByInstructorIdOrderByIdAsc(1)).thenReturn(List.of());

        assertTrue(service.getInstructorCourses(instructor(1)).isEmpty());
    }

    @Test
    void testGetInstructorCourses_RoleCheckIsCaseInsensitive() {
        User owner = instructor(1);
        owner.setRole("INSTRUCTOR");
        when(courseRepository.findByInstructorIdOrderByIdAsc(1)).thenReturn(List.of());

        assertDoesNotThrow(() -> service.getInstructorCourses(owner));
    }

    // ===== createCourse =====

    @Test
    void testCreateCourse_NullUser_Throws() {
        assertThrows(InvalidCredentialsException.class,
                () -> service.createCourse(null, new CreateCourseRequest("T", "D")));
    }

    @Test
    void testCreateCourse_NonInstructor_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class,
                () -> service.createCourse(student(2), new CreateCourseRequest("T", "D")));
    }

    @Test
    void testCreateCourse_BlankTitle_ThrowsAndDoesNotSave() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createCourse(instructor(1), new CreateCourseRequest("   ", "D")));

        assertTrue(ex.getMessage().contains("Course title cannot be blank"));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void testCreateCourse_NullTitle_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createCourse(instructor(1), new CreateCourseRequest(null, "D")));
    }

    @Test
    void testCreateCourse_Success_TrimsFieldsAndStartsAsDraft() {
        User owner = instructor(1);
        stubSaveReturnsArgument();

        CourseResponse response = service.createCourse(owner, new CreateCourseRequest("  Spring Boot  ", "  Learn it  "));

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository, times(1)).save(captor.capture());
        Course saved = captor.getValue();
        assertEquals("Spring Boot", saved.getTitle());
        assertEquals("Learn it", saved.getDescription());
        assertEquals(CourseStatus.DRAFT, saved.getStatus());
        assertSame(owner, saved.getInstructor());
        assertEquals("Spring Boot", response.getTitle());
        assertEquals(CourseStatus.DRAFT, response.getStatus());
        assertEquals(1, response.getInstructorId());
    }

    @Test
    void testCreateCourse_NullDescription_StoredAsNull() {
        stubSaveReturnsArgument();

        service.createCourse(instructor(1), new CreateCourseRequest("Title", null));

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertNull(captor.getValue().getDescription());
    }

    // ===== updateCourse =====

    @Test
    void testUpdateCourse_NullUser_Throws() {
        assertThrows(InvalidCredentialsException.class,
                () -> service.updateCourse(null, 10, new UpdateCourseRequest("T", "D")));
    }

    @Test
    void testUpdateCourse_NonInstructor_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class,
                () -> service.updateCourse(student(2), 10, new UpdateCourseRequest("T", "D")));
    }

    @Test
    void testUpdateCourse_BodyInstructorIdMismatch_ThrowsAccessDenied() {
        UpdateCourseRequest request = new UpdateCourseRequest("T", "D");
        request.setInstructorId(99);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> service.updateCourse(instructor(1), 10, request));

        assertTrue(ex.getMessage().contains("another instructor"));
        verifyNoInteractions(courseRepository);
    }

    @Test
    void testUpdateCourse_BodyCourseIdMismatch_ThrowsIllegalArgument() {
        UpdateCourseRequest request = new UpdateCourseRequest("T", "D");
        request.setId(77);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateCourse(instructor(1), 10, request));

        assertTrue(ex.getMessage().contains("do not match"));
        verifyNoInteractions(courseRepository);
    }

    @Test
    void testUpdateCourse_NullCourseId_Throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateCourse(instructor(1), null, new UpdateCourseRequest("T", "D")));

        assertTrue(ex.getMessage().contains("Course ID cannot be null"));
    }

    @Test
    void testUpdateCourse_CourseNotFound_Throws() {
        when(courseRepository.findById(99)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.updateCourse(instructor(1), 99, new UpdateCourseRequest("T", "D")));

        assertTrue(ex.getMessage().contains("Course not found with id: 99"));
    }

    @Test
    void testUpdateCourse_CourseOwnedByAnotherInstructor_ThrowsAccessDenied() {
        givenOwnedCourse(10, instructor(2));

        assertThrows(AccessDeniedException.class,
                () -> service.updateCourse(instructor(1), 10, new UpdateCourseRequest("T", "D")));

        verify(courseRepository, never()).save(any());
    }

    @Test
    void testUpdateCourse_CourseWithoutInstructor_ThrowsAccessDenied() {
        Course orphan = course(10, null);
        when(courseRepository.findById(10)).thenReturn(Optional.of(orphan));

        assertThrows(AccessDeniedException.class,
                () -> service.updateCourse(instructor(1), 10, new UpdateCourseRequest("T", "D")));
    }

    @Test
    void testUpdateCourse_BlankTitle_ThrowsAndDoesNotSave() {
        givenOwnedCourse(10, instructor(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateCourse(instructor(1), 10, new UpdateCourseRequest("  ", "D")));

        assertTrue(ex.getMessage().contains("Title cannot be blank"));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void testUpdateCourse_NullTitle_Throws() {
        givenOwnedCourse(10, instructor(1));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateCourse(instructor(1), 10, new UpdateCourseRequest(null, "D")));
    }

    @Test
    void testUpdateCourse_Success_TrimsTitleAndSetsDescriptionVerbatim() {
        Course existing = givenOwnedCourse(10, instructor(1));
        stubSaveReturnsArgument();

        CourseResponse response = service.updateCourse(instructor(1), 10, new UpdateCourseRequest("  New Title  ", "  New Desc  "));

        assertEquals("New Title", existing.getTitle());
        assertEquals("  New Desc  ", existing.getDescription());
        assertEquals("New Title", response.getTitle());
        verify(courseRepository, times(1)).save(existing);
    }

    @Test
    void testUpdateCourse_NullDescription_KeepsExistingDescription() {
        Course existing = givenOwnedCourse(10, instructor(1));
        stubSaveReturnsArgument();

        service.updateCourse(instructor(1), 10, new UpdateCourseRequest("New Title", null));

        assertEquals("Original Description", existing.getDescription());
    }

    @Test
    void testUpdateCourse_MatchingBodyIds_Allowed() {
        givenOwnedCourse(10, instructor(1));
        stubSaveReturnsArgument();
        UpdateCourseRequest request = new UpdateCourseRequest("New Title", "D");
        request.setId(10);
        request.setInstructorId(1);

        CourseResponse response = service.updateCourse(instructor(1), 10, request);

        assertEquals("New Title", response.getTitle());
    }

    // ===== updateCourseStatus =====

    @Test
    void testUpdateCourseStatus_NullUser_Throws() {
        assertThrows(InvalidCredentialsException.class,
                () -> service.updateCourseStatus(null, 10, new UpdateCourseStatusRequest("PUBLISHED")));
    }

    @Test
    void testUpdateCourseStatus_NonInstructor_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class,
                () -> service.updateCourseStatus(student(2), 10, new UpdateCourseStatusRequest("PUBLISHED")));
    }

    @Test
    void testUpdateCourseStatus_CourseNotFound_Throws() {
        when(courseRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateCourseStatus(instructor(1), 99, new UpdateCourseStatusRequest("PUBLISHED")));
    }

    @Test
    void testUpdateCourseStatus_NotOwner_ThrowsAccessDenied() {
        givenOwnedCourse(10, instructor(2));

        assertThrows(AccessDeniedException.class,
                () -> service.updateCourseStatus(instructor(1), 10, new UpdateCourseStatusRequest("PUBLISHED")));
    }

    @Test
    void testUpdateCourseStatus_NullRequest_Throws() {
        givenOwnedCourse(10, instructor(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateCourseStatus(instructor(1), 10, null));

        assertTrue(ex.getMessage().contains("Course status cannot be blank"));
    }

    @Test
    void testUpdateCourseStatus_NullStatus_Throws() {
        givenOwnedCourse(10, instructor(1));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateCourseStatus(instructor(1), 10, new UpdateCourseStatusRequest(null)));
    }

    @Test
    void testUpdateCourseStatus_BlankStatus_Throws() {
        givenOwnedCourse(10, instructor(1));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateCourseStatus(instructor(1), 10, new UpdateCourseStatusRequest("   ")));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void testUpdateCourseStatus_UnknownStatus_ThrowsAndDoesNotSave() {
        givenOwnedCourse(10, instructor(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateCourseStatus(instructor(1), 10, new UpdateCourseStatusRequest("archived")));

        assertTrue(ex.getMessage().contains("Invalid course status"));
        verify(courseRepository, never()).save(any());
    }

    private void assertStatusChange(String input, CourseStatus expected) {
        Course existing = givenOwnedCourse(10, instructor(1));
        stubSaveReturnsArgument();

        CourseResponse response = service.updateCourseStatus(instructor(1), 10, new UpdateCourseStatusRequest(input));

        assertEquals(expected, existing.getStatus());
        assertEquals(expected, response.getStatus());
        verify(courseRepository, times(1)).save(existing);
    }

    @Test
    void testUpdateCourseStatus_LowercasePublished_IsNormalised() {
        assertStatusChange("published", CourseStatus.PUBLISHED);
    }

    @Test
    void testUpdateCourseStatus_HyphenatedPendingReview_IsNormalised() {
        assertStatusChange("pending-review", CourseStatus.PENDING_REVIEW);
    }

    @Test
    void testUpdateCourseStatus_SurroundingWhitespace_IsTrimmed() {
        assertStatusChange("  DRAFT  ", CourseStatus.DRAFT);
    }

    @Test
    void testUpdateCourseStatus_InstructorCanSetRejected_NoTransitionRules() {
        assertStatusChange("REJECTED", CourseStatus.REJECTED);
    }

    @Test
    void testUpdateCourseStatus_PublishedBackToDraft_Allowed() {
        Course existing = givenOwnedCourse(10, instructor(1));
        existing.setStatus(CourseStatus.PUBLISHED);
        stubSaveReturnsArgument();

        CourseResponse response = service.updateCourseStatus(instructor(1), 10, new UpdateCourseStatusRequest("draft"));

        assertEquals(CourseStatus.DRAFT, response.getStatus());
    }
}
