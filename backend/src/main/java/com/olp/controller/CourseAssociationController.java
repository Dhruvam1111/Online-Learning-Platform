package com.olp.controller;

import com.olp.dto.ApiResponse;
import com.olp.dto.AvailableCourseAssociationResponse;
import com.olp.dto.CourseAssociationRequestDto;
import com.olp.dto.CourseAssociationRequestResponse;
import com.olp.entity.User;
import com.olp.service.CourseAssociationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller exposing instructor endpoints for Course Association Requests (FR-I58).
 */
@RestController
@RequestMapping({"/api/instructor", "/api/instructors"})
@PreAuthorize("hasRole('instructor')")
public class CourseAssociationController {

    private final CourseAssociationService courseAssociationService;

    public CourseAssociationController(CourseAssociationService courseAssociationService) {
        this.courseAssociationService = courseAssociationService;
    }

    /**
     * GET /api/instructor/association-requests/courses
     * Search existing published courses available for instructor association.
     */
    @GetMapping("/association-requests/courses")
    public ResponseEntity<List<AvailableCourseAssociationResponse>> searchAvailableCourses(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category) {
        User currentUser = courseAssociationService.resolveUser(authentication);
        List<AvailableCourseAssociationResponse> courses =
                courseAssociationService.searchAvailableCourses(currentUser, search, category);
        return ResponseEntity.ok(courses);
    }

    /**
     * POST /api/instructor/association-requests
     * Submit a request to join an existing course (courseId provided in request body).
     */
    @PostMapping("/association-requests")
    public ResponseEntity<CourseAssociationRequestResponse> submitAssociationRequest(
            Authentication authentication,
            @Valid @RequestBody CourseAssociationRequestDto request) {
        User currentUser = courseAssociationService.resolveUser(authentication);
        CourseAssociationRequestResponse response =
                courseAssociationService.submitAssociationRequest(currentUser, null, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/instructor/courses/{courseId}/association-requests
     * Submit a request to join a specific course identified by path variable.
     */
    @PostMapping("/courses/{courseId}/association-requests")
    public ResponseEntity<CourseAssociationRequestResponse> submitCourseAssociationRequest(
            Authentication authentication,
            @PathVariable Integer courseId,
            @Valid @RequestBody(required = false) CourseAssociationRequestDto request) {
        User currentUser = courseAssociationService.resolveUser(authentication);
        CourseAssociationRequestResponse response =
                courseAssociationService.submitAssociationRequest(currentUser, courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/instructor/association-requests
     * List association requests submitted by the authenticated instructor.
     */
    @GetMapping("/association-requests")
    public ResponseEntity<List<CourseAssociationRequestResponse>> getMyRequests(
            Authentication authentication,
            @RequestParam(required = false) String status) {
        User currentUser = courseAssociationService.resolveUser(authentication);
        List<CourseAssociationRequestResponse> requests =
                courseAssociationService.getMyRequests(currentUser, status);
        return ResponseEntity.ok(requests);
    }

    /**
     * GET /api/instructor/association-requests/{requestId}
     * View a single association request owned by the authenticated instructor.
     */
    @GetMapping("/association-requests/{requestId:\\d+}")
    public ResponseEntity<CourseAssociationRequestResponse> getMyRequestById(
            Authentication authentication,
            @PathVariable Integer requestId) {
        User currentUser = courseAssociationService.resolveUser(authentication);
        CourseAssociationRequestResponse response =
                courseAssociationService.getMyRequestById(currentUser, requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/instructor/association-requests/{requestId}
     * Cancel a PENDING association request owned by the authenticated instructor.
     */
    @DeleteMapping("/association-requests/{requestId:\\d+}")
    public ResponseEntity<ApiResponse> cancelMyRequest(
            Authentication authentication,
            @PathVariable Integer requestId) {
        User currentUser = courseAssociationService.resolveUser(authentication);
        courseAssociationService.cancelMyRequest(currentUser, requestId);
        return ResponseEntity.ok(new ApiResponse(true, "Association request cancelled successfully"));
    }
}

