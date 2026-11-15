package com.olp.controller;

import com.olp.dto.AnnouncementRequest;
import com.olp.dto.AnnouncementResponse;
import com.olp.security.CustomUserDetails;
import com.olp.service.InstructorAnnouncementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * FR-I39 — Course Announcements Controller.
 */
@RestController
public class InstructorAnnouncementController {

    private final InstructorAnnouncementService announcementService;

    @Autowired
    public InstructorAnnouncementController(InstructorAnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @PostMapping("/api/instructor/courses/{courseId}/announcements")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<AnnouncementResponse> createAnnouncement(
            @PathVariable Integer courseId,
            @Valid @RequestBody AnnouncementRequest request,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        AnnouncementResponse created = announcementService.createAnnouncement(
                userDetails.getId(),
                courseId,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/api/instructor/courses/{courseId}/announcements")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<List<AnnouncementResponse>> getInstructorCourseAnnouncements(
            @PathVariable Integer courseId,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        List<AnnouncementResponse> announcements = announcementService.getCourseAnnouncementsForInstructor(
                userDetails.getId(),
                courseId
        );
        return ResponseEntity.ok(announcements);
    }

    @PutMapping("/api/instructor/courses/{courseId}/announcements/{announcementId}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<AnnouncementResponse> updateAnnouncement(
            @PathVariable Integer courseId,
            @PathVariable Integer announcementId,
            @Valid @RequestBody AnnouncementRequest request,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        AnnouncementResponse updated = announcementService.updateAnnouncement(
                userDetails.getId(),
                courseId,
                announcementId,
                request
        );
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/api/instructor/courses/{courseId}/announcements/{announcementId}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteAnnouncement(
            @PathVariable Integer courseId,
            @PathVariable Integer announcementId,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        announcementService.deleteAnnouncement(userDetails.getId(), courseId, announcementId);
        return ResponseEntity.ok(Map.of("message", "Announcement deleted successfully"));
    }

    @GetMapping("/api/courses/{courseId}/announcements")
    public ResponseEntity<List<AnnouncementResponse>> getPublicCourseAnnouncements(
            @PathVariable Integer courseId) {
        return ResponseEntity.ok(announcementService.getCourseAnnouncementsPublic(courseId));
    }
}

