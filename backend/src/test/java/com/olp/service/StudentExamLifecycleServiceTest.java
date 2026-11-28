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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
        service.setClock(Clock.fixed(Instant.parse("2026-11-28T10:00:00Z"), ZoneOffset.UTC));
        student = mock(User.class);
        lenient().when(student.getId()).thenReturn(10);
        lenient().when(student.getRole()).thenReturn("student");
        exam = mock(Exam.class);
        lenient().when(exam.getId()).thenReturn(100);
        LocalDateTime utcNow = LocalDateTime.of(2026, 11, 28, 10, 0, 0);
        attempt = new ExamAttempt(exam, student, 1, utcNow.minusMinutes(1), utcNow.plusMinutes(10));
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
        attempt.setExpiresAt(LocalDateTime.of(2026, 11, 28, 9, 59, 0));
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

    @Test
    void resultHandlesNullExamQuestionMarksGracefully() {
        when(attempts.findById(50)).thenReturn(Optional.of(attempt));
        attempt.setStatus("GRADED");
        attempt.setIsPublished(true);
        attempt.setObtainedMarks(new BigDecimal("5.0"));
        attempt.setTotalMarks(new BigDecimal("10.0"));
        when(exam.getResultsPublished()).thenReturn(true);
        when(exam.getResultsWithheld()).thenReturn(false);

        Question q = mock(Question.class);
        when(q.getId()).thenReturn(7);
        ExamQuestion eq = new ExamQuestion();
        eq.setQuestion(q);
        eq.setMarks(null);
        when(examQuestions.findByExamIdOrderByOrderIndexAsc(100)).thenReturn(List.of(eq));

        ExamAttemptAnswer ans = new ExamAttemptAnswer(attempt, q, "ans", LocalDateTime.of(2026, 11, 28, 10, 0));
        ans.setMarksAwarded(new BigDecimal("5.0"));
        when(answers.findAnswers(50)).thenReturn(List.of(ans));

        var result = service.result(student, 50);
        assertEquals(1, result.questions().size());
        assertNull(result.questions().get(0).maximumMarks());
        assertEquals(new BigDecimal("5.0"), result.questions().get(0).obtainedMarks());
    }

    @Test
    void randomizedQuestionOrderBlocksUnassignedQuestion() {
        attempt.setQuestionOrder("1, 2, 3");
        when(attempts.findLocked(50)).thenReturn(Optional.of(attempt));

        assertThrows(ResponseStatusException.class,
                () -> service.save(student, 50, new StudentExamAnswerRequest(9, "Answer")));
    }

    @Test
    void expireAllDueUsesInjectedUtcClock() {
        LocalDateTime expectedUtc = LocalDateTime.of(2026, 11, 28, 10, 0, 0);
        when(attempts.submitExpired(expectedUtc)).thenReturn(3);
        assertEquals(3, service.expireAllDue());
        verify(attempts).submitExpired(expectedUtc);
    }
}
