package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.service.CourseStructureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseStructureControllerTest {

    @Mock
    private CourseStructureService service;

    @InjectMocks
    private CourseStructureController controller;

    private User instructor;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        instructor = new User("Dr. Turing", "turing@olp.com", "hash", "instructor", "active");
        instructor.setId(10);
        authentication = new UsernamePasswordAuthenticationToken(instructor, null);
    }

    private void givenResolvedUser() {
        when(service.resolveUser(authentication)).thenReturn(instructor);
    }

    @Test
    void testSecurityAnnotation_RequiresInstructorOrAdminRole() {
        PreAuthorize preAuth = CourseStructureController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuth);
        assertTrue(preAuth.value().contains("hasRole('instructor')"));
        assertTrue(preAuth.value().contains("hasRole('admin')"));
    }

    @Test
    void testGetCourseStructure_Success() {
        givenResolvedUser();
        CourseStructureResponse expected = new CourseStructureResponse(10, "Spring Boot", "DRAFT", List.of());
        when(service.getCourseStructure(instructor, 10)).thenReturn(expected);

        ResponseEntity<CourseStructureResponse> response = controller.getCourseStructure(10, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }

    // =========================================================================
    // MODULES
    // =========================================================================

    @Test
    void testCreateModule_Success() {
        givenResolvedUser();
        CourseModuleRequest req = new CourseModuleRequest("Module 1", "Desc", 1);
        CourseModuleResponse expected = new CourseModuleResponse(101, 10, "Module 1", "Desc", 1, List.of(), LocalDateTime.now(), LocalDateTime.now());
        when(service.createModule(instructor, 10, req)).thenReturn(expected);

        ResponseEntity<CourseModuleResponse> response = controller.createModule(10, req, authentication);

        assertEquals(201, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }

    @Test
    void testGetModules_Success() {
        givenResolvedUser();
        CourseModuleResponse m1 = new CourseModuleResponse(101, 10, "M1", null, 1, List.of(), LocalDateTime.now(), LocalDateTime.now());
        when(service.getModules(instructor, 10)).thenReturn(List.of(m1));

        ResponseEntity<List<CourseModuleResponse>> response = controller.getModules(10, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testUpdateModule_Success() {
        givenResolvedUser();
        CourseModuleRequest req = new CourseModuleRequest("M1 Updated", "New Desc", 2);
        CourseModuleResponse expected = new CourseModuleResponse(101, 10, "M1 Updated", "New Desc", 2, List.of(), LocalDateTime.now(), LocalDateTime.now());
        when(service.updateModule(instructor, 10, 101, req)).thenReturn(expected);

        ResponseEntity<CourseModuleResponse> response = controller.updateModule(10, 101, req, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }

    @Test
    void testDeleteModule_Success() {
        givenResolvedUser();
        ApiResponse expected = new ApiResponse(true, "Module deleted successfully");
        when(service.deleteModule(instructor, 10, 101)).thenReturn(expected);

        ResponseEntity<ApiResponse> response = controller.deleteModule(10, 101, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void testReorderModules_Success() {
        givenResolvedUser();
        ReorderStructureRequest req = new ReorderStructureRequest(List.of(102, 101));
        when(service.reorderModules(instructor, 10, req)).thenReturn(List.of());

        ResponseEntity<List<CourseModuleResponse>> response = controller.reorderModules(10, req, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    // =========================================================================
    // SECTIONS
    // =========================================================================

    @Test
    void testCreateSection_Success() {
        givenResolvedUser();
        CourseSectionRequest req = new CourseSectionRequest("Section 1", "Desc", 1);
        CourseSectionResponse expected = new CourseSectionResponse(201, 101, "Section 1", "Desc", 1, List.of(), LocalDateTime.now(), LocalDateTime.now());
        when(service.createSection(instructor, 10, 101, req)).thenReturn(expected);

        ResponseEntity<CourseSectionResponse> response = controller.createSection(10, 101, req, authentication);

        assertEquals(201, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }

    @Test
    void testUpdateSection_Success() {
        givenResolvedUser();
        CourseSectionRequest req = new CourseSectionRequest("Section 1 Renamed");
        CourseSectionResponse expected = new CourseSectionResponse(201, 101, "Section 1 Renamed", null, 1, List.of(), LocalDateTime.now(), LocalDateTime.now());
        when(service.updateSection(instructor, 10, 101, 201, req)).thenReturn(expected);

        ResponseEntity<CourseSectionResponse> response = controller.updateSection(10, 101, 201, req, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }

    @Test
    void testDeleteSection_Success() {
        givenResolvedUser();
        ApiResponse expected = new ApiResponse(true, "Section deleted successfully");
        when(service.deleteSection(instructor, 10, 101, 201)).thenReturn(expected);

        ResponseEntity<ApiResponse> response = controller.deleteSection(10, 101, 201, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void testReorderSections_Success() {
        givenResolvedUser();
        ReorderStructureRequest req = new ReorderStructureRequest(List.of(202, 201));
        when(service.reorderSections(instructor, 10, 101, req)).thenReturn(List.of());

        ResponseEntity<List<CourseSectionResponse>> response = controller.reorderSections(10, 101, req, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    // =========================================================================
    // LESSONS
    // =========================================================================

    @Test
    void testCreateLesson_Success() {
        givenResolvedUser();
        LessonRequest req = new LessonRequest("Video 1", "VIDEO", "Content", "https://url.com", 15, 1, null);
        LessonResponse expected = new LessonResponse(301, 201, "Video 1", "VIDEO", "Content", "https://url.com", 15, 1, null, LocalDateTime.now(), LocalDateTime.now());
        when(service.createLesson(instructor, 10, 101, 201, req)).thenReturn(expected);

        ResponseEntity<LessonResponse> response = controller.createLesson(10, 101, 201, req, authentication);

        assertEquals(201, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }

    @Test
    void testUpdateLesson_Success() {
        givenResolvedUser();
        LessonRequest req = new LessonRequest("Updated Video 1", "VIDEO");
        LessonResponse expected = new LessonResponse(301, 201, "Updated Video 1", "VIDEO", null, null, null, 1, null, LocalDateTime.now(), LocalDateTime.now());
        when(service.updateLesson(instructor, 10, 101, 201, 301, req)).thenReturn(expected);

        ResponseEntity<LessonResponse> response = controller.updateLesson(10, 101, 201, 301, req, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expected, response.getBody());
    }

    @Test
    void testDeleteLesson_Success() {
        givenResolvedUser();
        ApiResponse expected = new ApiResponse(true, "Lesson deleted successfully");
        when(service.deleteLesson(instructor, 10, 101, 201, 301)).thenReturn(expected);

        ResponseEntity<ApiResponse> response = controller.deleteLesson(10, 101, 201, 301, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void testReorderLessons_Success() {
        givenResolvedUser();
        ReorderStructureRequest req = new ReorderStructureRequest(List.of(302, 301));
        when(service.reorderLessons(instructor, 10, 101, 201, req)).thenReturn(List.of());

        ResponseEntity<List<LessonResponse>> response = controller.reorderLessons(10, 101, 201, req, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }
}

