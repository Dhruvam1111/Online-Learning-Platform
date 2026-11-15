package com.olp.service;

import com.olp.dto.QuestionReportRequest;
import com.olp.dto.ReportedQuestionInboxResponse;
import com.olp.entity.*;
import com.olp.event.QuestionReportNotificationEvent;
import com.olp.event.QuestionReportedEvent;
import com.olp.event.ReportedQuestionEscalatedEvent;
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
 * Service managing student question report ingestion, inbox aggregation,
 * instructor notification dispatch, and 48-hour SLA administrative escalation (FR-I35, Step 52).
 */
@Service
public class ReportedQuestionAlertService {

    private final ReportedQuestionRepository reportedQuestionRepository;
    private final StudentQuestionReportRepository studentQuestionReportRepository;
    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final QuizRepository quizRepository;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private Clock clock = Clock.systemDefaultZone();

    @Autowired
    public ReportedQuestionAlertService(ReportedQuestionRepository reportedQuestionRepository,
                                       StudentQuestionReportRepository studentQuestionReportRepository,
                                       QuestionRepository questionRepository,
                                       ExamRepository examRepository,
                                       QuizRepository quizRepository,
                                       CourseRepository courseRepository,
                                       CourseInstructorRepository courseInstructorRepository,
                                       UserRepository userRepository,
                                       ApplicationEventPublisher eventPublisher) {
        this.reportedQuestionRepository = reportedQuestionRepository;
        this.studentQuestionReportRepository = studentQuestionReportRepository;
        this.questionRepository = questionRepository;
        this.examRepository = examRepository;
        this.quizRepository = quizRepository;
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
     * Ingest a question report from a student, aggregate it into the inbox item,
     * record student report details, and notify course instructors (FR-I35, Step 52).
     */
    @Transactional
    public ReportedQuestionInboxResponse reportQuestion(User student,
                                                        AssessmentType assessmentType,
                                                        Integer assessmentId,
                                                        Integer questionId,
                                                        QuestionReportRequest request) {
        if (student == null) {
            throw new InvalidCredentialsException("Student must be authenticated");
        }
        if (request == null || request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Report reason is required");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        Course course;
        String assessmentTitle;
        if (assessmentType == AssessmentType.EXAM) {
            Exam exam = examRepository.findById(assessmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + assessmentId));
            course = exam.getCourse();
            assessmentTitle = exam.getTitle();
        } else if (assessmentType == AssessmentType.QUIZ) {
            Quiz quiz = quizRepository.findById(assessmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + assessmentId));
            course = quiz.getCourse();
            assessmentTitle = quiz.getTitle();
        } else {
            throw new IllegalArgumentException("Unsupported assessment type: " + assessmentType);
        }

        if (course == null) {
            throw new ResourceNotFoundException("Course association missing for " + assessmentType + " id: " + assessmentId);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Optional<ReportedQuestion> existingOpt = reportedQuestionRepository.findByAssessmentTypeAndAssessmentIdAndQuestionId(
                assessmentType, assessmentId, questionId);

        ReportedQuestion reportedQuestion;
        if (existingOpt.isEmpty()) {
            reportedQuestion = new ReportedQuestion(assessmentType, assessmentId, questionId, course);
            reportedQuestion.setReportCount(1);
            reportedQuestion.setStatus(ReportedQuestionStatus.PENDING);
            reportedQuestion.setLatestReason(request.getReason().trim());
            reportedQuestion.setLatestExplanation(request.getExplanation());
            reportedQuestion.setFirstReportedAt(now);
            reportedQuestion.setLastReportedAt(now);
            reportedQuestion.setEscalateAt(now.plusHours(48));
            reportedQuestion.setEscalatedToAdmin(false);
            reportedQuestion.setCreatedAt(now);
            reportedQuestion.setUpdatedAt(now);
            reportedQuestion = reportedQuestionRepository.save(reportedQuestion);

            StudentQuestionReport sqr = new StudentQuestionReport(
                    reportedQuestion, student, request.getAttemptId(), request.getReason().trim(), request.getExplanation());
            sqr.setReportedAt(now);
            studentQuestionReportRepository.save(sqr);
        } else {
            reportedQuestion = existingOpt.get();
            Optional<StudentQuestionReport> existingStudentReport = studentQuestionReportRepository
                    .findByReportedQuestionIdAndStudentId(reportedQuestion.getId(), student.getId());

            if (existingStudentReport.isPresent()) {
                StudentQuestionReport sqr = existingStudentReport.get();
                sqr.setReason(request.getReason().trim());
                sqr.setExplanation(request.getExplanation());
                if (request.getAttemptId() != null) {
                    sqr.setAttemptId(request.getAttemptId());
                }
                sqr.setReportedAt(now);
                studentQuestionReportRepository.save(sqr);
            } else {
                reportedQuestion.setReportCount(reportedQuestion.getReportCount() + 1);
                StudentQuestionReport sqr = new StudentQuestionReport(
                        reportedQuestion, student, request.getAttemptId(), request.getReason().trim(), request.getExplanation());
                sqr.setReportedAt(now);
                studentQuestionReportRepository.save(sqr);
            }

            reportedQuestion.setLatestReason(request.getReason().trim());
            reportedQuestion.setLatestExplanation(request.getExplanation());
            reportedQuestion.setLastReportedAt(now);
            reportedQuestion.setUpdatedAt(now);

            // If it was previously dismissed, reopen to PENDING so instructors can re-review new reports
            if (reportedQuestion.getStatus() == ReportedQuestionStatus.DISMISSED) {
                reportedQuestion.setStatus(ReportedQuestionStatus.PENDING);
            }

            reportedQuestion = reportedQuestionRepository.save(reportedQuestion);
        }

        // Dispatch domain events
        eventPublisher.publishEvent(new QuestionReportedEvent(
                this,
                reportedQuestion.getId(),
                assessmentType,
                assessmentId,
                questionId,
                course.getId(),
                student.getId(),
                request.getReason().trim(),
                reportedQuestion.getReportCount(),
                now
        ));

        // Dispatch notification events to course instructors
        notifyCourseInstructors(course, reportedQuestion, assessmentTitle);

        return toInboxResponse(reportedQuestion, assessmentTitle, question.getQuestionText());
    }

    /**
     * Retrieve reported questions for an instructor's inbox with optional filters (FR-I35, Step 52).
     */
    @Transactional(readOnly = true)
    public List<ReportedQuestionInboxResponse> getInstructorInbox(User instructor, Integer courseId, ReportedQuestionStatus status) {
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

            List<ReportedQuestion> list = (status != null)
                    ? reportedQuestionRepository.findByCourseIdAndStatus(courseId, status)
                    : reportedQuestionRepository.findByCourseId(courseId);

            return list.stream().map(this::toInboxResponse).collect(Collectors.toList());
        }

        // When courseId is null
        if (isAdmin) {
            List<ReportedQuestion> list = (status != null)
                    ? reportedQuestionRepository.findAllByStatusOrderByLastReportedAtDesc(status)
                    : reportedQuestionRepository.findAllOrderByLastReportedAtDesc();
            return list.stream().map(this::toInboxResponse).collect(Collectors.toList());
        }

        Set<Integer> accessibleIds = getAccessibleCourseIds(instructor);
        if (accessibleIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ReportedQuestion> list = (status != null)
                ? reportedQuestionRepository.findByCourseIdInAndStatus(accessibleIds, status)
                : reportedQuestionRepository.findByCourseIdIn(accessibleIds);

        return list.stream().map(this::toInboxResponse).collect(Collectors.toList());
    }

    /**
     * Check for overdue unresolved reported questions and escalate to admin once SLA expires (FR-I35, FR-A15, Step 52).
     */
    @Transactional
    public int escalateUnresolvedReports() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<ReportedQuestion> overdue = reportedQuestionRepository.findUnresolvedForEscalation(
                now, List.of(ReportedQuestionStatus.PENDING, ReportedQuestionStatus.UNDER_REVIEW));

        for (ReportedQuestion rq : overdue) {
            rq.setEscalatedToAdmin(true);
            rq.setEscalatedAt(now);
            rq.setStatus(ReportedQuestionStatus.ESCALATED);
            rq.setUpdatedAt(now);
            reportedQuestionRepository.save(rq);

            eventPublisher.publishEvent(new ReportedQuestionEscalatedEvent(
                    this,
                    rq.getId(),
                    rq.getAssessmentType(),
                    rq.getAssessmentId(),
                    rq.getQuestionId(),
                    rq.getCourse().getId(),
                    rq.getReportCount(),
                    rq.getFirstReportedAt(),
                    now
            ));
        }

        return overdue.size();
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

    private void notifyCourseInstructors(Course course, ReportedQuestion reportedQuestion, String assessmentTitle) {
        String reviewLink = "/instructor/reported-questions/" + reportedQuestion.getId();
        LocalDateTime now = LocalDateTime.now(clock);

        if (course.getInstructor() != null) {
            eventPublisher.publishEvent(new QuestionReportNotificationEvent(
                    this,
                    course.getInstructor().getId(),
                    reportedQuestion.getId(),
                    course.getId(),
                    course.getTitle(),
                    assessmentTitle,
                    reportedQuestion.getLatestReason(),
                    reviewLink,
                    now
            ));
        }

        List<CourseInstructor> coInstructors = courseInstructorRepository.findByCourseId(course.getId());
        for (CourseInstructor ci : coInstructors) {
            if (ci.getInstructor() != null &&
                    (course.getInstructor() == null || !Objects.equals(ci.getInstructor().getId(), course.getInstructor().getId()))) {
                eventPublisher.publishEvent(new QuestionReportNotificationEvent(
                        this,
                        ci.getInstructor().getId(),
                        reportedQuestion.getId(),
                        course.getId(),
                        course.getTitle(),
                        assessmentTitle,
                        reportedQuestion.getLatestReason(),
                        reviewLink,
                        now
                ));
            }
        }
    }

    public ReportedQuestionInboxResponse toInboxResponse(ReportedQuestion rq) {
        String assessmentTitle = getAssessmentTitle(rq.getAssessmentType(), rq.getAssessmentId());
        String questionText = questionRepository.findById(rq.getQuestionId())
                .map(Question::getQuestionText)
                .orElse("Question #" + rq.getQuestionId());
        return toInboxResponse(rq, assessmentTitle, questionText);
    }

    public ReportedQuestionInboxResponse toInboxResponse(ReportedQuestion rq, String assessmentTitle, String questionText) {
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
