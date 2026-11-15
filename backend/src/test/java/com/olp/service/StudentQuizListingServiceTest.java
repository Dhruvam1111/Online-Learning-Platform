package com.olp.service;

import com.olp.dto.QuizSettingsResponse;
import com.olp.entity.*;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class StudentQuizListingServiceTest {

    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuizQuestionRepository quizQuestionRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private QuizService quizService;
    @Mock
    private QuizAttemptAnswerRepository quizAttemptAnswerRepository;

    @InjectMocks
    private QuizAttemptService quizAttemptService;

    private User student;
    private Quiz quiz;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1);
        student.setRole("student");
        student.setAccountStatus("active");

        quiz = new Quiz();
        quiz.setId(10);
        quiz.setTitle("Midterm Quiz");
        quiz.setStatus(QuizStatus.PUBLISHED);
    }

    @Test
    void testGetStudentCourseQuizzes_Enrolled_ReturnsQuizzes() {
        Integer courseId = 50;
        when(enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), courseId)).thenReturn(true);
        when(quizRepository.findByCourseIdAndStatusOrderByCreatedAtDesc(courseId, QuizStatus.PUBLISHED))
                .thenReturn(List.of(quiz));

        QuizSettingsResponse settings = new QuizSettingsResponse();
        settings.setQuizId(quiz.getId());
        settings.setTitle(quiz.getTitle());
        when(quizService.buildQuizSettingsResponse(eq(quiz), anyList(), any(), anyLong())).thenReturn(settings);

        List<QuizSettingsResponse> result = quizAttemptService.getStudentCourseQuizzes(student, courseId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Midterm Quiz", result.get(0).getTitle());
    }

    @Test
    void testGetStudentCourseQuizzes_NotEnrolled_ThrowsAccessDenied() {
        Integer courseId = 50;
        when(enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), courseId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                quizAttemptService.getStudentCourseQuizzes(student, courseId));
    }
}