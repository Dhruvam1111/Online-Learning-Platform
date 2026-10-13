package com.olp.controller;

import com.olp.dto.ApiResponse;
import com.olp.dto.CreateLectureRequest;
import com.olp.dto.LectureResponse;
import com.olp.dto.ReorderLecturesRequest;
import com.olp.dto.UpdateLectureRequest;
import com.olp.entity.User;
import com.olp.service.InstructorLectureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/instructor/courses/{courseId}/lectures", "/api/instructors/courses/{courseId}/lectures"})
@PreAuthorize("hasRole('instructor')")
public class InstructorLectureController {

    private final InstructorLectureService instructorLectureService;

    public InstructorLectureController(InstructorLectureService instructorLectureService) {
        this.instructorLectureService = instructorLectureService;
    }

    /**
     * GET /api/instructor/courses/{courseId}/lectures
     * List all lectures belonging to the specified course in order.
     */
    @GetMapping
    public ResponseEntity<List<LectureResponse>> getLectures(
            @PathVariable Integer courseId,
            Authentication authentication,
            @RequestParam(name = "instructorId", required = false) Integer queryInstructorId,
            @RequestParam(name = "userId", required = false) Integer queryUserId) {

        User currentUser = instructorLectureService.resolveUser(authentication);

        if ((queryInstructorId != null && !queryInstructorId.equals(currentUser.getId())) ||
            (queryUserId != null && !queryUserId.equals(currentUser.getId()))) {
            throw new AccessDeniedException("Cannot access another instructor's course lectures");
        }

        List<LectureResponse> lectures = instructorLectureService.getCourseLectures(currentUser, courseId);
        return ResponseEntity.ok(lectures);
    }

    /**
     * PATCH /api/instructor/courses/{courseId}/lectures/reorder
     * Reorder lectures within an owned course.
     */
    @PatchMapping("/reorder")
    public ResponseEntity<List<LectureResponse>> reorderLectures(
            @PathVariable Integer courseId,
            @Valid @RequestBody ReorderLecturesRequest request,
            Authentication authentication,
            @RequestParam(name = "instructorId", required = false) Integer queryInstructorId,
            @RequestParam(name = "userId", required = false) Integer queryUserId) {

        User currentUser = instructorLectureService.resolveUser(authentication);

        if ((queryInstructorId != null && !queryInstructorId.equals(currentUser.getId())) ||
            (queryUserId != null && !queryUserId.equals(currentUser.getId()))) {
            throw new AccessDeniedException("Cannot modify another instructor's course lectures");
        }

        List<LectureResponse> reordered = instructorLectureService.reorderLectures(currentUser, courseId, request);
        return ResponseEntity.ok(reordered);
    }

    /**
     * DELETE /api/instructor/courses/{courseId}/lectures/{lectureId}
     * Delete a lecture from an owned course.
     */
    @DeleteMapping("/{lectureId}")
    public ResponseEntity<ApiResponse> deleteLecture(
            @PathVariable Integer courseId,
            @PathVariable Integer lectureId,
            Authentication authentication,
            @RequestParam(name = "instructorId", required = false) Integer queryInstructorId,
            @RequestParam(name = "userId", required = false) Integer queryUserId) {

        User currentUser = instructorLectureService.resolveUser(authentication);

        if ((queryInstructorId != null && !queryInstructorId.equals(currentUser.getId())) ||
            (queryUserId != null && !queryUserId.equals(currentUser.getId()))) {
            throw new AccessDeniedException("Cannot modify another instructor's course lectures");
        }

        instructorLectureService.deleteLecture(currentUser, courseId, lectureId);
        return ResponseEntity.ok(new ApiResponse(true, "Lecture deleted successfully"));
    }

    /**
     * POST /api/instructor/courses/{courseId}/lectures
     * Create a new lecture for an owned course (YouTube URL only).
     */
    @PostMapping
    public ResponseEntity<LectureResponse> createLecture(
            @PathVariable Integer courseId,
            @Valid @RequestBody CreateLectureRequest request,
            Authentication authentication,
            @RequestParam(name = "instructorId", required = false) Integer queryInstructorId,
            @RequestParam(name = "userId", required = false) Integer queryUserId) {

        User currentUser = instructorLectureService.resolveUser(authentication);

        if ((queryInstructorId != null && !queryInstructorId.equals(currentUser.getId())) ||
            (queryUserId != null && !queryUserId.equals(currentUser.getId()))) {
            throw new AccessDeniedException("Cannot modify another instructor's course lectures");
        }

        LectureResponse created = instructorLectureService.createLecture(currentUser, courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/instructor/courses/{courseId}/lectures/{lectureId}
     * Update an existing lecture in an owned course (YouTube URL and metadata).
     */
    @PutMapping("/{lectureId}")
    public ResponseEntity<LectureResponse> updateLecture(
            @PathVariable Integer courseId,
            @PathVariable Integer lectureId,
            @Valid @RequestBody UpdateLectureRequest request,
            Authentication authentication,
            @RequestParam(name = "instructorId", required = false) Integer queryInstructorId,
            @RequestParam(name = "userId", required = false) Integer queryUserId) {

        User currentUser = instructorLectureService.resolveUser(authentication);

        if ((queryInstructorId != null && !queryInstructorId.equals(currentUser.getId())) ||
            (queryUserId != null && !queryUserId.equals(currentUser.getId()))) {
            throw new AccessDeniedException("Cannot modify another instructor's course lectures");
        }

        LectureResponse updated = instructorLectureService.updateLecture(currentUser, courseId, lectureId, request);
        return ResponseEntity.ok(updated);
    }
}
