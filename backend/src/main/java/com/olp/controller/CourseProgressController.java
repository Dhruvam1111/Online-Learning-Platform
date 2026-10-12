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

        User currentUser = (User) authentication.getPrincipal();
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

        User currentUser = (User) authentication.getPrincipal();
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

        User currentUser = (User) authentication.getPrincipal();
        CourseProgressResponse response = progressService.getCourseProgress(currentUser.getId(), courseId);
        return ResponseEntity.ok(response);
    }
}