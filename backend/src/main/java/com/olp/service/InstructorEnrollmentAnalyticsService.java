package com.olp.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.olp.dto.ActiveVsDroppedStats;
import com.olp.dto.CourseEnrollmentComparisonDto;
import com.olp.dto.CourseEnrollmentSummary;
import com.olp.dto.CourseRosterResponse;
import com.olp.dto.EnrolledStudentResponse;
import com.olp.dto.EnrollmentAnalyticsResponse;
import com.olp.dto.EnrollmentDataPoint;
import com.olp.dto.InstructorEnrollmentOverviewResponse;
import com.olp.entity.Course;
import com.olp.entity.CourseInstructor;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.EnrollmentRepository;
import com.olp.repository.UserRepository;

/**
 * Step 43 / FR-I43: Enrollment Statistics & Trends Service.
 * Calculates total enrollments, enrollments over time (daily/weekly/monthly),
 * and active vs. dropped students built on top of roster data.
 * Uses performant database aggregations to ensure response times well under 4 seconds (NFR-05).
 */
@Service
public class InstructorEnrollmentAnalyticsService {

    private static final DateTimeFormatter DAY_LABEL_FORMAT = DateTimeFormatter.ofPattern("MMM d");
    private static final DateTimeFormatter MONTH_LABEL_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy");

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final InstructorRosterService rosterService;
    private final UserRepository userRepository;

    public InstructorEnrollmentAnalyticsService(
            EnrollmentRepository enrollmentRepository,
            CourseRepository courseRepository,
            CourseInstructorRepository courseInstructorRepository,
            InstructorRosterService rosterService,
            UserRepository userRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.rosterService = rosterService;
        this.userRepository = userRepository;
    }

    public User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public EnrollmentAnalyticsResponse getCourseEnrollmentAnalytics(
            Integer courseId,
            String interval,
            Integer days,
            LocalDateTime customStartDate,
            LocalDateTime customEndDate,
            Integer inactivityDaysThreshold,
            User currentUser) {

        Course course = verifyCourseAccess(courseId, currentUser);

        int effectiveDays = (days != null && days > 0) ? days : 30;
        int threshold = (inactivityDaysThreshold != null && inactivityDaysThreshold > 0) ? inactivityDaysThreshold : 30;
        String normalizedInterval = normalizeInterval(interval);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = (customEndDate != null) ? customEndDate : now;
        LocalDateTime startDate = (customStartDate != null)
                ? customStartDate
                : endDate.minusDays(effectiveDays - 1L).with(LocalTime.MIN);

        CourseEnrollmentSummary summary = calculateCourseSummary(course.getId(), now);
        ActiveVsDroppedStats retention = calculateActiveVsDropped(course.getId(), threshold, currentUser);
        List<EnrollmentDataPoint> trend = calculateCourseTrend(course.getId(), normalizedInterval, startDate, endDate);

        return new EnrollmentAnalyticsResponse(
                course.getId(),
                course.getTitle(),
                normalizedInterval,
                startDate,
                endDate,
                summary,
                retention,
                trend
        );
    }

    @Transactional(readOnly = true)
    public ActiveVsDroppedStats getActiveVsDroppedStats(
            Integer courseId,
            Integer inactivityDaysThreshold,
            User currentUser) {

        verifyCourseAccess(courseId, currentUser);
        int threshold = (inactivityDaysThreshold != null && inactivityDaysThreshold > 0) ? inactivityDaysThreshold : 30;
        return calculateActiveVsDropped(courseId, threshold, currentUser);
    }

    @Transactional(readOnly = true)
    public InstructorEnrollmentOverviewResponse getInstructorOverview(
            String interval,
            Integer days,
            LocalDateTime customStartDate,
            LocalDateTime customEndDate,
            User currentUser) {

        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }

        List<Course> courses = getInstructorCourses(currentUser);
        if (courses.isEmpty()) {
            return new InstructorEnrollmentOverviewResponse(
                    0, 0, 0, 0, 0.0,
                    normalizeInterval(interval),
                    LocalDateTime.now().minusDays(29).with(LocalTime.MIN),
                    LocalDateTime.now(),
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        List<Integer> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());

        int effectiveDays = (days != null && days > 0) ? days : 30;
        String normalizedInterval = normalizeInterval(interval);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = (customEndDate != null) ? customEndDate : now;
        LocalDateTime startDate = (customStartDate != null)
                ? customStartDate
                : endDate.minusDays(effectiveDays - 1L).with(LocalTime.MIN);

        long totalEnrollments = 0;
        long totalActive = 0;
        long totalDropped = 0;
        double completionRateSum = 0.0;

        List<CourseEnrollmentComparisonDto> breakdown = new ArrayList<>();
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        for (Course c : courses) {
            long courseEnrolled = enrollmentRepository.countByCourseId(c.getId());
            totalEnrollments += courseEnrolled;

            ActiveVsDroppedStats retention = calculateActiveVsDropped(c.getId(), 30, currentUser);
            totalActive += retention.getActiveCount();
            totalDropped += retention.getDroppedCount();
            completionRateSum += retention.getCompletionRate();

            long recentEnrollments = enrollmentRepository.countByCourseIdAndEnrolledAtGreaterThanEqual(c.getId(), sevenDaysAgo);

            breakdown.add(new CourseEnrollmentComparisonDto(
                    c.getId(),
                    c.getTitle(),
                    c.getStatus() != null ? c.getStatus().name() : "DRAFT",
                    courseEnrolled,
                    retention.getActiveCount(),
                    retention.getDroppedCount(),
                    retention.getCompletedCount(),
                    retention.getCompletionRate(),
                    recentEnrollments
            ));
        }

        breakdown.sort(Comparator.comparing(CourseEnrollmentComparisonDto::getTotalEnrollments).reversed());

        double overallCompletionRate = courses.isEmpty()
                ? 0.0
                : Math.round((completionRateSum / courses.size()) * 10.0) / 10.0;

        List<EnrollmentDataPoint> overallTrend = calculateMultiCourseTrend(courseIds, normalizedInterval, startDate, endDate);

        return new InstructorEnrollmentOverviewResponse(
                courses.size(),
                totalEnrollments,
                totalActive,
                totalDropped,
                overallCompletionRate,
                normalizedInterval,
                startDate,
                endDate,
                overallTrend,
                breakdown
        );
    }

    private Course verifyCourseAccess(Integer courseId, User currentUser) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        boolean isOwner = course.getInstructor() != null
                && course.getInstructor().getId().equals(currentUser.getId());
        boolean isCoInstructor = courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, currentUser.getId());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole())
                || "ROLE_ADMIN".equalsIgnoreCase(currentUser.getRole());

        if (!isOwner && !isCoInstructor && !isAdmin) {
            throw new AccessDeniedException("Access denied: you can only view analytics for your own courses (DR-05)");
        }
        return course;
    }

    private List<Course> getInstructorCourses(User currentUser) {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole())
                || "ROLE_ADMIN".equalsIgnoreCase(currentUser.getRole());

        if (isAdmin) {
            return courseRepository.findAll();
        }

        Set<Integer> courseIdSet = new HashSet<>();
        List<Course> result = new ArrayList<>();

        List<Course> owned = courseRepository.findByInstructorId(currentUser.getId());
        for (Course c : owned) {
            if (courseIdSet.add(c.getId())) {
                result.add(c);
            }
        }

        List<CourseInstructor> coInstructed = courseInstructorRepository.findByInstructorId(currentUser.getId());
        for (CourseInstructor ci : coInstructed) {
            if (ci.getCourse() != null && courseIdSet.add(ci.getCourse().getId())) {
                result.add(ci.getCourse());
            }
        }

        return result;
    }

    private CourseEnrollmentSummary calculateCourseSummary(Integer courseId, LocalDateTime now) {
        long totalEnrollments = enrollmentRepository.countByCourseId(courseId);

        LocalDateTime firstDayThisMonth = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        LocalDateTime firstDayLastMonth = now.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);

        long currentMonthCount = enrollmentRepository.countByCourseIdAndEnrolledAtGreaterThanEqual(courseId, firstDayThisMonth);
        long previousMonthCount = enrollmentRepository.countByCourseIdAndEnrolledAtBetween(courseId, firstDayLastMonth, firstDayThisMonth);

        Double growthRate = null;
        if (previousMonthCount > 0) {
            double rate = ((double) (currentMonthCount - previousMonthCount) / previousMonthCount) * 100.0;
            growthRate = Math.round(rate * 10.0) / 10.0;
        } else if (currentMonthCount > 0) {
            growthRate = 100.0;
        } else {
            growthRate = 0.0;
        }

        long recent7Days = enrollmentRepository.countByCourseIdAndEnrolledAtGreaterThanEqual(courseId, now.minusDays(7));
        long recent30Days = enrollmentRepository.countByCourseIdAndEnrolledAtGreaterThanEqual(courseId, now.minusDays(30));

        return new CourseEnrollmentSummary(
                totalEnrollments,
                currentMonthCount,
                previousMonthCount,
                growthRate,
                recent7Days,
                recent30Days
        );
    }

    private ActiveVsDroppedStats calculateActiveVsDropped(Integer courseId, int inactivityDaysThreshold, User currentUser) {
        CourseRosterResponse roster = rosterService.getCourseRoster(
                courseId,
                null,
                0,
                10000,
                "enrolledAt",
                "desc",
                currentUser
        );

        List<EnrolledStudentResponse> students = roster.getStudents();
        long total = students.size();
        if (total == 0) {
            return new ActiveVsDroppedStats(0, 0, 0, 0, 0, 0, 0.0, 0.0, 0.0, inactivityDaysThreshold);
        }

        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(inactivityDaysThreshold);
        LocalDateTime fourteenDaysAgo = LocalDateTime.now().minusDays(14);

        long completedCount = 0;
        long activeCount = 0;
        long droppedCount = 0;
        long notStartedCount = 0;
        long inProgressCount = 0;

        for (EnrolledStudentResponse s : students) {
            boolean isCompleted = (s.getProgressPercentage() != null && s.getProgressPercentage() >= 100.0)
                    || s.isCertificateIssued();

            if (isCompleted) {
                completedCount++;
                continue;
            }

            double progress = (s.getProgressPercentage() != null) ? s.getProgressPercentage() : 0.0;
            if (progress > 0.0) {
                inProgressCount++;
            }

            LocalDateTime lastActivity = s.getLastActivityAt();
            if (lastActivity != null && !lastActivity.isBefore(thresholdDate)) {
                activeCount++;
            } else {
                droppedCount++;
            }

            if (progress == 0.0 && s.getEnrolledAt() != null && s.getEnrolledAt().isAfter(fourteenDaysAgo)) {
                notStartedCount++;
            }
        }

        double activeRate = Math.round(((double) activeCount / total) * 1000.0) / 10.0;
        double dropRate = Math.round(((double) droppedCount / total) * 1000.0) / 10.0;
        double completionRate = Math.round(((double) completedCount / total) * 1000.0) / 10.0;

        return new ActiveVsDroppedStats(
                total,
                activeCount,
                inProgressCount,
                completedCount,
                droppedCount,
                notStartedCount,
                activeRate,
                dropRate,
                completionRate,
                inactivityDaysThreshold
        );
    }

    private List<EnrollmentDataPoint> calculateCourseTrend(
            Integer courseId,
            String interval,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        long priorCount = enrollmentRepository.countByCourseIdAndEnrolledAtBefore(courseId, startDate);
        List<LocalDateTime> timestamps = enrollmentRepository.findEnrolledAtByCourseIdAndRange(courseId, startDate, endDate);

        return buildTrendPoints(interval, startDate, endDate, priorCount, timestamps);
    }

    private List<EnrollmentDataPoint> calculateMultiCourseTrend(
            List<Integer> courseIds,
            String interval,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        if (courseIds.isEmpty()) {
            return Collections.emptyList();
        }

        long priorCount = enrollmentRepository.countByCourseIdInAndEnrolledAtBefore(courseIds, startDate);
        List<LocalDateTime> timestamps = enrollmentRepository.findEnrolledAtByCourseIdsAndRange(courseIds, startDate, endDate);

        return buildTrendPoints(interval, startDate, endDate, priorCount, timestamps);
    }

    private List<EnrollmentDataPoint> buildTrendPoints(
            String interval,
            LocalDateTime startDate,
            LocalDateTime endDate,
            long priorCount,
            List<LocalDateTime> timestamps) {

        if ("weekly".equalsIgnoreCase(interval)) {
            return buildWeeklyTrendPoints(startDate, endDate, priorCount, timestamps);
        } else if ("monthly".equalsIgnoreCase(interval)) {
            return buildMonthlyTrendPoints(startDate, endDate, priorCount, timestamps);
        } else {
            return buildDailyTrendPoints(startDate, endDate, priorCount, timestamps);
        }
    }

    private List<EnrollmentDataPoint> buildDailyTrendPoints(
            LocalDateTime startDate,
            LocalDateTime endDate,
            long priorCount,
            List<LocalDateTime> timestamps) {

        Map<LocalDate, Long> dayCounts = new HashMap<>();
        for (LocalDateTime ts : timestamps) {
            LocalDate d = ts.toLocalDate();
            dayCounts.put(d, dayCounts.getOrDefault(d, 0L) + 1L);
        }

        LocalDate startDay = startDate.toLocalDate();
        LocalDate endDay = endDate.toLocalDate();

        List<EnrollmentDataPoint> points = new ArrayList<>();
        long cumulative = priorCount;

        LocalDate current = startDay;
        while (!current.isAfter(endDay)) {
            long count = dayCounts.getOrDefault(current, 0L);
            cumulative += count;
            points.add(new EnrollmentDataPoint(
                    current.toString(),
                    current.format(DAY_LABEL_FORMAT),
                    count,
                    cumulative
            ));
            current = current.plusDays(1);
        }

        return points;
    }

    private List<EnrollmentDataPoint> buildWeeklyTrendPoints(
            LocalDateTime startDate,
            LocalDateTime endDate,
            long priorCount,
            List<LocalDateTime> timestamps) {

        Map<LocalDate, Long> weekCounts = new LinkedHashMap<>();
        for (LocalDateTime ts : timestamps) {
            LocalDate weekStart = ts.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            weekCounts.put(weekStart, weekCounts.getOrDefault(weekStart, 0L) + 1L);
        }

        LocalDate startWeek = startDate.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endWeek = endDate.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<EnrollmentDataPoint> points = new ArrayList<>();
        long cumulative = priorCount;

        LocalDate current = startWeek;
        while (!current.isAfter(endWeek)) {
            long count = weekCounts.getOrDefault(current, 0L);
            cumulative += count;
            String label = "Week of " + current.format(DAY_LABEL_FORMAT);
            points.add(new EnrollmentDataPoint(
                    current.toString(),
                    label,
                    count,
                    cumulative
            ));
            current = current.plusWeeks(1);
        }

        return points;
    }

    private List<EnrollmentDataPoint> buildMonthlyTrendPoints(
            LocalDateTime startDate,
            LocalDateTime endDate,
            long priorCount,
            List<LocalDateTime> timestamps) {

        Map<YearMonth, Long> monthCounts = new LinkedHashMap<>();
        for (LocalDateTime ts : timestamps) {
            YearMonth ym = YearMonth.from(ts);
            monthCounts.put(ym, monthCounts.getOrDefault(ym, 0L) + 1L);
        }

        YearMonth startMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(endDate);

        List<EnrollmentDataPoint> points = new ArrayList<>();
        long cumulative = priorCount;

        YearMonth current = startMonth;
        while (!current.isAfter(endMonth)) {
            long count = monthCounts.getOrDefault(current, 0L);
            cumulative += count;
            points.add(new EnrollmentDataPoint(
                    current.toString(),
                    current.format(MONTH_LABEL_FORMAT),
                    count,
                    cumulative
            ));
            current = current.plusMonths(1);
        }

        return points;
    }

    private String normalizeInterval(String interval) {
        if (interval == null || interval.trim().isEmpty()) {
            return "DAILY";
        }
        String trimmed = interval.trim().toUpperCase();
        if ("WEEKLY".equals(trimmed) || "MONTHLY".equals(trimmed)) {
            return trimmed;
        }
        return "DAILY";
    }
}
