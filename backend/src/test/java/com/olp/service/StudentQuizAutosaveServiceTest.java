package com.olp.service;

import com.olp.dto.SaveAnswerRequest;
import com.olp.entity.*;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class StudentQuizAutosaveServiceTest {

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
    private QuizAttempt attempt;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1);
        student.setRole("student");
        student.setAccountStatus("active");

        quiz = new Quiz();
        quiz.setId(10);
        quiz.setTimeLimitMinutes(30);

        attempt = new QuizAttempt();
        attempt.setId(100);
        attempt.setQuiz(quiz);
        attempt.setStudent(student);
        attempt.setStatus(QuizAttemptStatus.IN_PROGRESS);
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(5));
        attempt.setExpiresAt(LocalDateTime.now().plusMinutes(25));
    }

    @Test
    void testSaveAttemptAnswer_SavesNewAnswer() {
        when(quizAttemptRepository.findByIdAndQuizId(attempt.getId(), quiz.getId()))
                .thenReturn(Optional.of(attempt));

        Question q = new Question();
        q.setId(201);
        QuizQuestion qq = new QuizQuestion();
        qq.setQuestion(q);

        when(quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId()))
                .thenReturn(List.of(qq));
        when(quizAttemptAnswerRepository.findByAttemptIdAndQuestionId(attempt.getId(), 201))
                .thenReturn(Optional.empty());

        SaveAnswerRequest req = new SaveAnswerRequest(201, "Answer A");

        assertDoesNotThrow(() -> quizAttemptService.saveAttemptAnswer(student, quiz.getId(), attempt.getId(), req));
        verify(quizAttemptAnswerRepository, times(1)).save(any(QuizAttemptAnswer.class));
    }

    @Test
    void testSaveAttemptAnswer_UpdatesExistingAnswer() {
        when(quizAttemptRepository.findByIdAndQuizId(attempt.getId(), quiz.getId()))
                .thenReturn(Optional.of(attempt));

        Question q = new Question();
        q.setId(201);
        QuizQuestion qq = new QuizQuestion();
        qq.setQuestion(q);

        QuizAttemptAnswer existing = new QuizAttemptAnswer();
        existing.setSubmittedAnswer("Old Choice");

        when(quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId()))
                .thenReturn(List.of(qq));
        when(quizAttemptAnswerRepository.findByAttemptIdAndQuestionId(attempt.getId(), 201))
                .thenReturn(Optional.of(existing));

        SaveAnswerRequest req = new SaveAnswerRequest(201, "New Choice");

        assertDoesNotThrow(() -> quizAttemptService.saveAttemptAnswer(student, quiz.getId(), attempt.getId(), req));
        assertEquals("New Choice", existing.getSubmittedAnswer());
        verify(quizAttemptAnswerRepository, times(1)).save(existing);
    }
}