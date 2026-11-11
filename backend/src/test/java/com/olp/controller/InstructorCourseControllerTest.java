package com.olp.controller;

import com.olp.dto.CourseDraftRequest;
import com.olp.dto.CourseResponse;
import com.olp.dto.CreateCourseRequest;
import com.olp.dto.UpdateCourseRequest;
import com.olp.dto.UpdateCourseStatusRequest;
import com.olp.entity.CourseStatus;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.service.InstructorCourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstructorCourseControllerTest {

    @Mock
    private InstructorCourseService instructorCourseService;

    @InjectMocks
    private InstructorCourseController controller;

    private User currentUser;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        currentUser = new User("Dr. Chen", "chen@olp.com", "hash", "instructor");
        currentUser.setId(1);
        authentication = new UsernamePasswordAuthenticationToken(currentUser, null);
    }

    private void givenResolvedUser() {
        when(instructorCourseService.resolveUser(authentication)).thenReturn(currentUser);
    }

    private CourseResponse courseResponse(Integer id, String title) {
        return new CourseResponse(id, 1, title, "Description", CourseStatus.DRAFT, LocalDateTime.now());
    }

    @Test
    void testController_RequiresInstructorRole() {
        PreAuthorize preAuthorize = InstructorCourseController.class.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("hasRole('instructor')", preAuthorize.value());
    }

    @Test
    void testCreateCourse_Returns201WithCreatedCourse() {
        givenResolvedUser();
        CreateCourseRequest request = new CreateCourseRequest("Spring Boot", "Learn it");
        CourseResponse created = courseResponse(10, "Spring Boot");
        when(instructorCourseService.createCourse(currentUser, request)).thenReturn(created);

        ResponseEntity<CourseResponse> response = controller.createCourse(authentication, request);

        assertEquals(201, response.getStatusCode().value());
        assertSame(created, response.getBody());
        verify(instructorCourseService, times(1)).createCourse(currentUser, request);
    }

    @Test
    void testCreateCourse_FullPayloadWithPrerequisites_Returns201() {
        givenResolvedUser();
        CreateCourseRequest request = new CreateCourseRequest(
                "Full Stack Web", "Description", "Web Development",
                List.of(new com.olp.dto.CoursePrerequisiteRequest("SKILL", "Basic HTML")),
                List.of("Build sites")
        );
        CourseResponse created = new CourseResponse(
                10, 1, "Full Stack Web", "Description", CourseStatus.DRAFT, LocalDateTime.now(),
                "Web Development", List.of("Build sites"), List.of()
        );
        when(instructorCourseService.createCourse(currentUser, request)).thenReturn(created);

        ResponseEntity<CourseResponse> response = controller.createCourse(authentication, request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("Web Development", response.getBody().getCategory());
        assertEquals(CourseStatus.DRAFT, response.getBody().getStatus());
        verify(instructorCourseService, times(1)).createCourse(currentUser, request);
    }

    @Test
    void testCreateCourse_ServiceFailurePropagates() {
        givenResolvedUser();
        CreateCourseRequest request = new CreateCourseRequest("  ", null);
        when(instructorCourseService.createCourse(currentUser, request))
                .thenThrow(new IllegalArgumentException("Course title cannot be blank"));

        assertThrows(IllegalArgumentException.class, () -> controller.createCourse(authentication, request));
    }

    @Test
    void testGetCourses_NoQueryParams_Returns200WithList() {
        givenResolvedUser();
        List<CourseResponse> courses = List.of(courseResponse(10, "A"), courseResponse(11, "B"));
        when(instructorCourseService.getInstructorCourses(currentUser)).thenReturn(courses);

        ResponseEntity<List<CourseResponse>> response = controller.getCourses(authentication, null, null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(2, response.getBody().size());
        assertSame(courses, response.getBody());
    }

    @Test
    void testGetCourses_QueryParamsMatchingCurrentUser_Allowed() {
        givenResolvedUser();
        when(instructorCourseService.getInstructorCourses(currentUser)).thenReturn(List.of());

        ResponseEntity<List<CourseResponse>> response = controller.getCourses(authentication, 1, 1);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void testGetCourses_InstructorIdOfAnotherUser_ThrowsAccessDenied() {
        givenResolvedUser();

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> controller.getCourses(authentication, 99, null));

        assertTrue(ex.getMessage().contains("Cannot access another instructor's courses"));
        verify(instructorCourseService, never()).getInstructorCourses(any());
    }

    @Test
    void testGetCourses_UserIdOfAnotherUser_ThrowsAccessDenied() {
        givenResolvedUser();

        assertThrows(AccessDeniedException.class, () -> controller.getCourses(authentication, null, 99));
        verify(instructorCourseService, never()).getInstructorCourses(any());
    }

    @Test
    void testGetCourses_MatchingInstructorIdButOtherUserId_ThrowsAccessDenied() {
        givenResolvedUser();

        assertThrows(AccessDeniedException.class, () -> controller.getCourses(authentication, 1, 99));
        verify(instructorCourseService, never()).getInstructorCourses(any());
    }

    @Test
    void testUpdateCourse_Returns200WithUpdatedCourse() {
        givenResolvedUser();
        UpdateCourseRequest request = new UpdateCourseRequest("New Title", "New Desc");
        CourseResponse updated = courseResponse(10, "New Title");
        when(instructorCourseService.updateCourse(currentUser, 10, request)).thenReturn(updated);

        ResponseEntity<CourseResponse> response = controller.updateCourse(authentication, 10, request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(updated, response.getBody());
        verify(instructorCourseService, times(1)).updateCourse(currentUser, 10, request);
    }

    @Test
    void testUpdateCourse_NotFoundPropagates() {
        givenResolvedUser();
        UpdateCourseRequest request = new UpdateCourseRequest("T", "D");
        when(instructorCourseService.updateCourse(currentUser, 99, request))
                .thenThrow(new ResourceNotFoundException("Course not found with id: 99"));

        assertThrows(ResourceNotFoundException.class, () -> controller.updateCourse(authentication, 99, request));
    }

    @Test
    void testUpdateCourseStatus_Returns200WithNewStatus() {
        givenResolvedUser();
        UpdateCourseStatusRequest request = new UpdateCourseStatusRequest("PUBLISHED");
        CourseResponse updated = new CourseResponse(10, 1, "Title", "Desc", CourseStatus.PUBLISHED, LocalDateTime.now());
        when(instructorCourseService.updateCourseStatus(currentUser, 10, request)).thenReturn(updated);

        ResponseEntity<CourseResponse> response = controller.updateCourseStatus(authentication, 10, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(CourseStatus.PUBLISHED, response.getBody().getStatus());
        verify(instructorCourseService, times(1)).updateCourseStatus(currentUser, 10, request);
    }

    @Test
    void testSaveDraft_NewDraft_Returns201() {
        givenResolvedUser();
        CourseDraftRequest request = new CourseDraftRequest(null, "WIP description");
        CourseResponse draft = courseResponse(15, "Untitled Draft");
        when(instructorCourseService.saveDraft(currentUser, request)).thenReturn(draft);

        ResponseEntity<CourseResponse> response = controller.saveDraft(authentication, request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(CourseStatus.DRAFT, response.getBody().getStatus());
        verify(instructorCourseService).saveDraft(currentUser, request);
    }

    @Test
    void testSaveDraft_WithExistingIdInBody_Returns200() {
        givenResolvedUser();
        CourseDraftRequest request = new CourseDraftRequest("Updated Draft Title", null);
        request.setId(15);
        CourseResponse draft = courseResponse(15, "Updated Draft Title");
        when(instructorCourseService.saveDraft(currentUser, request)).thenReturn(draft);

        ResponseEntity<CourseResponse> response = controller.saveDraft(authentication, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Updated Draft Title", response.getBody().getTitle());
    }

    @Test
    void testUpdateDraft_Returns200() {
        givenResolvedUser();
        CourseDraftRequest request = new CourseDraftRequest(null, "Partial description");
        CourseResponse updated = courseResponse(10, "Original Title");
        when(instructorCourseService.saveDraft(currentUser, 10, request)).thenReturn(updated);

        ResponseEntity<CourseResponse> response = controller.updateDraft(authentication, 10, request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(updated, response.getBody());
        verify(instructorCourseService).saveDraft(currentUser, 10, request);
    }

    @Test
    void testGetDrafts_Returns200WithDraftList() {
        givenResolvedUser();
        List<CourseResponse> drafts = List.of(courseResponse(10, "Draft 1"));
        when(instructorCourseService.getInstructorDrafts(currentUser)).thenReturn(drafts);

        ResponseEntity<List<CourseResponse>> response = controller.getDrafts(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(instructorCourseService).getInstructorDrafts(currentUser);
    }

    @Test
    void testGetCourseById_Returns200WithCourse() {
        givenResolvedUser();
        CourseResponse course = courseResponse(10, "Draft Course");
        when(instructorCourseService.getInstructorCourseById(currentUser, 10)).thenReturn(course);

        ResponseEntity<CourseResponse> response = controller.getCourseById(authentication, 10);

        assertEquals(200, response.getStatusCode().value());
        assertSame(course, response.getBody());
        verify(instructorCourseService).getInstructorCourseById(currentUser, 10);
    }
}
