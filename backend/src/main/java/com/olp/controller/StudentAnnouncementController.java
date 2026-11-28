package com.olp.controller;

import com.olp.dto.CourseAnnouncementRequest;
import com.olp.dto.CourseAnnouncementResponse;
import com.olp.entity.User;
import com.olp.service.CourseAnnouncementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for viewing and posting course announcements (FR-S62, FR-S65).
 */
@RestController
@RequestMapping("/api/student")
public class StudentAnnouncementController {

    private final CourseAnnouncementService announcementService;

    public StudentAnnouncementController(CourseAnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return user;
    }

    @GetMapping("/courses/{courseId}/announcements")
    public ResponseEntity<List<CourseAnnouncementResponse>> getCourseAnnouncements(
            @PathVariable Integer courseId,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(announcementService.getCourseAnnouncements(user, courseId));
    }

    @GetMapping("/announcements/{announcementId}")
    public ResponseEntity<CourseAnnouncementResponse> getAnnouncement(
            @PathVariable Integer announcementId,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(announcementService.getAnnouncementById(user, announcementId));
    }

    @PostMapping("/courses/{courseId}/announcements")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<CourseAnnouncementResponse> createAnnouncement(
            @PathVariable Integer courseId,
            @Valid @RequestBody CourseAnnouncementRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        CourseAnnouncementResponse response = announcementService.createAnnouncement(user, courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
