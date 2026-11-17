package com.olp.service;

import com.olp.dto.*;
import com.olp.dto.StudentExamDtos.*;
import com.olp.entity.*;
import com.olp.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Student-facing lifecycle: authenticated ownership, server clock, durable autosaves and publication gate. */
@Service
public class StudentExamLifecycleService {
    private final StudentExamReadRepository exams;
    private final StudentExamAttemptReadRepository attempts;
    private final ExamAttemptAnswerRepository answers;
    private final ExamQuestionRepository examQuestions;
    private final QuestionRepository questions;
    private final ExamService existingExamService;
    private final Clock clock = Clock.systemUTC();

    public StudentExamLifecycleService(StudentExamReadRepository exams,
            StudentExamAttemptReadRepository attempts,
            ExamAttemptAnswerRepository answers, ExamQuestionRepository examQuestions,
            QuestionRepository questions, ExamService existingExamService) {
        this.exams = exams; this.attempts = attempts; this.answers = answers;
        this.examQuestions = examQuestions; this.questions = questions;
        this.existingExamService = existingExamService;
    }

    public static void requireStudent(User user) {
        if (user == null || !"student".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Student account required");
        }
    }

    @Transactional(readOnly = true)
    public List<ExamListItem> list(User student, String state) {
        requireStudent(student);
        String filter = state == null ? "ALL" : state.toUpperCase(Locale.ROOT);
        if (!Set.of("ALL", "UPCOMING", "ONGOING", "COMPLETED").contains(filter))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown exam state");
        LocalDateTime now = LocalDateTime.now(clock);
        return exams.findEnrolledExams(student.getId()).stream().filter(e ->
                !"DRAFT".equalsIgnoreCase(e.getStatus()) && !"CANCELLED".equalsIgnoreCase(e.getStatus()))
            .map(e -> new ExamListItem(e.getId(), e.getCourse().getId(), e.getTitle(), state(e, now),
                e.getInstructions(), e.getStartWindow(), e.getEndWindow(), e.getDurationMinutes(),
                e.getMaxMarks(), e.getMinProgressPercentage(), e.getRequirePrerequisites(), e.getRequireEnrollment()))
            .filter(e -> "ALL".equals(filter) || filter.equals(e.state()))
            .toList();
    }

    private static String state(Exam exam, LocalDateTime now) {
        if (now.isBefore(exam.getStartWindow())) return "UPCOMING";
        if (now.isAfter(exam.getEndWindow()) || now.isEqual(exam.getEndWindow())) return "COMPLETED";
        return "ONGOING";
    }

    public ExamEligibilityResponse eligibility(User student, Integer examId) {
        requireStudent(student);
        return existingExamService.checkEligibility(student, examId);
    }

    public ExamStartResponse start(User student, Integer examId) {
        requireStudent(student);
        return existingExamService.startExam(student, examId);
    }

    @Transactional
    public AttemptState attempt(User student, Integer attemptId) {
        ExamAttempt a = ownedLocked(student, attemptId);
        expireIfNeeded(a, LocalDateTime.now(clock));
        return toState(a);
    }

    @Transactional
    public SavedAnswer save(User student, Integer attemptId, StudentExamAnswerRequest request) {
        ExamAttempt a = ownedLocked(student, attemptId);
        LocalDateTime now = LocalDateTime.now(clock);
        expireIfNeeded(a, now);
        if (!"IN_PROGRESS".equalsIgnoreCase(a.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Examination is no longer active");
        if (request == null || request.questionId() == null || request.answerText() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question and answer required");
        if (request.answerText().length() > 20000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Answer exceeds size limit");
        Integer examId = a.getExam().getId();
        // The immutable per-attempt question list must be enforced for randomized exams.
        // Unrandomized exams use the instructor's exam_question mapping.
        if (a.getQuestionOrder() != null && !a.getQuestionOrder().isBlank()) {
            boolean present = Arrays.stream(a.getQuestionOrder().split(","))
                    .map(String::trim).anyMatch(id -> id.equals(String.valueOf(request.questionId())));
            if (!present) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question not assigned to attempt");
        } else if (!examQuestions.existsByExamIdAndQuestionId(examId, request.questionId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question not assigned to exam");
        }
        Question q = questions.findById(request.questionId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        ExamAttemptAnswer answer = answers.findAnswer(attemptId, request.questionId())
            .orElseGet(() -> new ExamAttemptAnswer(a, q, null, now));
        answer.setAnswerText(request.answerText());
        answer.setSavedAt(now);
        answers.save(answer);
        return new SavedAnswer(q.getId(), answer.getAnswerText(), now);
    }

    @Transactional
    public AttemptState submit(User student, Integer attemptId) {
        ExamAttempt a = ownedLocked(student, attemptId);
        LocalDateTime now = LocalDateTime.now(clock);
        expireIfNeeded(a, now);
        if ("IN_PROGRESS".equalsIgnoreCase(a.getStatus())) {
            a.setStatus("SUBMITTED");
            a.setSubmittedAt(now);
            attempts.save(a);
        } else if (!"SUBMITTED".equalsIgnoreCase(a.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Attempt cannot be submitted in this state");
        }
        return toState(a);
    }

    @Transactional(readOnly = true)
    public ExamResult result(User student, Integer attemptId) {
        ExamAttempt a = owned(student, attemptId);
        Exam exam = a.getExam();
        if (!Boolean.TRUE.equals(exam.getResultsPublished()) || Boolean.TRUE.equals(exam.getResultsWithheld())
                || !Boolean.TRUE.equals(a.getIsPublished()) || !"GRADED".equalsIgnoreCase(a.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Result not graded and published");
        }
        Map<Integer, java.math.BigDecimal> maxima = examQuestions.findByExamIdOrderByOrderIndexAsc(exam.getId())
            .stream().collect(Collectors.toMap(e -> e.getQuestion().getId(), ExamQuestion::getMarks, (x,y) -> x));
        List<QuestionResult> detail = answers.findAnswers(attemptId).stream()
            .map(ans -> new QuestionResult(ans.getQuestion().getId(), maxima.get(ans.getQuestion().getId()),
                    ans.getMarksAwarded(), ans.getFeedback())).toList();
        return new ExamResult(a.getId(), exam.getId(), a.getStatus(), a.getObtainedMarks(),
                a.getTotalMarks(), a.getPublishedAt(), detail.stream().anyMatch(d -> d.obtainedMarks() != null || d.feedback() != null), detail);
    }

    // Read-only result requests use owned(); updates use ownedLocked() to serialize writes.
    private ExamAttempt owned(User student, Integer attemptId) {
        requireStudent(student);
        ExamAttempt a = attempts.findById(attemptId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found"));
        if (!a.getStudent().getId().equals(student.getId()))
            throw new AccessDeniedException("Cannot access another student's attempt");
        return a;
    }

    private ExamAttempt ownedLocked(User student, Integer attemptId) {
        requireStudent(student);
        ExamAttempt a = attempts.findLocked(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found"));
        if (!a.getStudent().getId().equals(student.getId()))
            throw new AccessDeniedException("Cannot access another student's attempt");
        return a;
    }

    private void expireIfNeeded(ExamAttempt a, LocalDateTime now) {
        if ("IN_PROGRESS".equalsIgnoreCase(a.getStatus()) && !now.isBefore(a.getExpiresAt())) {
            a.setStatus("SUBMITTED");
            a.setSubmittedAt(a.getExpiresAt());
            attempts.save(a);
        }
    }

    private AttemptState toState(ExamAttempt a) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<SavedAnswer> saved = answers.findAnswers(a.getId()).stream()
                .map(x -> new SavedAnswer(x.getQuestion().getId(), x.getAnswerText(), x.getSavedAt())).toList();
        return new AttemptState(a.getId(), a.getExam().getId(), a.getStatus(), now,
                a.getExpiresAt(), "IN_PROGRESS".equalsIgnoreCase(a.getStatus()) ? a.getRemainingSeconds(now) : 0, saved);
    }

    /** Atomic database-side automatic submission, independent of connected browsers. */
    @Transactional
    public int expireAllDue() {
        return attempts.submitExpired(LocalDateTime.now(clock));
    }
}
