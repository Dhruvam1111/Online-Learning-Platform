package com.olp.controller;

import com.olp.dto.ApiResponse;
import com.olp.dto.QuestionRequest;
import com.olp.dto.QuestionResponse;
import com.olp.entity.User;
import com.olp.service.QuestionService;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionControllerTest {

    @Mock
    private QuestionService questionService;

    @InjectMocks
    private QuestionController controller;

    private User instructor;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        instructor = new User("Dr. Hopper", "hopper@olp.com", "hash", "instructor", "active");
        instructor.setId(8);
        authentication = new UsernamePasswordAuthenticationToken(instructor, null);
    }

    private void givenResolvedUser() {
        when(questionService.resolveUser(authentication)).thenReturn(instructor);
    }

    @Test
    void testSecurityAnnotation_RequiresInstructorOrAdmin() {
        PreAuthorize preAuth = QuestionController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuth);
        assertTrue(preAuth.value().contains("hasRole('instructor')"));
        assertTrue(preAuth.value().contains("hasRole('admin')"));
    }

    @Test
    void testCreateGetUpdateDeleteAndManualGradingEndpoints() {
        givenResolvedUser();
        QuestionRequest req = new QuestionRequest();
        req.setQuestionText("Q?");
        req.setQuestionType("TRUE_FALSE");

        QuestionResponse qRes = new QuestionResponse();
        qRes.setId(12);
        qRes.setQuestionType("TRUE_FALSE");

        when(questionService.createQuestion(instructor, req)).thenReturn(qRes);
        when(questionService.getQuestions(instructor, null, null, null)).thenReturn(List.of(qRes));
        when(questionService.getQuestions(instructor, 10, "DESCRIPTIVE", true)).thenReturn(List.of(qRes));
        when(questionService.getQuestionById(instructor, 12)).thenReturn(qRes);
        when(questionService.updateQuestion(instructor, 12, req)).thenReturn(qRes);
        when(questionService.deleteQuestion(instructor, 12)).thenReturn(new ApiResponse(true, "Question deleted successfully"));
        when(questionService.getManualGradingQuestions(instructor, null)).thenReturn(List.of(qRes));
        when(questionService.sendForManualGrading(instructor, 12, "Rubric")).thenReturn(qRes);

        ResponseEntity<QuestionResponse> created = controller.createQuestion(req, authentication);
        assertEquals(201, created.getStatusCode().value());

        ResponseEntity<QuestionResponse> createdForCourse = controller.createQuestionForCourse(10, req, authentication);
        assertEquals(201, createdForCourse.getStatusCode().value());
        assertEquals(10, req.getCourseId());

        assertEquals(1, controller.getQuestions(null, null, null, authentication).getBody().size());
        assertEquals(1, controller.getQuestionsByCourse(10, "DESCRIPTIVE", true, authentication).getBody().size());
        assertSame(qRes, controller.getQuestionById(12, authentication).getBody());
        assertSame(qRes, controller.updateQuestion(12, req, authentication).getBody());
        assertTrue(controller.deleteQuestion(12, authentication).getBody().isSuccess());
        assertEquals(1, controller.getManualGradingQuestions(null, authentication).getBody().size());
        assertSame(qRes, controller.sendForManualGrading(12, Map.of("gradingRubric", "Rubric"), authentication).getBody());
    }
}

