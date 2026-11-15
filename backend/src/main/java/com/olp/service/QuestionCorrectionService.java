package com.olp.service;

import com.olp.dto.ApplyQuestionCorrectionRequest;
import com.olp.dto.ApplyQuestionCorrectionResponse;
import com.olp.dto.AttemptCorrectionSummaryResponse;
import com.olp.dto.CompletionEvaluationResult;
import com.olp.entity.*;
import com.olp.event.QuestionCorrectionAppliedEvent;
import com.olp.event.StudentScoreCorrectedEvent;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for applying approved question corrections to results across student attempts (FR-I37, Step 54).
 * Recomputes marks according to FR-I66 question correction policies, audits all changes via immutable ResultVersion records,
 * recalculates certificate eligibility, ensures strict idempotency (cannot be applied twice), and notifies learners.
 */
@Service
public class QuestionCorrectionService {

    private final ReportedQuestionRepository reportedQuestionRepository;
    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final QuizRepository quizRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAttemptAnswerRepository quizAttemptAnswerRepository;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final UserRepository userRepository;
    private final ResultVersionRepository resultVersionRepository;
    private final CourseCompletionService courseCompletionService;
    private final ApplicationEventPublisher eventPublisher;
    private Clock clock = Clock.systemDefaultZone();

    @Autowired
    public QuestionCorrectionService(ReportedQuestionRepository reportedQuestionRepository,
                                    QuestionRepository questionRepository,
                                    ExamRepository examRepository,
                                    QuizRepository quizRepository,
                                    ExamQuestionRepository examQuestionRepository,
                                    QuizQuestionRepository quizQuestionRepository,
                                    ExamAttemptRepository examAttemptRepository,
                                    QuizAttemptRepository quizAttemptRepository,
                                    QuizAttemptAnswerRepository quizAttemptAnswerRepository,
                                    CourseRepository courseRepository,
                                    CourseInstructorRepository courseInstructorRepository,
                                    UserRepository userRepository,
                                    ResultVersionRepository resultVersionRepository,
                                    @Autowired(required = false) CourseCompletionService courseCompletionService,
                                    ApplicationEventPublisher eventPublisher) {
        this.reportedQuestionRepository = reportedQuestionRepository;
        this.questionRepository = questionRepository;
        this.examRepository = examRepository;
        this.quizRepository = quizRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizAttemptAnswerRepository = quizAttemptAnswerRepository;
        this.courseRepository = courseRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.userRepository = userRepository;
        this.resultVersionRepository = resultVersionRepository;
        this.courseCompletionService = courseCompletionService;
        this.eventPublisher = eventPublisher;
    }

    public void setClock(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    public User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        if (principal instanceof String email) {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new InvalidCredentialsException("User not found: " + email));
        }
        if (authentication.getName() != null) {
            return userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new InvalidCredentialsException("User not found: " + authentication.getName()));
        }
        throw new InvalidCredentialsException("Unable to resolve authenticated user");
    }

    /**
     * Apply approved correction to all student attempts for a reported question (FR-I37, Step 54).
     * Enforces strict idempotency: running twice changes nothing the second time.
     */
    @Transactional
    public ApplyQuestionCorrectionResponse applyCorrection(User instructor,
                                                           Integer reportedQuestionId,
                                                           ApplyQuestionCorrectionRequest request) {
        if (instructor == null) {
            throw new InvalidCredentialsException("Instructor must be authenticated");
        }

        ReportedQuestion rq = reportedQuestionRepository.findById(reportedQuestionId)
                .orElseThrow(() -> new ResourceNotFoundException("Reported question not found with id: " + reportedQuestionId));

        Course course = rq.getCourse();
        if (!isInstructorOfCourse(instructor, course)) {
            throw new AccessDeniedException("You are not authorized to apply corrections for course: " + course.getId());
        }

        // Idempotency check: if already applied, return immediately with no further changes
        if (Boolean.TRUE.equals(rq.getCorrectionApplied())) {
            ApplyQuestionCorrectionResponse response = new ApplyQuestionCorrectionResponse();
            response.setReportedQuestionId(rq.getId());
            response.setAssessmentType(rq.getAssessmentType());
            response.setAssessmentId(rq.getAssessmentId());
            response.setQuestionId(rq.getQuestionId());
            response.setCourseId(course.getId());
            response.setPolicy(QuestionCorrectionPolicy.fromString(rq.getCorrectionPolicy()));
            response.setAlreadyApplied(true);
            response.setAffectedAttemptsCount(rq.getAffectedAttemptsCount() != null ? rq.getAffectedAttemptsCount() : 0);
            response.setAppliedAt(rq.getCorrectionAppliedAt());
            if (rq.getCorrectionAppliedBy() != null) {
                response.setAppliedById(rq.getCorrectionAppliedBy().getId());
                response.setAppliedByName(rq.getCorrectionAppliedBy().getName());
            }
            response.setMessage("Correction has already been applied for this reported question. No duplicate changes made.");
            response.setAttemptResults(Collections.emptyList());
            return response;
        }

        LocalDateTime now = LocalDateTime.now(clock);

        // Determine question correction policy (FR-I66, Step 41)
        QuestionCorrectionPolicy policy;
        String assessmentTitle;
        BigDecimal questionMarks = BigDecimal.ONE;

        Question question = questionRepository.findById(rq.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + rq.getQuestionId()));
        if (question.getDefaultMarks() != null && question.getDefaultMarks().compareTo(BigDecimal.ZERO) > 0) {
            questionMarks = question.getDefaultMarks();
        }

        if (rq.getAssessmentType() == AssessmentType.EXAM) {
            Exam exam = examRepository.findById(rq.getAssessmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + rq.getAssessmentId()));
            assessmentTitle = exam.getTitle();
            policy = (request != null && request.getPolicy() != null)
                    ? request.getPolicy()
                    : (exam.getQuestionCorrectionPolicy() != null ? exam.getQuestionCorrectionPolicy() : QuestionCorrectionPolicy.AWARD_FULL_MARKS);

            Optional<ExamQuestion> examQuestionOpt = examQuestionRepository.findByExamIdAndQuestionId(exam.getId(), question.getId());
            if (examQuestionOpt.isPresent() && examQuestionOpt.get().getMarks() != null) {
                questionMarks = examQuestionOpt.get().getMarks();
            }
        } else if (rq.getAssessmentType() == AssessmentType.QUIZ) {
            Quiz quiz = quizRepository.findById(rq.getAssessmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + rq.getAssessmentId()));
            assessmentTitle = quiz.getTitle();
            policy = (request != null && request.getPolicy() != null)
                    ? request.getPolicy()
                    : QuestionCorrectionPolicy.AWARD_FULL_MARKS;

            Optional<QuizQuestion> quizQuestionOpt = quizQuestionRepository.findByQuizIdAndQuestionId(quiz.getId(), question.getId());
            if (quizQuestionOpt.isPresent() && quizQuestionOpt.get().getMarks() != null) {
                questionMarks = quizQuestionOpt.get().getMarks();
            }
        } else {
            throw new IllegalArgumentException("Unsupported assessment type: " + rq.getAssessmentType());
        }

        String justification = (request != null && request.getJustification() != null && !request.getJustification().trim().isEmpty())
                ? request.getJustification().trim()
                : "Approved correction for question #" + rq.getQuestionId() + " (" + policy.name() + ")";

        List<AttemptCorrectionSummaryResponse> summaries = new ArrayList<>();

        if (rq.getAssessmentType() == AssessmentType.EXAM) {
            List<ExamAttempt> attempts = examAttemptRepository.findByExamId(rq.getAssessmentId());
            for (ExamAttempt attempt : attempts) {
                // Only modify finished or submitted attempts
                if ("IN_PROGRESS".equalsIgnoreCase(attempt.getStatus())) {
                    continue;
                }

                BigDecimal oldScore = attempt.getObtainedMarks() != null ? attempt.getObtainedMarks() : BigDecimal.ZERO;
                BigDecimal totalMarks = attempt.getTotalMarks() != null ? attempt.getTotalMarks() : BigDecimal.valueOf(100);
                BigDecimal newScore = calculateNewScore(oldScore, totalMarks, questionMarks, policy);

                attempt.setObtainedMarks(newScore);
                if (policy == QuestionCorrectionPolicy.DROP_AND_RESCALE) {
                    BigDecimal newTotal = totalMarks.subtract(questionMarks);
                    if (newTotal.compareTo(BigDecimal.ZERO) > 0) {
                        attempt.setTotalMarks(newTotal);
                    }
                }
                examAttemptRepository.save(attempt);

                // Audit: Record immutable ResultVersion (FR-I74, Step 49)
                ResultVersion rv = new ResultVersion(
                        course,
                        rq.getAssessmentType(),
                        rq.getAssessmentId(),
                        assessmentTitle,
                        attempt.getStudent(),
                        attempt.getId(),
                        oldScore,
                        newScore,
                        oldScore.toPlainString(),
                        newScore.toPlainString(),
                        justification,
                        instructor
                );
                resultVersionRepository.save(rv);

                // Recalculate certificate eligibility (FR-I56, Step 44)
                boolean certificateEligible = checkCertificateEligibility(attempt.getStudent(), course);

                summaries.add(new AttemptCorrectionSummaryResponse(
                        attempt.getId(),
                        attempt.getStudent().getId(),
                        attempt.getStudent().getName(),
                        oldScore,
                        newScore,
                        certificateEligible
                ));

                // Dispatch individual student notification event
                eventPublisher.publishEvent(new StudentScoreCorrectedEvent(
                        this,
                        attempt.getStudent().getId(),
                        course.getId(),
                        rq.getAssessmentType(),
                        rq.getAssessmentId(),
                        assessmentTitle,
                        attempt.getId(),
                        oldScore,
                        newScore,
                        policy,
                        certificateEligible,
                        now
                ));
            }
        } else if (rq.getAssessmentType() == AssessmentType.QUIZ) {
            List<QuizAttempt> attempts = quizAttemptRepository.findByQuizId(rq.getAssessmentId());
            for (QuizAttempt attempt : attempts) {
                if (attempt.getStatus() == QuizAttemptStatus.IN_PROGRESS) {
                    continue;
                }

                BigDecimal oldScore = attempt.getObtainedMarks() != null ? attempt.getObtainedMarks() : BigDecimal.ZERO;
                BigDecimal totalMarks = BigDecimal.valueOf(100); // Standard quiz baseline
                BigDecimal newScore = calculateNewScore(oldScore, totalMarks, questionMarks, policy);

                attempt.setObtainedMarks(newScore);
                quizAttemptRepository.save(attempt);

                // Also update individual QuizAttemptAnswer if present
                Optional<QuizAttemptAnswer> answerOpt = quizAttemptAnswerRepository
                        .findByAttemptIdAndQuestionId(attempt.getId(), rq.getQuestionId());
                if (answerOpt.isPresent()) {
                    QuizAttemptAnswer ans = answerOpt.get();
                    ans.setAwardedMarks(questionMarks);
                    ans.setEvaluationStatus(EvaluationStatus.CORRECT);
                    quizAttemptAnswerRepository.save(ans);
                }

                // Audit: Record immutable ResultVersion
                ResultVersion rv = new ResultVersion(
                        course,
                        rq.getAssessmentType(),
                        rq.getAssessmentId(),
                        assessmentTitle,
                        attempt.getStudent(),
                        attempt.getId(),
                        oldScore,
                        newScore,
                        oldScore.toPlainString(),
                        newScore.toPlainString(),
                        justification,
                        instructor
                );
                resultVersionRepository.save(rv);

                boolean certificateEligible = checkCertificateEligibility(attempt.getStudent(), course);

                summaries.add(new AttemptCorrectionSummaryResponse(
                        attempt.getId(),
                        attempt.getStudent().getId(),
                        attempt.getStudent().getName(),
                        oldScore,
                        newScore,
                        certificateEligible
                ));

                eventPublisher.publishEvent(new StudentScoreCorrectedEvent(
                        this,
                        attempt.getStudent().getId(),
                        course.getId(),
                        rq.getAssessmentType(),
                        rq.getAssessmentId(),
                        assessmentTitle,
                        attempt.getId(),
                        oldScore,
                        newScore,
                        policy,
                        certificateEligible,
                        now
                ));
            }
        }

        // Mark ReportedQuestion as applied for strict idempotency
        rq.setCorrectionApplied(true);
        rq.setCorrectionAppliedAt(now);
        rq.setCorrectionAppliedBy(instructor);
        rq.setCorrectionPolicy(policy.name());
        rq.setAffectedAttemptsCount(summaries.size());
        rq.setStatus(ReportedQuestionStatus.RESOLVED_CORRECTED);
        rq.setUpdatedAt(now);
        reportedQuestionRepository.save(rq);

        // Dispatch domain batch completion event
        eventPublisher.publishEvent(new QuestionCorrectionAppliedEvent(
                this,
                rq.getId(),
                rq.getAssessmentType(),
                rq.getAssessmentId(),
                rq.getQuestionId(),
                course.getId(),
                policy,
                summaries.size(),
                instructor.getId(),
                now
        ));

        ApplyQuestionCorrectionResponse response = new ApplyQuestionCorrectionResponse();
        response.setReportedQuestionId(rq.getId());
        response.setAssessmentType(rq.getAssessmentType());
        response.setAssessmentId(rq.getAssessmentId());
        response.setQuestionId(rq.getQuestionId());
        response.setCourseId(course.getId());
        response.setPolicy(policy);
        response.setAlreadyApplied(false);
        response.setAffectedAttemptsCount(summaries.size());
        response.setAppliedAt(now);
        response.setAppliedById(instructor.getId());
        response.setAppliedByName(instructor.getName());
        response.setMessage("Question correction successfully applied across " + summaries.size() + " attempt(s).");
        response.setAttemptResults(summaries);
        return response;
    }

    private BigDecimal calculateNewScore(BigDecimal oldScore, BigDecimal totalMarks, BigDecimal questionMarks, QuestionCorrectionPolicy policy) {
        if (policy == QuestionCorrectionPolicy.AWARD_FULL_MARKS) {
            BigDecimal newScore = oldScore.add(questionMarks);
            if (totalMarks != null && totalMarks.compareTo(BigDecimal.ZERO) > 0 && newScore.compareTo(totalMarks) > 0) {
                return totalMarks;
            }
            return newScore;
        } else if (policy == QuestionCorrectionPolicy.DROP_AND_RESCALE) {
            BigDecimal newTotal = totalMarks.subtract(questionMarks);
            if (newTotal.compareTo(BigDecimal.ZERO) <= 0) {
                return oldScore;
            }
            return oldScore.multiply(totalMarks).divide(newTotal, 2, RoundingMode.HALF_UP);
        }
        return oldScore;
    }

    private boolean checkCertificateEligibility(User student, Course course) {
        if (courseCompletionService == null || student == null || course == null) {
            return false;
        }
        try {
            CompletionEvaluationResult res = courseCompletionService.evaluate(student, course);
            return res != null && res.isEligible();
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean isInstructorOfCourse(User user, Course course) {
        if (user == null || course == null) return false;
        if ("ADMIN".equalsIgnoreCase(user.getRole())) return true;
        if (course.getInstructor() != null && Objects.equals(course.getInstructor().getId(), user.getId())) {
            return true;
        }
        return courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), user.getId());
    }
}
