package com.olp.controller;

import com.olp.dto.AnnouncementRequest;
import com.olp.dto.AnnouncementResponse;
import com.olp.entity.User;
import com.olp.service.InstructorAnnouncementService;
import com.olp.service.NotificationService;
import jakarta.validation.Valid;
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
@RequestMapping("/api")
public class InstructorAnnouncementController {

    private final InstructorAnnouncementService announcementService;
    private final NotificationService notificationService;

    public InstructorAnnouncementController(
            InstructorAnnouncementService announcementService,
            NotificationService notificationService) {
        this.announcementService = announcementService;
        this.notificationService = notificationService;
    }

    @PostMapping("/instructor/courses/{courseId}/announcements")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<AnnouncementResponse> createAnnouncement(
            @PathVariable Integer courseId,
            @Valid @RequestBody AnnouncementRequest request,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        AnnouncementResponse created = announcementService.createAnnouncement(
                currentUser.getId(),
                courseId,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/instructor/courses/{courseId}/announcements")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<List<AnnouncementResponse>> getInstructorCourseAnnouncements(
            @PathVariable Integer courseId,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        List<AnnouncementResponse> announcements = announcementService.getCourseAnnouncementsForInstructor(
                currentUser.getId(),
                courseId
        );
        return ResponseEntity.ok(announcements);
    }

    @PutMapping("/instructor/courses/{courseId}/announcements/{announcementId}")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<AnnouncementResponse> updateAnnouncement(
            @PathVariable Integer courseId,
            @PathVariable Integer announcementId,
            @Valid @RequestBody AnnouncementRequest request,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        AnnouncementResponse updated = announcementService.updateAnnouncement(
                currentUser.getId(),
                courseId,
                announcementId,
                request
        );
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/instructor/courses/{courseId}/announcements/{announcementId}")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<Map<String, String>> deleteAnnouncement(
            @PathVariable Integer courseId,
            @PathVariable Integer announcementId,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        announcementService.deleteAnnouncement(currentUser.getId(), courseId, announcementId);
        return ResponseEntity.ok(Map.of("message", "Announcement deleted successfully"));
    }

    @GetMapping("/courses/{courseId}/announcements")
    public ResponseEntity<List<AnnouncementResponse>> getPublicCourseAnnouncements(
            @PathVariable Integer courseId) {
        return ResponseEntity.ok(announcementService.getCourseAnnouncementsPublic(courseId));
    }
}
