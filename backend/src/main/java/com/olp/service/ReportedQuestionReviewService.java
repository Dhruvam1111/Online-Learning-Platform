package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.event.ReportedQuestionReviewedEvent;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for instructor review of reported questions (FR-I36, Step 53).
 * Enables filtering reports by assessment and status, inspecting student report histories,
 * previewing affected student attempt counts, and executing correct/invalidate/dismiss actions.
 */
@Service
public class ReportedQuestionReviewService {

    private final ReportedQuestionRepository reportedQuestionRepository;
    private final StudentQuestionReportRepository studentQuestionReportRepository;
    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final QuizRepository quizRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private Clock clock = Clock.systemDefaultZone();

    @Autowired
    public ReportedQuestionReviewService(ReportedQuestionRepository reportedQuestionRepository,
                                         StudentQuestionReportRepository studentQuestionReportRepository,
                                         QuestionRepository questionRepository,
                                         ExamRepository examRepository,
                                         QuizRepository quizRepository,
                                         ExamAttemptRepository examAttemptRepository,
                                         QuizAttemptRepository quizAttemptRepository,
                                         CourseRepository courseRepository,
                                         CourseInstructorRepository courseInstructorRepository,
                                         UserRepository userRepository,
                                         ApplicationEventPublisher eventPublisher) {
        this.reportedQuestionRepository = reportedQuestionRepository;
        this.studentQuestionReportRepository = studentQuestionReportRepository;
        this.questionRepository = questionRepository;
        this.examRepository = examRepository;
        this.quizRepository = quizRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.courseRepository = courseRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.userRepository = userRepository;
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
     * List reported questions filtered by course, assessment type, assessment ID, and status (FR-I36, Step 53).
     */
    @Transactional(readOnly = true)
    public List<ReportedQuestionInboxResponse> getReportedQuestions(User instructor,
                                                                   Integer courseId,
                                                                   AssessmentType assessmentType,
                                                                   Integer assessmentId,
                                                                   ReportedQuestionStatus status) {
        if (instructor == null) {
            throw new InvalidCredentialsException("Instructor must be authenticated");
        }

        boolean isAdmin = "ADMIN".equalsIgnoreCase(instructor.getRole());

        if (courseId != null) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
            if (!isAdmin && !isInstructorOfCourse(instructor, course)) {
                throw new AccessDeniedException("You are not authorized to view reported questions for course: " + courseId);
            }
        }

        List<ReportedQuestion> list;
        if (isAdmin) {
            list = reportedQuestionRepository.findWithFiltersAdmin(courseId, assessmentType, assessmentId, status);
        } else {
            Set<Integer> accessibleIds = getAccessibleCourseIds(instructor);
            if (accessibleIds.isEmpty()) {
                return Collections.emptyList();
            }
            if (courseId != null && !accessibleIds.contains(courseId)) {
                return Collections.emptyList();
            }
            list = reportedQuestionRepository.findWithFilters(accessibleIds, courseId, assessmentType, assessmentId, status);
        }

        return list.stream().map(this::toInboxResponse).collect(Collectors.toList());
    }

    /**
     * Get detailed information for a reported question including all student reports and affected attempts (FR-I36, Step 53).
     */
    @Transactional(readOnly = true)
    public ReportedQuestionDetailResponse getReportDetails(User instructor, Integer reportedQuestionId) {
        if (instructor == null) {
            throw new InvalidCredentialsException("Instructor must be authenticated");
        }

        ReportedQuestion rq = reportedQuestionRepository.findById(reportedQuestionId)
                .orElseThrow(() -> new ResourceNotFoundException("Reported question not found with id: " + reportedQuestionId));

        if (!isInstructorOfCourse(instructor, rq.getCourse())) {
            throw new AccessDeniedException("You are not authorized to view reported question: " + reportedQuestionId);
        }

        return toDetailResponse(rq);
    }

    /**
     * Return count of student attempts that would be affected by modifying or invalidating the reported question (FR-I36, Step 53).
     */
    @Transactional(readOnly = true)
    public AffectedAttemptsCountResponse getAffectedAttemptsCount(User instructor, Integer reportedQuestionId) {
        if (instructor == null) {
            throw new InvalidCredentialsException("Instructor must be authenticated");
        }

        ReportedQuestion rq = reportedQuestionRepository.findById(reportedQuestionId)
                .orElseThrow(() -> new ResourceNotFoundException("Reported question not found with id: " + reportedQuestionId));

        if (!isInstructorOfCourse(instructor, rq.getCourse())) {
            throw new AccessDeniedException("You are not authorized to view reported question: " + reportedQuestionId);
        }

        long affectedCount = calculateAffectedAttempts(rq.getAssessmentType(), rq.getAssessmentId(), rq.getQuestionId());
        return new AffectedAttemptsCountResponse(rq.getId(), rq.getAssessmentType(), rq.getAssessmentId(), rq.getQuestionId(), affectedCount);
    }

    /**
     * Execute review action on a reported question: correct, invalidate, or dismiss, with mandatory comment (FR-I36, Step 53).
     */
    @Transactional
    public ReportedQuestionDetailResponse reviewReportedQuestion(User instructor,
                                                                 Integer reportedQuestionId,
                                                                 ReportedQuestionReviewRequest request) {
        if (instructor == null) {
            throw new InvalidCredentialsException("Instructor must be authenticated");
        }
        if (request == null || request.getAction() == null) {
            throw new IllegalArgumentException("Review action is required (CORRECT, INVALIDATE, DISMISS)");
        }
        if (request.getComment() == null || request.getComment().trim().isEmpty()) {
            throw new IllegalArgumentException("Review comment is mandatory");
        }

        ReportedQuestion rq = reportedQuestionRepository.findById(reportedQuestionId)
                .orElseThrow(() -> new ResourceNotFoundException("Reported question not found with id: " + reportedQuestionId));

        if (!isInstructorOfCourse(instructor, rq.getCourse())) {
            throw new AccessDeniedException("You are not authorized to review reported question: " + reportedQuestionId);
        }

        LocalDateTime now = LocalDateTime.now(clock);

        ReportedQuestionStatus newStatus;
        switch (request.getAction()) {
            case CORRECT -> newStatus = ReportedQuestionStatus.RESOLVED_CORRECTED;
            case INVALIDATE -> newStatus = ReportedQuestionStatus.RESOLVED_INVALIDATED;
            case DISMISS -> newStatus = ReportedQuestionStatus.DISMISSED;
            default -> throw new IllegalArgumentException("Unsupported review action: " + request.getAction());
        }

        rq.setStatus(newStatus);
        rq.setResolutionAction(request.getAction().name());
        rq.setResolutionComment(request.getComment().trim());
        rq.setResolvedBy(instructor);
        rq.setResolvedAt(now);
        rq.setUpdatedAt(now);

        ReportedQuestion saved = reportedQuestionRepository.save(rq);

        long affectedCount = calculateAffectedAttempts(saved.getAssessmentType(), saved.getAssessmentId(), saved.getQuestionId());

        eventPublisher.publishEvent(new ReportedQuestionReviewedEvent(
                this,
                saved.getId(),
                saved.getAssessmentType(),
                saved.getAssessmentId(),
                saved.getQuestionId(),
                saved.getCourse().getId(),
                request.getAction().name(),
                request.getComment().trim(),
                instructor.getId(),
                now,
                affectedCount
        ));

        return toDetailResponse(saved);
    }

    public long calculateAffectedAttempts(AssessmentType assessmentType, Integer assessmentId, Integer questionId) {
        if (assessmentType == AssessmentType.EXAM) {
            return examAttemptRepository.countByExamId(assessmentId);
        } else if (assessmentType == AssessmentType.QUIZ) {
            return quizAttemptRepository.countByQuizId(assessmentId);
        }
        return 0;
    }

    public boolean isInstructorOfCourse(User user, Course course) {
        if (user == null || course == null) return false;
        if ("ADMIN".equalsIgnoreCase(user.getRole())) return true;
        if (course.getInstructor() != null && Objects.equals(course.getInstructor().getId(), user.getId())) {
            return true;
        }
        return courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), user.getId());
    }

    public Set<Integer> getAccessibleCourseIds(User user) {
        Set<Integer> ids = new HashSet<>();
        List<Course> direct = courseRepository.findByInstructorId(user.getId());
        for (Course c : direct) {
            ids.add(c.getId());
        }
        List<CourseInstructor> assigned = courseInstructorRepository.findByInstructorId(user.getId());
        for (CourseInstructor ci : assigned) {
            if (ci.getCourse() != null) {
                ids.add(ci.getCourse().getId());
            }
        }
        return ids;
    }

    public ReportedQuestionInboxResponse toInboxResponse(ReportedQuestion rq) {
        String assessmentTitle = getAssessmentTitle(rq.getAssessmentType(), rq.getAssessmentId());
        String questionText = questionRepository.findById(rq.getQuestionId())
                .map(Question::getQuestionText)
                .orElse("Question #" + rq.getQuestionId());

        ReportedQuestionInboxResponse resp = new ReportedQuestionInboxResponse();
        resp.setId(rq.getId());
        resp.setAssessmentType(rq.getAssessmentType());
        resp.setAssessmentId(rq.getAssessmentId());
        resp.setAssessmentTitle(assessmentTitle);
        resp.setQuestionId(rq.getQuestionId());
        resp.setQuestionText(questionText);
        resp.setCourseId(rq.getCourse().getId());
        resp.setCourseTitle(rq.getCourse().getTitle());
        resp.setReportCount(rq.getReportCount());
        resp.setStatus(rq.getStatus());
        resp.setLatestReason(rq.getLatestReason());
        resp.setLatestExplanation(rq.getLatestExplanation());
        resp.setFirstReportedAt(rq.getFirstReportedAt());
        resp.setLastReportedAt(rq.getLastReportedAt());
        resp.setEscalateAt(rq.getEscalateAt());
        resp.setEscalatedToAdmin(rq.getEscalatedToAdmin());
        resp.setEscalatedAt(rq.getEscalatedAt());
        resp.setReviewLink("/instructor/reported-questions/" + rq.getId());
        return resp;
    }

    public ReportedQuestionDetailResponse toDetailResponse(ReportedQuestion rq) {
        ReportedQuestionDetailResponse resp = new ReportedQuestionDetailResponse();
        resp.setId(rq.getId());
        resp.setAssessmentType(rq.getAssessmentType());
        resp.setAssessmentId(rq.getAssessmentId());
        resp.setAssessmentTitle(getAssessmentTitle(rq.getAssessmentType(), rq.getAssessmentId()));
        resp.setQuestionId(rq.getQuestionId());

        questionRepository.findById(rq.getQuestionId()).ifPresent(q -> {
            resp.setQuestionText(q.getQuestionText());
            resp.setOptionsJson(q.getOptionsJson());
            resp.setCorrectAnswer(q.getCorrectAnswer());
        });

        resp.setCourseId(rq.getCourse().getId());
        resp.setCourseTitle(rq.getCourse().getTitle());
        resp.setReportCount(rq.getReportCount());
        resp.setStatus(rq.getStatus());
        resp.setLatestReason(rq.getLatestReason());
        resp.setLatestExplanation(rq.getLatestExplanation());
        resp.setFirstReportedAt(rq.getFirstReportedAt());
        resp.setLastReportedAt(rq.getLastReportedAt());
        resp.setEscalateAt(rq.getEscalateAt());
        resp.setEscalatedToAdmin(rq.getEscalatedToAdmin());
        resp.setEscalatedAt(rq.getEscalatedAt());
        resp.setResolutionAction(rq.getResolutionAction());
        resp.setResolutionComment(rq.getResolutionComment());

        if (rq.getResolvedBy() != null) {
            resp.setResolvedById(rq.getResolvedBy().getId());
            resp.setResolvedByName(rq.getResolvedBy().getName());
        }
        resp.setResolvedAt(rq.getResolvedAt());

        long affectedAttempts = calculateAffectedAttempts(rq.getAssessmentType(), rq.getAssessmentId(), rq.getQuestionId());
        resp.setAffectedAttemptsCount(affectedAttempts);

        List<StudentQuestionReport> studentReports = studentQuestionReportRepository.findByReportedQuestionId(rq.getId());
        List<StudentQuestionReportResponse> reportResponses = studentReports.stream().map(sr -> {
            StudentQuestionReportResponse r = new StudentQuestionReportResponse();
            r.setId(sr.getId());
            if (sr.getStudent() != null) {
                r.setStudentId(sr.getStudent().getId());
                r.setStudentName(sr.getStudent().getName());
                r.setStudentEmail(sr.getStudent().getEmail());
            }
            r.setAttemptId(sr.getAttemptId());
            r.setReason(sr.getReason());
            r.setExplanation(sr.getExplanation());
            r.setReportedAt(sr.getReportedAt());
            return r;
        }).collect(Collectors.toList());

        resp.setReports(reportResponses);
        return resp;
    }

    private String getAssessmentTitle(AssessmentType assessmentType, Integer assessmentId) {
        if (assessmentType == AssessmentType.EXAM) {
            return examRepository.findById(assessmentId)
                    .map(Exam::getTitle)
                    .orElse("Exam #" + assessmentId);
        } else if (assessmentType == AssessmentType.QUIZ) {
            return quizRepository.findById(assessmentId)
                    .map(Quiz::getTitle)
                    .orElse("Quiz #" + assessmentId);
        }
        return assessmentType + " #" + assessmentId;
    }
}
