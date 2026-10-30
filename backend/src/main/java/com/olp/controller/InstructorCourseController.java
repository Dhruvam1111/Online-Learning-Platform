package com.olp.controller;

import com.olp.dto.CourseResponse;
import com.olp.dto.CreateCourseRequest;
import com.olp.dto.UpdateCourseRequest;
import com.olp.dto.UpdateCourseStatusRequest;
import com.olp.entity.User;
import com.olp.service.InstructorCourseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/instructor/courses", "/api/instructors/courses"})
@PreAuthorize("hasRole('instructor')")
public class InstructorCourseController {

    private final InstructorCourseService instructorCourseService;

    public InstructorCourseController(InstructorCourseService instructorCourseService) {
        this.instructorCourseService = instructorCourseService;
    }

    /**
     * POST /api/instructor/courses
     * Create a new course owned by the authenticated instructor.
     */
    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(
            Authentication authentication,
            @Valid @RequestBody CreateCourseRequest request) {

        User currentUser = instructorCourseService.resolveUser(authentication);
        CourseResponse response = instructorCourseService.createCourse(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/instructor/courses
     * FR-I60: List courses owned by the authenticated instructor.
     */
    @GetMapping
    public ResponseEntity<List<CourseResponse>> getCourses(
            Authentication authentication,
            @RequestParam(name = "instructorId", required = false) Integer queryInstructorId,
            @RequestParam(name = "userId", required = false) Integer queryUserId) {

        User currentUser = instructorCourseService.resolveUser(authentication);

        if ((queryInstructorId != null && !queryInstructorId.equals(currentUser.getId())) ||
            (queryUserId != null && !queryUserId.equals(currentUser.getId()))) {
            throw new AccessDeniedException("Cannot access another instructor's courses");
        }

        List<CourseResponse> courses = instructorCourseService.getInstructorCourses(currentUser);
        return ResponseEntity.ok(courses);
    }

    /**
     * PUT /api/instructor/courses/{courseId}
     * FR-I09: Update title and description for an owned course.
     */
    @PutMapping("/{courseId}")
    public ResponseEntity<CourseResponse> updateCourse(
            Authentication authentication,
            @PathVariable Integer courseId,
            @Valid @RequestBody UpdateCourseRequest request) {

        User currentUser = instructorCourseService.resolveUser(authentication);
        CourseResponse response = instructorCourseService.updateCourse(currentUser, courseId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/instructor/courses/{courseId}/status
     * FR-I09: Change course status between DRAFT and PUBLISHED for an owned course.
     */
    @PatchMapping("/{courseId}/status")
    public ResponseEntity<CourseResponse> updateCourseStatus(
            Authentication authentication,
            @PathVariable Integer courseId,
            @Valid @RequestBody UpdateCourseStatusRequest request) {

        User currentUser = instructorCourseService.resolveUser(authentication);
        CourseResponse response = instructorCourseService.updateCourseStatus(currentUser, courseId, request);
        return ResponseEntity.ok(response);
    }
}
