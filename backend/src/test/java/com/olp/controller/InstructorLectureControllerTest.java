package com.olp.controller;

import com.olp.dto.ApiResponse;
import com.olp.dto.CreateLectureRequest;
import com.olp.dto.LectureResponse;
import com.olp.dto.ReorderLecturesRequest;
import com.olp.dto.UpdateLectureRequest;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.service.InstructorLectureService;
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
class InstructorLectureControllerTest {

    private static final String URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
    private static final String VIDEO_ID = "dQw4w9WgXcQ";

    @Mock
    private InstructorLectureService instructorLectureService;

    @InjectMocks
    private InstructorLectureController controller;

    private User currentUser;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        currentUser = new User("Dr. Chen", "chen@olp.com", "hash", "instructor");
        currentUser.setId(1);
        authentication = new UsernamePasswordAuthenticationToken(currentUser, null);
    }

    private void givenResolvedUser() {
        when(instructorLectureService.resolveUser(authentication)).thenReturn(currentUser);
    }

    private LectureResponse lectureResponse(Integer id, int orderIndex) {
        return new LectureResponse(id, 10, "Lecture " + id, URL, VIDEO_ID,
                "https://www.youtube.com/embed/" + VIDEO_ID,
                "https://img.youtube.com/vi/" + VIDEO_ID + "/mqdefault.jpg",
                orderIndex, LocalDateTime.now());
    }

    @Test
    void testController_RequiresInstructorRole() {
        PreAuthorize preAuthorize = InstructorLectureController.class.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("hasRole('instructor')", preAuthorize.value());
    }

    // ===== getLectures =====

    @Test
    void testGetLectures_Returns200WithLectures() {
        givenResolvedUser();
        List<LectureResponse> lectures = List.of(lectureResponse(1, 1), lectureResponse(2, 2));
        when(instructorLectureService.getCourseLectures(currentUser, 10)).thenReturn(lectures);

        ResponseEntity<List<LectureResponse>> response = controller.getLectures(10, authentication, null, null);

        assertEquals(200, response.getStatusCode().value());
        assertSame(lectures, response.getBody());
    }

    @Test
    void testGetLectures_MatchingQueryParams_Allowed() {
        givenResolvedUser();
        when(instructorLectureService.getCourseLectures(currentUser, 10)).thenReturn(List.of());

        ResponseEntity<List<LectureResponse>> response = controller.getLectures(10, authentication, 1, 1);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void testGetLectures_InstructorIdOfAnotherUser_ThrowsAccessDenied() {
        givenResolvedUser();

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> controller.getLectures(10, authentication, 99, null));

        assertTrue(ex.getMessage().contains("Cannot access another instructor's course lectures"));
        verify(instructorLectureService, never()).getCourseLectures(any(), any());
    }

    @Test
    void testGetLectures_UserIdOfAnotherUser_ThrowsAccessDenied() {
        givenResolvedUser();

        assertThrows(AccessDeniedException.class, () -> controller.getLectures(10, authentication, null, 99));
        verify(instructorLectureService, never()).getCourseLectures(any(), any());
    }

    // ===== reorderLectures =====

    @Test
    void testReorderLectures_Returns200WithReorderedList() {
        givenResolvedUser();
        ReorderLecturesRequest request = new ReorderLecturesRequest(List.of(2, 1));
        List<LectureResponse> reordered = List.of(lectureResponse(2, 1), lectureResponse(1, 2));
        when(instructorLectureService.reorderLectures(currentUser, 10, request)).thenReturn(reordered);

        ResponseEntity<List<LectureResponse>> response = controller.reorderLectures(10, request, authentication, null, null);

        assertEquals(200, response.getStatusCode().value());
        assertSame(reordered, response.getBody());
    }

    @Test
    void testReorderLectures_InstructorIdOfAnotherUser_ThrowsAccessDenied() {
        givenResolvedUser();
        ReorderLecturesRequest request = new ReorderLecturesRequest(List.of(1));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> controller.reorderLectures(10, request, authentication, 99, null));

        assertTrue(ex.getMessage().contains("Cannot modify another instructor's course lectures"));
        verify(instructorLectureService, never()).reorderLectures(any(), any(), any());
    }

    @Test
    void testReorderLectures_UserIdOfAnotherUser_ThrowsAccessDenied() {
        givenResolvedUser();
        ReorderLecturesRequest request = new ReorderLecturesRequest(List.of(1));

        assertThrows(AccessDeniedException.class,
                () -> controller.reorderLectures(10, request, authentication, null, 99));
        verify(instructorLectureService, never()).reorderLectures(any(), any(), any());
    }

    // ===== deleteLecture =====

    @Test
    void testDeleteLecture_Returns200WithSuccessMessage() {
        givenResolvedUser();

        ResponseEntity<ApiResponse> response = controller.deleteLecture(10, 3, authentication, null, null);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Lecture deleted successfully", response.getBody().getMessage());
        verify(instructorLectureService, times(1)).deleteLecture(currentUser, 10, 3);
    }

    @Test
    void testDeleteLecture_InstructorIdOfAnotherUser_ThrowsAccessDenied() {
        givenResolvedUser();

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> controller.deleteLecture(10, 3, authentication, 99, null));

        assertTrue(ex.getMessage().contains("Cannot modify another instructor's course lectures"));
        verify(instructorLectureService, never()).deleteLecture(any(), any(), any());
    }

    @Test
    void testDeleteLecture_UserIdOfAnotherUser_ThrowsAccessDenied() {
        givenResolvedUser();

        assertThrows(AccessDeniedException.class, () -> controller.deleteLecture(10, 3, authentication, null, 99));
        verify(instructorLectureService, never()).deleteLecture(any(), any(), any());
    }

    @Test
    void testDeleteLecture_ServiceNotFoundPropagates() {
        givenResolvedUser();
        doThrow(new ResourceNotFoundException("Lecture not found with id: 3"))
                .when(instructorLectureService).deleteLecture(currentUser, 10, 3);

        assertThrows(ResourceNotFoundException.class, () -> controller.deleteLecture(10, 3, authentication, null, null));
    }

    // ===== createLecture =====

    @Test
    void testCreateLecture_Returns201WithCreatedLecture() {
        givenResolvedUser();
        CreateLectureRequest request = new CreateLectureRequest("Intro", URL);
        LectureResponse created = lectureResponse(5, 1);
        when(instructorLectureService.createLecture(currentUser, 10, request)).thenReturn(created);

        ResponseEntity<LectureResponse> response = controller.createLecture(10, request, authentication, null, null);

        assertEquals(201, response.getStatusCode().value());
        assertSame(created, response.getBody());
    }

    @Test
    void testCreateLecture_InstructorIdOfAnotherUser_ThrowsAccessDenied() {
        givenResolvedUser();
        CreateLectureRequest request = new CreateLectureRequest("Intro", URL);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> controller.createLecture(10, request, authentication, 99, null));

        assertTrue(ex.getMessage().contains("Cannot modify another instructor's course lectures"));
        verify(instructorLectureService, never()).createLecture(any(), any(), any());
    }

    @Test
    void testCreateLecture_UserIdOfAnotherUser_ThrowsAccessDenied() {
        givenResolvedUser();
        CreateLectureRequest request = new CreateLectureRequest("Intro", URL);

        assertThrows(AccessDeniedException.class,
                () -> controller.createLecture(10, request, authentication, null, 99));
        verify(instructorLectureService, never()).createLecture(any(), any(), any());
    }

    @Test
    void testCreateLecture_InvalidUrlFailurePropagates() {
        givenResolvedUser();
        CreateLectureRequest request = new CreateLectureRequest("Intro", "https://example.com/video");
        when(instructorLectureService.createLecture(currentUser, 10, request))
                .thenThrow(new IllegalArgumentException("Invalid YouTube URL"));

        assertThrows(IllegalArgumentException.class,
                () -> controller.createLecture(10, request, authentication, null, null));
    }

    // ===== updateLecture =====

    @Test
    void testUpdateLecture_Returns200WithUpdatedLecture() {
        givenResolvedUser();
        UpdateLectureRequest request = new UpdateLectureRequest("New Title", URL);
        LectureResponse updated = lectureResponse(3, 2);
        when(instructorLectureService.updateLecture(currentUser, 10, 3, request)).thenReturn(updated);

        ResponseEntity<LectureResponse> response = controller.updateLecture(10, 3, request, authentication, null, null);

        assertEquals(200, response.getStatusCode().value());
        assertSame(updated, response.getBody());
    }

    @Test
    void testUpdateLecture_MatchingQueryParams_Allowed() {
        givenResolvedUser();
        UpdateLectureRequest request = new UpdateLectureRequest("New Title", null);
        when(instructorLectureService.updateLecture(currentUser, 10, 3, request)).thenReturn(lectureResponse(3, 1));

        ResponseEntity<LectureResponse> response = controller.updateLecture(10, 3, request, authentication, 1, 1);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testUpdateLecture_InstructorIdOfAnotherUser_ThrowsAccessDenied() {
        givenResolvedUser();
        UpdateLectureRequest request = new UpdateLectureRequest("New Title", null);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> controller.updateLecture(10, 3, request, authentication, 99, null));

        assertTrue(ex.getMessage().contains("Cannot modify another instructor's course lectures"));
        verify(instructorLectureService, never()).updateLecture(any(), any(), any(), any());
    }

    @Test
    void testUpdateLecture_UserIdOfAnotherUser_ThrowsAccessDenied() {
        givenResolvedUser();
        UpdateLectureRequest request = new UpdateLectureRequest("New Title", null);

        assertThrows(AccessDeniedException.class,
                () -> controller.updateLecture(10, 3, request, authentication, null, 99));
        verify(instructorLectureService, never()).updateLecture(any(), any(), any(), any());
    }
}
