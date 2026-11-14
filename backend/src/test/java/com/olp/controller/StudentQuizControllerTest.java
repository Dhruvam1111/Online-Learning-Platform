package com.olp.controller;

import com.olp.dto.QuizAttemptResponse;
import com.olp.dto.QuizEvaluationSummary;
import com.olp.dto.QuizSettingsResponse;
import com.olp.entity.User;
import com.olp.service.QuizAttemptService;
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
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentQuizControllerTest {

    @Mock
    private QuizAttemptService quizAttemptService;

    @InjectMocks
    private StudentQuizController controller;

    private User student;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        student = new User("Alice", "alice@olp.com", "hash", "student", "active");
        student.setId(3);
        authentication = new UsernamePasswordAuthenticationToken(student, null);
    }

    @Test
    void testSecurityAnnotation_RequiresStudentRole() {
        PreAuthorize preAuth = StudentQuizController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuth);
        assertTrue(preAuth.value().contains("hasRole('student')"));
    }

    @Test
    void testStudentQuizSettingsAndAttemptEndpoints_Success() {
        when(quizAttemptService.resolveUser(authentication)).thenReturn(student);

        QuizSettingsResponse settings = new QuizSettingsResponse();
        settings.setQuizId(10);
        settings.setCurrentlyAvailable(true);
        when(quizAttemptService.getStudentQuizSettings(student, 10)).thenReturn(settings);

        QuizAttemptResponse attempt = new QuizAttemptResponse();
        attempt.setId(55);
        attempt.setQuizId(10);
        attempt.setStatus("IN_PROGRESS");
        when(quizAttemptService.startQuizAttempt(student, 10)).thenReturn(attempt);
        when(quizAttemptService.getStudentQuizAttempts(student, 10)).thenReturn(List.of(attempt));
        when(quizAttemptService.getQuizAttemptStatus(student, 10, 55)).thenReturn(attempt);

        QuizAttemptResponse submitted = new QuizAttemptResponse();
        submitted.setId(55);
        submitted.setStatus("SUBMITTED");
        when(quizAttemptService.submitQuizAttempt(student, 10, 55, null)).thenReturn(submitted);

        QuizEvaluationSummary evalSummary = new QuizEvaluationSummary();
        evalSummary.setQuizId(10);
        evalSummary.setPassed(true);
        when(quizAttemptService.getAttemptEvaluation(student, 10, 55)).thenReturn(evalSummary);

        assertSame(settings, controller.getStudentQuizSettings(10, authentication).getBody());

        ResponseEntity<QuizAttemptResponse> startRes = controller.startQuizAttempt(10, authentication);
        assertEquals(201, startRes.getStatusCode().value());
        assertSame(attempt, startRes.getBody());

        assertEquals(1, Objects.requireNonNull(controller.getStudentQuizAttempts(10, authentication).getBody()).size());
        assertSame(attempt, controller.getQuizAttemptStatus(10, 55, authentication).getBody());
        assertEquals("SUBMITTED", Objects.requireNonNull(controller.submitQuizAttempt(10, 55, null, authentication).getBody()).getStatus());
        assertSame(evalSummary, controller.getAttemptEvaluation(10, 55, authentication).getBody());
    }
}
