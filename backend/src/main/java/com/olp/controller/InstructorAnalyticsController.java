package com.olp.controller;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.olp.dto.ActiveVsDroppedStats;
import com.olp.dto.EnrollmentAnalyticsResponse;
import com.olp.dto.InstructorEnrollmentOverviewResponse;
import com.olp.entity.User;
import com.olp.service.InstructorEnrollmentAnalyticsService;

/**
 * Step 43 / FR-I43: Enrollment Statistics & Trends Controller.
 * Restricts access to course instructors and admins (DR-05).
 * Computes time-series trends (daily/weekly/monthly), active vs. dropped students,
 * and high-speed aggregated KPIs designed to load within 4 seconds (NFR-05).
 */
@RestController
@RequestMapping("/api")
public class InstructorAnalyticsController {

    private final InstructorEnrollmentAnalyticsService analyticsService;

    public InstructorAnalyticsController(InstructorEnrollmentAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * GET /api/instructor/courses/{courseId}/analytics/enrollments
     * Computes enrollment statistics, trend charts (daily/weekly/monthly),
     * and active vs dropped student distributions for a specific course.
     */
    @GetMapping("/instructor/courses/{courseId}/analytics/enrollments")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<EnrollmentAnalyticsResponse> getCourseEnrollmentAnalytics(
            @PathVariable Integer courseId,
            @RequestParam(required = false, defaultValue = "daily") String interval,
            @RequestParam(required = false, defaultValue = "30") Integer days,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false, defaultValue = "30") Integer inactivityDaysThreshold,
            Authentication authentication) {

        User currentUser = analyticsService.resolveUser(authentication);
        EnrollmentAnalyticsResponse response = analyticsService.getCourseEnrollmentAnalytics(
                courseId,
                interval,
                days,
                startDate,
                endDate,
                inactivityDaysThreshold,
                currentUser
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/courses/{courseId}/analytics/active-vs-dropped
     * Focused endpoint for active vs. dropped / disengaged learner distributions.
     */
    @GetMapping("/instructor/courses/{courseId}/analytics/active-vs-dropped")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<ActiveVsDroppedStats> getActiveVsDroppedStats(
            @PathVariable Integer courseId,
            @RequestParam(required = false, defaultValue = "30") Integer inactivityDaysThreshold,
            Authentication authentication) {

        User currentUser = analyticsService.resolveUser(authentication);
        ActiveVsDroppedStats response = analyticsService.getActiveVsDroppedStats(
                courseId,
                inactivityDaysThreshold,
                currentUser
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/analytics/enrollments
     * Comprehensive instructor-wide overview aggregating enrollment trends
     * and comparative retention metrics across all instructor courses.
     */
    @GetMapping("/instructor/analytics/enrollments")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<InstructorEnrollmentOverviewResponse> getInstructorOverview(
            @RequestParam(required = false, defaultValue = "daily") String interval,
            @RequestParam(required = false, defaultValue = "30") Integer days,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication authentication) {

        User currentUser = analyticsService.resolveUser(authentication);
        InstructorEnrollmentOverviewResponse response = analyticsService.getInstructorOverview(
                interval,
                days,
                startDate,
                endDate,
                currentUser
        );
        return ResponseEntity.ok(response);
    }
}
