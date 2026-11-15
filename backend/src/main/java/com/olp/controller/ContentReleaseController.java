package com.olp.controller;

import com.olp.dto.ScheduleContentReleaseRequest;
import com.olp.dto.ScheduledReleaseResponse;
import com.olp.entity.User;
import com.olp.service.ScheduledContentReleaseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for managing content release schedules for lessons and learning materials (FR-I18, Step 20).
 * Enforces DR-ED08 timezone handling and server-side release gating.
 */
@RestController
@RequestMapping({"/api/instructor", "/api/instructors"})
@PreAuthorize("hasAnyRole('instructor', 'admin')")
public class ContentReleaseController {

    private final ScheduledContentReleaseService releaseService;

    @Autowired
    public ContentReleaseController(ScheduledContentReleaseService releaseService) {
        this.releaseService = releaseService;
    }

    /**
     * PUT /api/instructor/materials/{materialId}/schedule-release
     * Schedules release date-time for a supplementary learning material.
     */
    @PutMapping("/materials/{materialId}/schedule-release")
    public ResponseEntity<ScheduledReleaseResponse> scheduleMaterialRelease(
            @PathVariable Integer materialId,
            @Valid @RequestBody ScheduleContentReleaseRequest request,
            Authentication authentication) {
        User instructor = releaseService.resolveUser(authentication);
        ScheduledReleaseResponse response = releaseService.scheduleMaterialRelease(instructor, materialId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/materials/{materialId}/schedule-release
     * Retrieves the release schedule for a supplementary learning material.
     */
    @GetMapping("/materials/{materialId}/schedule-release")
    public ResponseEntity<ScheduledReleaseResponse> getMaterialReleaseSchedule(
            @PathVariable Integer materialId,
            Authentication authentication) {
        User instructor = releaseService.resolveUser(authentication);
        ScheduledReleaseResponse response = releaseService.getMaterialReleaseSchedule(instructor, materialId);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/instructor/lessons/{lessonId}/schedule-release
     * Schedules release date-time for a lesson.
     */
    @PutMapping("/lessons/{lessonId}/schedule-release")
    public ResponseEntity<ScheduledReleaseResponse> scheduleLessonRelease(
            @PathVariable Integer lessonId,
            @Valid @RequestBody ScheduleContentReleaseRequest request,
            Authentication authentication) {
        User instructor = releaseService.resolveUser(authentication);
        ScheduledReleaseResponse response = releaseService.scheduleLessonRelease(instructor, lessonId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/lessons/{lessonId}/schedule-release
     * Retrieves the release schedule for a lesson.
     */
    @GetMapping("/lessons/{lessonId}/schedule-release")
    public ResponseEntity<ScheduledReleaseResponse> getLessonReleaseSchedule(
            @PathVariable Integer lessonId,
            Authentication authentication) {
        User instructor = releaseService.resolveUser(authentication);
        ScheduledReleaseResponse response = releaseService.getLessonReleaseSchedule(instructor, lessonId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/instructor/content/scan-releases
     * Triggers publication scan for all due content items.
     */
    @PostMapping("/content/scan-releases")
    public ResponseEntity<Map<String, Object>> scanAndPublishReleases(Authentication authentication) {
        releaseService.resolveUser(authentication);
        int publishedCount = releaseService.triggerContentReleaseScan();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "publishedCount", publishedCount,
                "message", "Content release scan executed successfully"
        ));
    }
}

