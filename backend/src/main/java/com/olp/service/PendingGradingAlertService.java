package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing instructor pending grading alerts, workload counters,
 * submission notifications, and daily digests (FR-I33, Step 50).
 */
@Service
public class PendingGradingAlertService {

    private final QuizAttemptAnswerRepository quizAttemptAnswerRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final PendingGradingAlertRepository pendingGradingAlertRepository;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final UserRepository userRepository;
    private final AssignmentService assignmentService;

    @Autowired
    public PendingGradingAlertService(QuizAttemptAnswerRepository quizAttemptAnswerRepository,
                                     AssignmentSubmissionRepository assignmentSubmissionRepository,
                                     PendingGradingAlertRepository pendingGradingAlertRepository,
                                     CourseRepository courseRepository,
                                     CourseInstructorRepository courseInstructorRepository,
                                     UserRepository userRepository,
                                     AssignmentService assignmentService) {
        this.quizAttemptAnswerRepository = quizAttemptAnswerRepository;
        this.assignmentSubmissionRepository = assignmentSubmissionRepository;
        this.pendingGradingAlertRepository = pendingGradingAlertRepository;
        this.courseRepository = courseRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.userRepository = userRepository;
        this.assignmentService = assignmentService;
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
        throw new InvalidCredentialsException("Invalid authentication principal");
    }

    /**
     * Get aggregated pending grading counts for dashboard badge.
     * Shows only courses the instructor is associated with.
     */
    @Transactional(readOnly = true)
    public PendingGradingCountResponse getPendingGradingCount(User currentUser) {
        validateInstructorOrAdmin(currentUser);
        List<Course> courses = resolveAssociatedCourses(currentUser);

        long grandTotalPending = 0;
        long grandTotalDescriptive = 0;
        long grandTotalAssignment = 0;
        List<CoursePendingGradingSummary> summaries = new ArrayList<>();

        for (Course course : courses) {
            long descriptiveCount = quizAttemptAnswerRepository.countPendingDescriptiveByCourseId(course.getId());
            long assignmentCount = assignmentSubmissionRepository.countPendingByCourseId(course.getId());
            long courseTotal = descriptiveCount + assignmentCount;

            grandTotalDescriptive += descriptiveCount;
            grandTotalAssignment += assignmentCount;
            grandTotalPending += courseTotal;

            summaries.add(new CoursePendingGradingSummary(
                    course.getId(),
                    course.getTitle(),
                    courseTotal,
                    descriptiveCount,
                    assignmentCount
            ));
        }

        return new PendingGradingCountResponse(
                grandTotalPending,
                grandTotalDescriptive,
                grandTotalAssignment,
                summaries
        );
    }

    /**
     * Get detailed breakdown containing counts and lists of ungraded descriptive answers and assignments for a course.
     */
    @Transactional(readOnly = true)
    public CoursePendingDetailsResponse getCoursePendingDetails(User currentUser, Integer courseId) {
        validateInstructorOrAdmin(currentUser);
        Course course = verifyCourseAccess(courseId, currentUser);

        List<QuizAttemptAnswer> descriptiveAnswers =
                quizAttemptAnswerRepository.findPendingDescriptiveByCourseId(courseId);
        List<DescriptiveSubmissionResponse> descriptiveDtos = descriptiveAnswers.stream()
                .map(DescriptiveSubmissionResponse::fromEntity)
                .collect(Collectors.toList());

        List<AssignmentSubmission> assignmentSubmissions =
                assignmentSubmissionRepository.findPendingByCourseId(courseId);
        List<AssignmentSubmissionResponse> assignmentDtos = assignmentSubmissions.stream()
                .map(assignmentService::mapToSubmissionResponse)
                .collect(Collectors.toList());

        long descriptiveCount = descriptiveDtos.size();
        long assignmentCount = assignmentDtos.size();
        long totalPending = descriptiveCount + assignmentCount;

        return new CoursePendingDetailsResponse(
                course.getId(),
                course.getTitle(),
                totalPending,
                descriptiveCount,
                assignmentCount,
                descriptiveDtos,
                assignmentDtos
        );
    }

    /**
     * Get alerts/notifications received by the instructor.
     */
    @Transactional(readOnly = true)
    public List<PendingGradingAlertResponse> getInstructorAlerts(User currentUser, Boolean unreadOnly) {
        validateInstructorOrAdmin(currentUser);
        List<PendingGradingAlert> alerts;
        if (Boolean.TRUE.equals(unreadOnly)) {
            alerts = pendingGradingAlertRepository.findByInstructorIdAndIsReadOrderBySentAtDesc(
                    currentUser.getId(), false);
        } else {
            alerts = pendingGradingAlertRepository.findByInstructorIdOrderBySentAtDesc(currentUser.getId());
        }
        return alerts.stream()
                .map(PendingGradingAlertResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Mark an alert notification as read.
     */
    @Transactional
    public PendingGradingAlertResponse markAlertAsRead(User currentUser, Integer alertId) {
        validateInstructorOrAdmin(currentUser);
        if (alertId == null) {
            throw new IllegalArgumentException("Alert ID cannot be null");
        }
        PendingGradingAlert alert = pendingGradingAlertRepository.findByIdAndInstructorId(alertId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Grading alert not found: " + alertId));
        alert.setIsRead(true);
        PendingGradingAlert saved = pendingGradingAlertRepository.save(alert);
        return PendingGradingAlertResponse.fromEntity(saved);
    }

    /**
     * Triggered when a new submission arrives to notify all associated instructors for the course.
     */
    @Transactional
    public List<PendingGradingAlertResponse> triggerNewSubmissionAlert(Integer courseId,
                                                                      String submissionTitle,
                                                                      String studentName) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        Set<User> instructors = resolveCourseInstructors(course);
        if (instructors.isEmpty()) {
            return Collections.emptyList();
        }

        long descCount = quizAttemptAnswerRepository.countPendingDescriptiveByCourseId(courseId);
        long assignCount = assignmentSubmissionRepository.countPendingByCourseId(courseId);
        int totalPending = (int) (descCount + assignCount);

        String title = "New Submission Awaiting Grading: " + (submissionTitle != null ? submissionTitle : "Item");
        String message = String.format("Student %s submitted '%s' for course '%s'. Total pending items: %d.",
                studentName != null ? studentName : "A student",
                submissionTitle != null ? submissionTitle : "work",
                course.getTitle(),
                totalPending);

        List<PendingGradingAlertResponse> createdAlerts = new ArrayList<>();
        for (User instructor : instructors) {
            PendingGradingAlert alert = new PendingGradingAlert(
                    instructor,
                    course,
                    GradingAlertType.NEW_SUBMISSION,
                    totalPending,
                    title,
                    message,
                    null
            );
            PendingGradingAlert saved = pendingGradingAlertRepository.save(alert);
            createdAlerts.add(PendingGradingAlertResponse.fromEntity(saved));
        }
        return createdAlerts;
    }

    /**
     * Sends daily reminder digest once per day per instructor and course when pending workload exists.
     */
    @Transactional
    public int sendDailyReminderDigests() {
        LocalDate today = LocalDate.now();
        List<Course> allCourses = courseRepository.findAll();
        int digestsSent = 0;

        for (Course course : allCourses) {
            long descCount = quizAttemptAnswerRepository.countPendingDescriptiveByCourseId(course.getId());
            long assignCount = assignmentSubmissionRepository.countPendingByCourseId(course.getId());
            int totalPending = (int) (descCount + assignCount);

            if (totalPending <= 0) {
                continue;
            }

            Set<User> instructors = resolveCourseInstructors(course);
            for (User instructor : instructors) {
                boolean alreadySent = pendingGradingAlertRepository.existsByInstructorIdAndCourseIdAndAlertTypeAndDigestDate(
                        instructor.getId(), course.getId(), GradingAlertType.DAILY_DIGEST, today);

                if (!alreadySent) {
                    String title = "Daily Grading Digest: " + totalPending + " pending submission(s)";
                    String message = String.format("You have %d pending submission(s) awaiting grading in '%s'.",
                            totalPending, course.getTitle());

                    PendingGradingAlert digest = new PendingGradingAlert(
                            instructor,
                            course,
                            GradingAlertType.DAILY_DIGEST,
                            totalPending,
                            title,
                            message,
                            today
                    );
                    pendingGradingAlertRepository.save(digest);
                    digestsSent++;
                }
            }
        }
        return digestsSent;
    }

    public Course verifyCourseAccess(Integer courseId, User user) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        if ("admin".equalsIgnoreCase(user.getRole())) {
            return course;
        }

        boolean isOwner = course.getInstructor() != null && course.getInstructor().getId().equals(user.getId());
        boolean isCollaborator = courseInstructorRepository != null
                && courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, user.getId());

        if (!isOwner && !isCollaborator) {
            throw new AccessDeniedException("You are not authorized to view grading workload for course: " + courseId);
        }
        return course;
    }

    private List<Course> resolveAssociatedCourses(User currentUser) {
        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            return courseRepository.findAll();
        }
        Set<Course> courseSet = new LinkedHashSet<>();
        List<Course> owned = courseRepository.findByInstructorId(currentUser.getId());
        if (owned != null) {
            courseSet.addAll(owned);
        }
        List<CourseInstructor> collabs = courseInstructorRepository.findByInstructorId(currentUser.getId());
        if (collabs != null) {
            for (CourseInstructor ci : collabs) {
                if (ci.getCourse() != null) {
                    courseSet.add(ci.getCourse());
                }
            }
        }
        return new ArrayList<>(courseSet);
    }

    private Set<User> resolveCourseInstructors(Course course) {
        Set<User> instructors = new HashSet<>();
        if (course.getInstructor() != null) {
            instructors.add(course.getInstructor());
        }
        List<CourseInstructor> collabs = courseInstructorRepository.findByCourseIdWithInstructor(course.getId());
        if (collabs != null) {
            for (CourseInstructor ci : collabs) {
                if (ci.getInstructor() != null) {
                    instructors.add(ci.getInstructor());
                }
            }
        }
        return instructors;
    }

    private void validateInstructorOrAdmin(User user) {
        if (user == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        String role = user.getRole();
        if (role == null || (!role.equalsIgnoreCase("INSTRUCTOR") && !role.equalsIgnoreCase("ADMIN"))) {
            throw new AccessDeniedException("Only instructors and administrators can access pending grading alerts");
        }
    }
}
