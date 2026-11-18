package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/** FR-S41–S45: student reporting, personal status and resolution feed.
 * Instructor reviews, grading adjustments and result-version audits remain owned
 * by the existing ReportedQuestionReviewService/QuestionCorrectionService.
 */
@Service
public class StudentQuestionReportingService {
    private static final Set<String> REASONS = Set.of("AMBIGUOUS", "INCORRECT", "INCOMPLETE", "OTHER");

    private final ReportedQuestionAlertService alertService;
    private final StudentQuestionReportStudentRepository reports;
    private final ExamAttemptRepository examAttempts;
    private final QuizAttemptRepository quizAttempts;
    private final ExamQuestionRepository examQuestions;
    private final QuizQuestionRepository quizQuestions;

    public StudentQuestionReportingService(ReportedQuestionAlertService alertService,
                                           StudentQuestionReportStudentRepository reports,
                                           ExamAttemptRepository examAttempts,
                                           QuizAttemptRepository quizAttempts,
                                           ExamQuestionRepository examQuestions,
                                           QuizQuestionRepository quizQuestions) {
        this.alertService = alertService;
        this.reports = reports;
        this.examAttempts = examAttempts;
        this.quizAttempts = quizAttempts;
        this.examQuestions = examQuestions;
        this.quizQuestions = quizQuestions;
    }

    public User currentStudent(Authentication authentication) {
        User user = alertService.resolveUser(authentication);
        if (user == null || user.getId() == null || !"STUDENT".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student account required");
        }
        return user;
    }

    @Transactional
    public StudentQuestionReportView submit(User student, StudentQuestionReportCreateRequest input) {
        requireStudent(student);
        if (input == null || input.assessmentType() == null || input.assessmentId() == null
                || input.questionId() == null || input.attemptId() == null
                || input.assessmentId() <= 0 || input.questionId() <= 0 || input.attemptId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assessment, question and attempt are required");
        }
        String reason = input.reason() == null ? "" : input.reason().trim().toUpperCase(Locale.ROOT);
        if (!REASONS.contains(reason)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reason must be AMBIGUOUS, INCORRECT, INCOMPLETE or OTHER");
        }
        String explanation = input.explanation() == null ? "" : input.explanation().trim();
        if (explanation.isEmpty() || explanation.length() > 4000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Explanation must be between 1 and 4000 characters");
        }
        boolean questionBelongs;
        boolean ownsAttempt;
        if (input.assessmentType() == AssessmentType.EXAM) {
            questionBelongs = examQuestions.existsByExamIdAndQuestionId(input.assessmentId(), input.questionId());
            ownsAttempt = examAttempts.findById(input.attemptId())
                    .filter(a -> a.getStudent() != null && student.getId().equals(a.getStudent().getId()))
                    .filter(a -> a.getExam() != null && input.assessmentId().equals(a.getExam().getId()))
                    .isPresent();
        } else if (input.assessmentType() == AssessmentType.QUIZ) {
            questionBelongs = quizQuestions.existsByQuizIdAndQuestionId(input.assessmentId(), input.questionId());
            ownsAttempt = quizAttempts.findById(input.attemptId())
                    .filter(a -> a.getStudent() != null && student.getId().equals(a.getStudent().getId()))
                    .filter(a -> a.getQuiz() != null && input.assessmentId().equals(a.getQuiz().getId()))
                    .isPresent();
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only EXAM and QUIZ questions can be reported");
        }
        if (!questionBelongs) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question is not part of this assessment");
        }
        if (!ownsAttempt) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only report questions from your own attempts");
        }
        // Reuse the instructor-facing aggregate and individual report persistence.
        alertService.reportQuestion(student, input.assessmentType(), input.assessmentId(), input.questionId(),
                new QuestionReportRequest(reason, explanation, input.attemptId()));
        // Existing implementation updates the student's report if one already exists.
        return reports.findMine(student.getId()).stream()
                .filter(r -> r.getReportedQuestion().getAssessmentType() == input.assessmentType())
                .filter(r -> input.assessmentId().equals(r.getReportedQuestion().getAssessmentId()))
                .filter(r -> input.questionId().equals(r.getReportedQuestion().getQuestionId()))
                .findFirst().map(this::view)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Report was saved but could not be loaded"));
    }

    @Transactional(readOnly = true)
    public List<StudentQuestionReportView> history(User student) {
        requireStudent(student);
        return reports.findMine(student.getId()).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public StudentQuestionReportView status(User student, Integer reportId) {
        requireStudent(student);
        return reports.findMineById(student.getId(), reportId).map(this::view)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
    }

    /** Pollable resolution feed. No new table or claims of email/push delivery. */
    @Transactional(readOnly = true)
    public List<StudentQuestionReportView> resolutionFeed(User student) {
        return history(student).stream().filter(StudentQuestionReportView::resolved).toList();
    }

    private void requireStudent(User student) {
        if (student == null || student.getId() == null || !"STUDENT".equalsIgnoreCase(student.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student account required");
        }
    }

    private StudentQuestionReportView view(StudentQuestionReport r) {
        ReportedQuestion aggregate = r.getReportedQuestion();
        return new StudentQuestionReportView(
                r.getId(), aggregate.getId(), aggregate.getAssessmentType(),
                aggregate.getAssessmentId(), aggregate.getQuestionId(), r.getAttemptId(),
                r.getReason(), r.getExplanation(), r.getReportedAt(), aggregate.getStatus(),
                aggregate.getResolutionAction(), aggregate.getResolutionComment(), aggregate.getResolvedAt(),
                Boolean.TRUE.equals(aggregate.getCorrectionApplied()), aggregate.getCorrectionAppliedAt());
    }
}
