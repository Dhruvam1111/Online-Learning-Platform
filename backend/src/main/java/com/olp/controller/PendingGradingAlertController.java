package com.olp.controller;

import com.olp.dto.CoursePendingDetailsResponse;
import com.olp.dto.PendingGradingAlertResponse;
import com.olp.dto.PendingGradingCountResponse;
import com.olp.entity.User;
import com.olp.service.PendingGradingAlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for instructor pending grading alerts, badge counters, and notification feeds (FR-I33, Step 50).
 */
@RestController
@RequestMapping("/api/instructor/grading-alerts")
public class PendingGradingAlertController {

    private final PendingGradingAlertService pendingGradingAlertService;

    @Autowired
    public PendingGradingAlertController(PendingGradingAlertService pendingGradingAlertService) {
        this.pendingGradingAlertService = pendingGradingAlertService;
    }

    /**
     * Get aggregated pending grading counts for dashboard badge.
     */
    @GetMapping("/pending-count")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<PendingGradingCountResponse> getPendingGradingCount(Authentication authentication) {
        User currentUser = pendingGradingAlertService.resolveUser(authentication);
        PendingGradingCountResponse response = pendingGradingAlertService.getPendingGradingCount(currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Get pending count and lists of ungraded descriptive submissions and assignments for a course.
     */
    @GetMapping("/courses/{courseId}/pending")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<CoursePendingDetailsResponse> getCoursePendingDetails(
            @PathVariable Integer courseId,
            Authentication authentication) {
        User currentUser = pendingGradingAlertService.resolveUser(authentication);
        CoursePendingDetailsResponse response = pendingGradingAlertService.getCoursePendingDetails(currentUser, courseId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get alert notifications delivered to the instructor.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<PendingGradingAlertResponse>> getInstructorAlerts(
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
            Authentication authentication) {
        User currentUser = pendingGradingAlertService.resolveUser(authentication);
        List<PendingGradingAlertResponse> alerts = pendingGradingAlertService.getInstructorAlerts(currentUser, unreadOnly);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Mark an alert notification as read.
     */
    @PatchMapping("/{alertId}/read")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<PendingGradingAlertResponse> markAlertAsRead(
            @PathVariable Integer alertId,
            Authentication authentication) {
        User currentUser = pendingGradingAlertService.resolveUser(authentication);
        PendingGradingAlertResponse response = pendingGradingAlertService.markAlertAsRead(currentUser, alertId);
        return ResponseEntity.ok(response);
    }

    /**
     * Manually or programmatically trigger a daily reminder digest.
     */
    @PostMapping("/send-daily-digest")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> sendDailyReminderDigests() {
        int count = pendingGradingAlertService.sendDailyReminderDigests();
        return ResponseEntity.ok(Map.of("message", "Daily digests processed", "digestsSent", count));
    }

    /**
     * Trigger a new submission notification for associated course instructors.
     */
    @PostMapping("/courses/{courseId}/trigger-submission-alert")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<PendingGradingAlertResponse>> triggerSubmissionAlert(
            @PathVariable Integer courseId,
            @RequestParam(required = false, defaultValue = "Submission") String submissionTitle,
            @RequestParam(required = false, defaultValue = "Student") String studentName) {
        List<PendingGradingAlertResponse> alerts = pendingGradingAlertService.triggerNewSubmissionAlert(
                courseId, submissionTitle, studentName);
        return ResponseEntity.ok(alerts);
    }
}
