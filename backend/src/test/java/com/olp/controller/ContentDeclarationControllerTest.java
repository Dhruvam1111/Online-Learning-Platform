package com.olp.controller;

import com.olp.dto.ContentDeclarationRequest;
import com.olp.dto.ContentDeclarationResponse;
import com.olp.dto.TakedownReplaceRequest;
import com.olp.dto.TakedownResponseRequest;
import com.olp.entity.LicenseType;
import com.olp.entity.TakedownStatus;
import com.olp.entity.User;
import com.olp.service.ContentDeclarationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentDeclarationControllerTest {

    @Mock
    private ContentDeclarationService declarationService;

    @InjectMocks
    private ContentDeclarationController controller;

    private User instructor;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        instructor = new User("Jane Instructor", "jane@olp.com", "hash", "instructor");
        instructor.setId(10);
        authentication = new UsernamePasswordAuthenticationToken(instructor, null);
    }

    private ContentDeclarationResponse sampleResponse(Integer id, Integer courseId, Integer lessonId) {
        ContentDeclarationResponse resp = new ContentDeclarationResponse();
        resp.setId(id);
        resp.setCourseId(courseId);
        resp.setLessonId(lessonId);
        resp.setLicenseType(LicenseType.ORIGINAL_CREATOR);
        resp.setTakedownStatus(TakedownStatus.NONE);
        return resp;
    }

    @Test
    void testDeclareContent() {
        ContentDeclarationRequest request = new ContentDeclarationRequest();
        request.setCourseId(100);
        request.setLicenseType(LicenseType.ORIGINAL_CREATOR);
        request.setDeclarationText("Sole author declaration.");

        ContentDeclarationResponse expected = sampleResponse(1, 100, null);

        when(declarationService.resolveUser(authentication)).thenReturn(instructor);
        when(declarationService.declareContent(eq(instructor), eq(request))).thenReturn(expected);

        ResponseEntity<ContentDeclarationResponse> response = controller.declareContent(authentication, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getId());
        verify(declarationService, times(1)).declareContent(instructor, request);
    }

    @Test
    void testGetDeclarationsByCourse() {
        ContentDeclarationResponse resp = sampleResponse(1, 100, null);
        when(declarationService.resolveUser(authentication)).thenReturn(instructor);
        when(declarationService.getDeclarationsByCourse(100, instructor)).thenReturn(List.of(resp));

        ResponseEntity<List<ContentDeclarationResponse>> response = controller.getDeclarationsByCourse(100, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(1, response.getBody().get(0).getId());
    }

    @Test
    void testGetDeclarationByLesson() {
        ContentDeclarationResponse resp = sampleResponse(5, 100, 400);
        when(declarationService.resolveUser(authentication)).thenReturn(instructor);
        when(declarationService.getDeclarationByLesson(400, instructor)).thenReturn(resp);

        ResponseEntity<ContentDeclarationResponse> response = controller.getDeclarationByLesson(400, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().getId());
        assertEquals(400, response.getBody().getLessonId());
    }

    @Test
    void testGetDeclarationById() {
        ContentDeclarationResponse resp = sampleResponse(7, 100, null);
        when(declarationService.resolveUser(authentication)).thenReturn(instructor);
        when(declarationService.getDeclarationById(7, instructor)).thenReturn(resp);

        ResponseEntity<ContentDeclarationResponse> response = controller.getDeclarationById(7, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(7, response.getBody().getId());
    }

    @Test
    void testRespondToTakedown() {
        TakedownResponseRequest request = new TakedownResponseRequest();
        request.setResponse("Response statement");

        ContentDeclarationResponse resp = sampleResponse(7, 100, null);
        resp.setTakedownStatus(TakedownStatus.RESPONDED);
        resp.setInstructorResponse("Response statement");

        when(declarationService.resolveUser(authentication)).thenReturn(instructor);
        when(declarationService.respondToTakedown(eq(7), eq(instructor), eq(request))).thenReturn(resp);

        ResponseEntity<ContentDeclarationResponse> response = controller.respondToTakedown(7, authentication, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(TakedownStatus.RESPONDED, response.getBody().getTakedownStatus());
    }

    @Test
    void testRemoveContent() {
        ContentDeclarationResponse resp = sampleResponse(7, 100, null);
        resp.setTakedownStatus(TakedownStatus.REMOVED);

        when(declarationService.resolveUser(authentication)).thenReturn(instructor);
        when(declarationService.removeContent(7, instructor)).thenReturn(resp);

        ResponseEntity<ContentDeclarationResponse> response = controller.removeContent(7, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(TakedownStatus.REMOVED, response.getBody().getTakedownStatus());
    }

    @Test
    void testReplaceContent() {
        TakedownReplaceRequest request = new TakedownReplaceRequest();
        request.setSourceAttribution("https://example.com/license");

        ContentDeclarationResponse resp = sampleResponse(7, 100, null);
        resp.setTakedownStatus(TakedownStatus.REPLACED);
        resp.setSourceAttribution("https://example.com/license");

        when(declarationService.resolveUser(authentication)).thenReturn(instructor);
        when(declarationService.replaceContent(eq(7), eq(instructor), eq(request))).thenReturn(resp);

        ResponseEntity<ContentDeclarationResponse> response = controller.replaceContent(7, authentication, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(TakedownStatus.REPLACED, response.getBody().getTakedownStatus());
    }

    @Test
    void testCheckDeclarationStatus() {
        Map<String, Object> map = Map.of("courseId", 100, "lessonId", 400, "declared", true);
        when(declarationService.resolveUser(authentication)).thenReturn(instructor);
        when(declarationService.checkDeclarationStatus(100, 400, instructor)).thenReturn(map);

        ResponseEntity<Map<String, Object>> response = controller.checkDeclarationStatus(100, 400, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("declared"));
    }
}

