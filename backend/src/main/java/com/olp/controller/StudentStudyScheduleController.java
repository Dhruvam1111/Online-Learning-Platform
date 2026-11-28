package com.olp.controller;

import com.olp.dto.ApiResponse;
import com.olp.dto.StudyScheduleGenerateRequest;
import com.olp.dto.StudyScheduleRequest;
import com.olp.dto.StudyScheduleResponse;
import com.olp.entity.User;
import com.olp.service.StudyScheduleService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller for student personalized study schedules and deadline tracking (FR-S52, DR-ED08).
 */
@RestController
@RequestMapping("/api/student/study-schedules")
@PreAuthorize("hasRole('student')")
public class StudentStudyScheduleController {

    private final StudyScheduleService studyScheduleService;

    public StudentStudyScheduleController(StudyScheduleService studyScheduleService) {
        this.studyScheduleService = studyScheduleService;
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return user;
    }

    @GetMapping
    public ResponseEntity<List<StudyScheduleResponse>> getSchedule(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Authentication authentication) {
        User student = resolveUser(authentication);
        return ResponseEntity.ok(studyScheduleService.getStudentSchedule(student, start, end));
    }

    @PostMapping
    public ResponseEntity<StudyScheduleResponse> createCustomItem(
            @Valid @RequestBody StudyScheduleRequest request,
            Authentication authentication) {
        User student = resolveUser(authentication);
        StudyScheduleResponse response = studyScheduleService.createCustomItem(student, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/generate")
    public ResponseEntity<List<StudyScheduleResponse>> generateSchedule(
            @RequestBody(required = false) StudyScheduleGenerateRequest request,
            Authentication authentication) {
        User student = resolveUser(authentication);
        return ResponseEntity.ok(studyScheduleService.generatePersonalizedSchedule(student, request));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<StudyScheduleResponse> markComplete(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "true") Boolean completed,
            Authentication authentication) {
        User student = resolveUser(authentication);
        return ResponseEntity.ok(studyScheduleService.markItemCompleted(student, id, completed));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteItem(
            @PathVariable Integer id,
            Authentication authentication) {
        User student = resolveUser(authentication);
        studyScheduleService.deleteItem(student, id);
        return ResponseEntity.ok(new ApiResponse(true, "Schedule item deleted successfully"));
    }
}
