package com.olp.service;

import com.olp.dto.StudentExamAnswerRequest;
import com.olp.entity.*;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentExamLifecycleServiceTest {
    @Mock StudentExamReadRepository exams;
    @Mock StudentExamAttemptReadRepository attempts;
    @Mock ExamAttemptAnswerRepository answers;
    @Mock ExamQuestionRepository examQuestions;
    @Mock QuestionRepository questions;
    @Mock ExamService existing;
    StudentExamLifecycleService service;
    User student;
    Exam exam;
    ExamAttempt attempt;

    @BeforeEach
    void setup() {
        service = new StudentExamLifecycleService(exams, attempts, answers, examQuestions, questions, existing);
        student = mock(User.class);
        lenient().when(student.getId()).thenReturn(10);
        lenient().when(student.getRole()).thenReturn("student");
        exam = mock(Exam.class);
        lenient().when(exam.getId()).thenReturn(100);
        attempt = new ExamAttempt(exam, student, 1, LocalDateTime.now(Clock.systemUTC()).minusMinutes(1), LocalDateTime.now(Clock.systemUTC()).plusMinutes(10));
        attempt.setId(50);
    }

    @Test
    void refusesOtherStudentsAttempt() {
        User stranger = mock(User.class);
        when(stranger.getId()).thenReturn(20);
        when(stranger.getRole()).thenReturn("student");
        when(attempts.findLocked(50)).thenReturn(Optional.of(attempt));
        assertThrows(AccessDeniedException.class, () -> service.attempt(stranger, 50));
    }

    @Test
    void savesAnswerOnlyForAssignedQuestion() {
        when(attempts.findLocked(50)).thenReturn(Optional.of(attempt));
        when(examQuestions.existsByExamIdAndQuestionId(100, 4)).thenReturn(true);
        Question question = mock(Question.class);
        when(question.getId()).thenReturn(4);
        when(questions.findById(4)).thenReturn(Optional.of(question));
        when(answers.findAnswer(50, 4)).thenReturn(Optional.empty());
        var saved = service.save(student, 50, new StudentExamAnswerRequest(4, "B"));
        assertEquals("B", saved.answerText());
        verify(answers).save(any(ExamAttemptAnswer.class));
    }

    @Test
    void cannotSaveUnknownQuestion() {
        when(attempts.findLocked(50)).thenReturn(Optional.of(attempt));
        assertThrows(ResponseStatusException.class,
                () -> service.save(student, 50, new StudentExamAnswerRequest(999, "X")));
        verify(answers, never()).save(any());
    }

    @Test
    void submitsOnlyOnce() {
        when(attempts.findLocked(50)).thenReturn(Optional.of(attempt));
        when(answers.findAnswers(50)).thenReturn(List.of());
        service.submit(student, 50);
        service.submit(student, 50);
        assertEquals("SUBMITTED", attempt.getStatus());
        verify(attempts, times(1)).save(attempt);
    }

    @Test
    void timedOutAttemptIsExpiredAndRejectsLateSave() {
        attempt.setExpiresAt(LocalDateTime.now(Clock.systemUTC()).minusMinutes(1));
        when(attempts.findLocked(50)).thenReturn(Optional.of(attempt));
        assertThrows(ResponseStatusException.class,
                () -> service.save(student, 50, new StudentExamAnswerRequest(4, "late")));
        assertEquals("SUBMITTED", attempt.getStatus());
        verify(answers, never()).save(any());
    }

    @Test
    void hidesUnpublishedResults() {
        when(attempts.findById(50)).thenReturn(Optional.of(attempt));
        assertThrows(ResponseStatusException.class, () -> service.result(student, 50));
    }
    @Test
    void showsPublishedGradedResultOnlyToOwner() {
        when(attempts.findById(50)).thenReturn(Optional.of(attempt));
        attempt.setStatus("GRADED");
        attempt.setIsPublished(true);
        attempt.setObtainedMarks(new java.math.BigDecimal("7.0"));
        attempt.setTotalMarks(new java.math.BigDecimal("10.0"));
        when(exam.getResultsPublished()).thenReturn(true);
        when(exam.getResultsWithheld()).thenReturn(false);
        when(examQuestions.findByExamIdOrderByOrderIndexAsc(100)).thenReturn(List.of());
        when(answers.findAnswers(50)).thenReturn(List.of());
        var result = service.result(student, 50);
        assertEquals(new java.math.BigDecimal("7.0"), result.obtainedMarks());
        assertEquals(100, result.examId());
    }

    @Test
    void rejectsUnsupportedExamFilter() {
        assertThrows(ResponseStatusException.class, () -> service.list(student, "UNKNOWN"));
    }

}
