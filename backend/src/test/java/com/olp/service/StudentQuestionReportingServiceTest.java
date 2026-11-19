package com.olp.service;

import com.olp.dto.*;
import com.olp.controller.StudentQuestionReportingController;
import org.springframework.security.access.prepost.PreAuthorize;
import com.olp.entity.*;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentQuestionReportingServiceTest {
    @Mock ReportedQuestionAlertService alertService;
    @Mock StudentQuestionReportStudentRepository reports;
    @Mock ExamAttemptRepository examAttempts;
    @Mock QuizAttemptRepository quizAttempts;
    @Mock ExamQuestionRepository examQuestions;
    @Mock QuizQuestionRepository quizQuestions;
    @InjectMocks StudentQuestionReportingService service;

    User student;

    @BeforeEach void setup() {
        student = new User();
        student.setId(7);
        student.setRole("STUDENT");
    }

    @Test void controllerRoleMatchesJwtAndSecurityConfig() {
        PreAuthorize annotation = StudentQuestionReportingController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertEquals("hasRole('student')", annotation.value());
    }

    @Test void rejectsNonStudent() {
        student.setRole("INSTRUCTOR");
        assertThrows(ResponseStatusException.class, () -> service.history(student));
        verifyNoInteractions(reports);
    }

    @Test void rejectsUnknownReasons() {
        var request = new StudentQuestionReportCreateRequest(AssessmentType.EXAM, 12, 4,
                "unrelated reason", "Please review", 10);
        assertThrows(ResponseStatusException.class, () -> service.submit(student, request));
        verifyNoInteractions(alertService);
    }

    @Test void rejectsReportsForOtherStudentsAttempts() {
        Exam exam = new Exam(); exam.setId(12);
        User otherStudent = new User(); otherStudent.setId(99);
        ExamAttempt attempt = new ExamAttempt(); attempt.setStudent(otherStudent); attempt.setExam(exam);
        when(examQuestions.existsByExamIdAndQuestionId(12, 4)).thenReturn(true);
        when(examAttempts.findById(10)).thenReturn(Optional.of(attempt));
        var request = new StudentQuestionReportCreateRequest(AssessmentType.EXAM, 12, 4,
                "INCORRECT", "Wrong key", 10);
        assertThrows(ResponseStatusException.class, () -> service.submit(student, request));
        verify(alertService, never()).reportQuestion(any(), any(), any(), any(), any());
    }

    @Test void rejectsUnrelatedQuestions() {
        var request = new StudentQuestionReportCreateRequest(AssessmentType.EXAM, 12, 4,
                "INCORRECT", "Wrong key", 10);
        assertThrows(ResponseStatusException.class, () -> service.submit(student, request));
        verifyNoInteractions(alertService);
    }

    @Test void submitsOwnedExamAttemptAndReturnsReport() {
        Exam exam = new Exam(); exam.setId(12);
        ExamAttempt attempt = new ExamAttempt(); attempt.setStudent(student); attempt.setExam(exam);
        var saved = report(1, ReportedQuestionStatus.PENDING);
        when(examQuestions.existsByExamIdAndQuestionId(12, 4)).thenReturn(true);
        when(examAttempts.findById(10)).thenReturn(Optional.of(attempt));
        when(reports.findMine(7)).thenReturn(List.of(saved));
        var request = new StudentQuestionReportCreateRequest(AssessmentType.EXAM, 12, 4,
                "incorrect", "Wrong answer key", 10);
        var response = service.submit(student, request);
        assertEquals(1, response.reportId());
        verify(alertService).reportQuestion(eq(student), eq(AssessmentType.EXAM), eq(12), eq(4),
                argThat(r -> "INCORRECT".equals(r.getReason()) && r.getAttemptId() == 10));
    }

    @Test void submitsOwnedQuizAttemptAndReturnsReport() {
        Quiz quiz = new Quiz(); quiz.setId(12);
        QuizAttempt attempt = new QuizAttempt(); attempt.setStudent(student); attempt.setQuiz(quiz);
        var saved = report(1, ReportedQuestionStatus.PENDING);
        saved.getReportedQuestion().setAssessmentType(AssessmentType.QUIZ);
        when(quizQuestions.existsByQuizIdAndQuestionId(12, 4)).thenReturn(true);
        when(quizAttempts.findById(10)).thenReturn(Optional.of(attempt));
        when(reports.findMine(7)).thenReturn(List.of(saved));
        var request = new StudentQuestionReportCreateRequest(AssessmentType.QUIZ, 12, 4,
                "AMBIGUOUS", "Both answers seem correct", 10);
        assertEquals(1, service.submit(student, request).reportId());
    }

    @Test void showsOnlyOwnHistoryAndResolutions() {
        var pending = report(1, ReportedQuestionStatus.PENDING);
        var resolved = report(2, ReportedQuestionStatus.RESOLVED_CORRECTED);
        when(reports.findMine(7)).thenReturn(List.of(pending, resolved));
        assertEquals(2, service.history(student).size());
        var feed = service.resolutionFeed(student);
        assertEquals(1, feed.size());
        assertEquals(2, feed.get(0).reportId());
    }

    @Test void protectsReportDetailsFromOtherUsers() {
        when(reports.findMineById(7, 99)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.status(student, 99));
    }

    private StudentQuestionReport report(int id, ReportedQuestionStatus status) {
        ReportedQuestion rq = new ReportedQuestion();
        rq.setId(id + 100);
        rq.setAssessmentType(AssessmentType.EXAM);
        rq.setAssessmentId(12);
        rq.setQuestionId(4);
        rq.setStatus(status);
        StudentQuestionReport r = new StudentQuestionReport();
        r.setId(id);
        r.setReportedQuestion(rq);
        r.setStudent(student);
        r.setReason("INCORRECT");
        return r;
    }
}
