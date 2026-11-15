package com.olp.service;

import com.olp.dto.StudentQuizQuestionDto;
import com.olp.entity.*;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class StudentQuizAttemptServiceTest  {

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
    void testGetAttemptQuestions_InProgress_ReturnsQuestionsWithoutCorrectAnswer() {
        when(quizAttemptRepository.findByIdAndQuizId(attempt.getId(), quiz.getId()))
                .thenReturn(Optional.of(attempt));

        Question q = new Question();
        q.setId(201);
        q.setQuestionText("What is OOP?");
        q.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        q.setOptionsJson("[\"A\",\"B\"]");
        q.setCorrectAnswer("A"); // Security check: must never be returned to student

        QuizQuestion qq = new QuizQuestion();
        qq.setQuestion(q);
        qq.setOrderIndex(1);
        qq.setMarks(new BigDecimal("1.0"));

        when(quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId()))
                .thenReturn(List.of(qq));
        when(quizAttemptAnswerRepository.findByAttemptId(attempt.getId()))
                .thenReturn(List.of());

        List<StudentQuizQuestionDto> questions = quizAttemptService.getAttemptQuestions(student, quiz.getId(), attempt.getId());

        assertNotNull(questions);
        assertEquals(1, questions.size());
        assertEquals(201, questions.get(0).getQuestionId());
        assertEquals("What is OOP?", questions.get(0).getQuestionText());
    }
}