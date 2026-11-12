package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.service.QuizService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizControllerTest {

    @Mock
    private QuizService quizService;

    @InjectMocks
    private QuizController controller;

    private User instructor;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        instructor = new User("Dr. Ada", "ada@olp.com", "hash", "instructor", "active");
        instructor.setId(5);
        authentication = new UsernamePasswordAuthenticationToken(instructor, null);
    }

    private void givenResolvedUser() {
        when(quizService.resolveUser(authentication)).thenReturn(instructor);
    }

    @Test
    void testSecurityAnnotation_RequiresInstructorOrAdmin() {
        PreAuthorize preAuth = QuizController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuth);
        assertTrue(preAuth.value().contains("hasRole('instructor')"));
        assertTrue(preAuth.value().contains("hasRole('admin')"));
    }

    @Test
    void testCreateQuiz_Success() {
        givenResolvedUser();
        QuizRequest req = new QuizRequest();
        req.setCourseId(10);
        req.setTitle("Week 1 Quiz");

        QuizResponse expected = new QuizResponse();
        expected.setId(1);
        expected.setStatus("DRAFT");
        when(quizService.createQuiz(instructor, req)).thenReturn(expected);

        ResponseEntity<QuizResponse> res = controller.createQuiz(req, authentication);
        assertEquals(201, res.getStatusCode().value());
        assertSame(expected, res.getBody());
    }

    @Test
    void testCreateQuizForCourse_SetsCourseIdAndReturns201() {
        givenResolvedUser();
        QuizRequest req = new QuizRequest();
        req.setTitle("Course Quiz");

        QuizResponse expected = new QuizResponse();
        expected.setId(2);
        expected.setCourseId(10);
        when(quizService.createQuiz(instructor, req)).thenReturn(expected);

        ResponseEntity<QuizResponse> res = controller.createQuizForCourse(10, req, authentication);
        assertEquals(201, res.getStatusCode().value());
        assertEquals(10, req.getCourseId());
        assertSame(expected, res.getBody());
    }

    @Test
    void testGetMyQuizzes_AndCourseModuleQuizzes_Success() {
        givenResolvedUser();
        QuizResponse q = new QuizResponse();
        q.setId(3);
        when(quizService.getMyQuizzes(instructor, "DRAFT")).thenReturn(List.of(q));
        when(quizService.getQuizzesByCourse(instructor, 10, null)).thenReturn(List.of(q));
        when(quizService.getQuizzesByModule(instructor, 10, 100)).thenReturn(List.of(q));
        when(quizService.getQuizById(instructor, 3)).thenReturn(q);

        assertEquals(200, controller.getMyQuizzes("DRAFT", authentication).getStatusCode().value());
        assertEquals(200, controller.getQuizzesByCourse(10, null, authentication).getStatusCode().value());
        assertEquals(200, controller.getQuizzesByModule(10, 100, authentication).getStatusCode().value());
        assertEquals(200, controller.getQuizById(3, authentication).getStatusCode().value());
    }

    @Test
    void testUpdateAndDeleteQuiz_Success() {
        givenResolvedUser();
        QuizRequest req = new QuizRequest();
        req.setTitle("Updated Quiz");
        QuizResponse updated = new QuizResponse();
        updated.setId(3);
        updated.setTitle("Updated Quiz");

        when(quizService.updateQuiz(instructor, 3, req)).thenReturn(updated);
        when(quizService.deleteQuiz(instructor, 3)).thenReturn(new ApiResponse(true, "Quiz deleted successfully"));

        ResponseEntity<QuizResponse> updateRes = controller.updateQuiz(3, req, authentication);
        assertEquals(200, updateRes.getStatusCode().value());
        assertSame(updated, updateRes.getBody());

        ResponseEntity<ApiResponse> deleteRes = controller.deleteQuiz(3, authentication);
        assertEquals(200, deleteRes.getStatusCode().value());
        assertTrue(deleteRes.getBody().isSuccess());
    }

    @Test
    void testQuizQuestionsEndpoints_Success() {
        givenResolvedUser();
        QuizQuestionResponse qq = new QuizQuestionResponse();
        qq.setId(50);
        qq.setOrderIndex(1);

        QuizQuestionRequest addReq = new QuizQuestionRequest();
        addReq.setQuestionText("Sample?");

        ReorderStructureRequest reorderReq = new ReorderStructureRequest(List.of(50));

        when(quizService.getQuizQuestions(instructor, 3)).thenReturn(List.of(qq));
        when(quizService.addQuestionToQuiz(instructor, 3, addReq)).thenReturn(qq);
        when(quizService.updateQuizQuestion(instructor, 3, 50, addReq)).thenReturn(qq);
        when(quizService.removeQuestionFromQuiz(instructor, 3, 50))
                .thenReturn(new ApiResponse(true, "Question removed from quiz successfully"));
        when(quizService.reorderQuizQuestions(instructor, 3, reorderReq)).thenReturn(List.of(qq));

        assertEquals(200, controller.getQuizQuestions(3, authentication).getStatusCode().value());
        assertEquals(201, controller.addQuestionToQuiz(3, addReq, authentication).getStatusCode().value());
        assertEquals(200, controller.updateQuizQuestion(3, 50, addReq, authentication).getStatusCode().value());
        assertEquals(200, controller.removeQuestionFromQuiz(3, 50, authentication).getStatusCode().value());
        assertEquals(200, controller.reorderQuizQuestions(3, reorderReq, authentication).getStatusCode().value());
    }

    @Test
    void testQuizSettingsEndpoints_Success() {
        givenResolvedUser();
        QuizSettingsRequest req = new QuizSettingsRequest();
        req.setTimeLimitMinutes(30);
        req.setMaxAttempts(2);

        QuizSettingsResponse expected = new QuizSettingsResponse();
        expected.setQuizId(3);
        expected.setTimeLimitMinutes(30);
        expected.setMaxAttempts(2);

        when(quizService.getQuizSettings(instructor, 3)).thenReturn(expected);
        when(quizService.configureQuizSettings(instructor, 3, req)).thenReturn(expected);

        assertEquals(200, controller.getQuizSettings(3, authentication).getStatusCode().value());
        assertSame(expected, controller.createOrConfigureQuizSettings(3, req, authentication).getBody());
        assertSame(expected, controller.updateQuizSettings(3, req, authentication).getBody());
    }
}

