package com.olp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.olp.dto.CourseRosterResponse;
import com.olp.dto.CourseRosterSummaryResponse;
import com.olp.entity.User;
import com.olp.service.InstructorRosterService;

/**
 * Step 42 / FR-I63: Enrolled Student Roster Controller.
 * Restricts access to course instructors and admins (DR-05).
 * Serves as the primary data source for student tracking and analytics.
 */
@RestController
@RequestMapping("/api")
public class InstructorRosterController {

    private final InstructorRosterService rosterService;

    public InstructorRosterController(InstructorRosterService rosterService) {
        this.rosterService = rosterService;
    }

    /**
     * GET /api/instructor/courses/{courseId}/roster
     * Lists enrolled students with enrollment date, progress %, and last activity.
     * Supports search (name/email), pagination, and sorting (enrolledAt, name, email, progress, lastActivity).
     */
    @GetMapping("/instructor/courses/{courseId}/roster")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<CourseRosterResponse> getCourseRoster(
            @PathVariable Integer courseId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "enrolledAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {

        User currentUser = rosterService.resolveUser(authentication);
        CourseRosterResponse response = rosterService.getCourseRoster(
                courseId,
                search,
                page,
                size,
                sortBy,
                sortDir,
                currentUser
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/courses/{courseId}/students
     * Alias endpoint for roster retrieval.
     */
    @GetMapping("/instructor/courses/{courseId}/students")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<CourseRosterResponse> getCourseStudents(
            @PathVariable Integer courseId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "enrolledAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {

        User currentUser = rosterService.resolveUser(authentication);
        CourseRosterResponse response = rosterService.getCourseRoster(
                courseId,
                search,
                page,
                size,
                sortBy,
                sortDir,
                currentUser
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/courses/{courseId}/roster/summary
     * Quick aggregation summary of the enrolled student roster for analytics widgets.
     */
    @GetMapping("/instructor/courses/{courseId}/roster/summary")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<CourseRosterSummaryResponse> getCourseRosterSummary(
            @PathVariable Integer courseId,
            Authentication authentication) {

        User currentUser = rosterService.resolveUser(authentication);
        CourseRosterSummaryResponse summary = rosterService.getRosterSummary(courseId, currentUser);
        return ResponseEntity.ok(summary);
    }
}
