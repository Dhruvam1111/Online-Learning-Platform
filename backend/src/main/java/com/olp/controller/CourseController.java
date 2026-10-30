package com.olp.controller;

import com.olp.dto.CourseDetailResponse;
import com.olp.dto.EnrollmentResponse;
import com.olp.dto.LectureResponse;
import com.olp.dto.PublicCourseResponse;
import com.olp.entity.User;
import com.olp.service.CourseCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseCatalogService courseCatalogService;

    public CourseController(CourseCatalogService courseCatalogService) {
        this.courseCatalogService = courseCatalogService;
    }

    /**
     * GET /api/courses
     * Public course catalog — returns all published courses.
     */
    @GetMapping
    public ResponseEntity<List<PublicCourseResponse>> getCourses(
            @RequestParam(required = false) String search,
            Authentication authentication) {

        User currentUser = courseCatalogService.resolveUserOptional(authentication);
        List<PublicCourseResponse> courses = courseCatalogService.getPublishedCourses(search, currentUser);
        return ResponseEntity.ok(courses);
    }

    /**
     * GET /api/courses/{id}
     * Course details with instructor, lectures, ratings, and enrollment status.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CourseDetailResponse> getCourseDetail(
            @PathVariable Integer id,
            Authentication authentication) {

        User currentUser = courseCatalogService.resolveUserOptional(authentication);
        CourseDetailResponse course = courseCatalogService.getCourseDetail(id, currentUser);
        return ResponseEntity.ok(course);
    }

    /**
     * GET /api/courses/{id}/lectures
     * Retrieve all lectures for a course.
     */
    @GetMapping("/{id}/lectures")
    public ResponseEntity<List<LectureResponse>> getCourseLectures(
            @PathVariable Integer id,
            Authentication authentication) {

        User currentUser = courseCatalogService.resolveUserOptional(authentication);
        List<LectureResponse> lectures = courseCatalogService.getCourseLectures(id, currentUser);
        return ResponseEntity.ok(lectures);
    }

    /**
     * POST /api/courses/{id}/enroll
     * Enroll authenticated student in the course.
     */
    @PostMapping("/{id}/enroll")
    @PreAuthorize("hasRole('student')")
    public ResponseEntity<EnrollmentResponse> enrollCourse(
            @PathVariable Integer id,
            Authentication authentication) {

        User currentUser = courseCatalogService.resolveUser(authentication);
        EnrollmentResponse response = courseCatalogService.enrollCourse(currentUser, id);
        return ResponseEntity.ok(response);
    }
}
