package com.olp.controller;

import com.olp.dto.CourseProgressResponse;
import com.olp.entity.User;
import com.olp.service.CourseProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/courses/{courseId}")
public class CourseProgressController {

    private final CourseProgressService progressService;

    public CourseProgressController(CourseProgressService progressService) {
        this.progressService = progressService;
    }

    private User resolveStudent(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return user;
    }

    /**
     * POST /api/courses/{courseId}/lectures/{lectureId}/complete
     * Mark a lecture as completed.
     */
    @PostMapping("/lectures/{lectureId}/complete")
    @PreAuthorize("hasRole('student')")
    public ResponseEntity<Map<String, Object>> completeLecture(
            @PathVariable Integer courseId,
            @PathVariable Integer lectureId,
            Authentication authentication) {

        User currentUser = resolveStudent(authentication);
        progressService.markLectureComplete(currentUser.getId(), courseId, lectureId);

        return ResponseEntity.ok(Map.of(
                "message", "Lecture marked as completed",
                "lectureId", lectureId,
                "completed", true
        ));
    }

    /**
     * DELETE /api/courses/{courseId}/lectures/{lectureId}/complete
     * Unmark a lecture as completed.
     */
    @DeleteMapping("/lectures/{lectureId}/complete")
    @PreAuthorize("hasRole('student')")
    public ResponseEntity<Map<String, Object>> uncompleteLecture(
            @PathVariable Integer courseId,
            @PathVariable Integer lectureId,
            Authentication authentication) {

        User currentUser = resolveStudent(authentication);
        progressService.unmarkLectureComplete(currentUser.getId(), courseId, lectureId);

        return ResponseEntity.ok(Map.of(
                "message", "Lecture unmarked as completed",
                "lectureId", lectureId,
                "completed", false
        ));
    }

    /**
     * GET /api/courses/{courseId}/progress
     * Returns completed count / total count and percentage.
     */
    @GetMapping("/progress")
    @PreAuthorize("hasRole('student')")
    public ResponseEntity<CourseProgressResponse> getProgress(
            @PathVariable Integer courseId,
            Authentication authentication) {

        User currentUser = resolveStudent(authentication);
        CourseProgressResponse response = progressService.getCourseProgress(currentUser.getId(), courseId);
        return ResponseEntity.ok(response);
    }
}