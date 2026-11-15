package com.olp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.olp.dto.AnnouncementRequest;
import com.olp.dto.AnnouncementResponse;
import com.olp.entity.User;
import com.olp.service.InstructorAnnouncementService;

@RestController
@RequestMapping("/api")
public class InstructorAnnouncementController {

    private final InstructorAnnouncementService announcementService;

    public InstructorAnnouncementController(InstructorAnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @PostMapping("/instructor/courses/{courseId}/announcements")
    public ResponseEntity<?> createAnnouncement(
            @PathVariable Integer courseId,
            @RequestBody AnnouncementRequest request,
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        try {
            AnnouncementResponse created = announcementService.createAnnouncement(courseId, request, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/instructor/courses/{courseId}/announcements")
    public ResponseEntity<?> getCourseAnnouncementsForInstructor(
            @PathVariable Integer courseId,
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        try {
            List<AnnouncementResponse> list = announcementService.getCourseAnnouncementsForInstructor(courseId, currentUser);
            return ResponseEntity.ok(list);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/instructor/announcements")
    public ResponseEntity<?> getInstructorAnnouncements(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        try {
            List<AnnouncementResponse> list = announcementService.getInstructorAnnouncements(currentUser);
            return ResponseEntity.ok(list);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @DeleteMapping("/instructor/announcements/{announcementId}")
    public ResponseEntity<?> deleteAnnouncement(
            @PathVariable Integer announcementId,
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        try {
            announcementService.deleteAnnouncement(announcementId, currentUser);
            return ResponseEntity.ok(Map.of("message", "Announcement deleted successfully"));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/courses/{courseId}/announcements")
    public ResponseEntity<?> getCourseAnnouncementsForStudent(
            @PathVariable Integer courseId,
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        try {
            List<AnnouncementResponse> list = announcementService.getCourseAnnouncementsForViewer(courseId, currentUser);
            return ResponseEntity.ok(list);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}

