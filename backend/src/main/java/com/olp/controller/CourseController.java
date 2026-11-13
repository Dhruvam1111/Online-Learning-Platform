package com.olp.controller;

import com.olp.dto.CourseDetailResponse;
import com.olp.dto.CourseInstructorResponse;
import com.olp.dto.CoursePreviewResponse;
import com.olp.dto.CoursePricingResponse;
import com.olp.dto.EnrollmentResponse;
import com.olp.dto.LectureResponse;
import com.olp.dto.PublicCourseResponse;
import com.olp.entity.User;
import com.olp.service.CourseCatalogService;
import com.olp.service.CoursePreviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseCatalogService courseCatalogService;
    private final CoursePreviewService coursePreviewService;

    public CourseController(CourseCatalogService courseCatalogService) {
        this(courseCatalogService, null);
    }

    @Autowired
    public CourseController(
            CourseCatalogService courseCatalogService,
            @Autowired(required = false) CoursePreviewService coursePreviewService) {
        this.courseCatalogService = courseCatalogService;
        this.coursePreviewService = coursePreviewService;
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
     * GET /api/courses/{id}/instructors
     * FR-I59: Retrieve all instructors associated with a course, including roles and basic profile info.
     */
    @GetMapping("/{id}/instructors")
    public ResponseEntity<List<CourseInstructorResponse>> getCourseInstructors(
            @PathVariable Integer id,
            Authentication authentication) {

        User currentUser = courseCatalogService.resolveUserOptional(authentication);
        List<CourseInstructorResponse> instructors = courseCatalogService.getCourseInstructors(id, currentUser);
        return ResponseEntity.ok(instructors);
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

    /**
     * GET /api/courses/{id}/pricing
     * FR-I10: Calculate and expose the final payable amount (price − discount + tax)
     * before payment, following DR-PY03.
     */
    @GetMapping("/{id}/pricing")
    public ResponseEntity<CoursePricingResponse> getCoursePricing(@PathVariable Integer id) {
        CoursePricingResponse response = courseCatalogService.getCoursePricing(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/courses/{id}/preview
     * FR-I73: Preview course as student for authorized instructors or admins.
     */
    @GetMapping("/{id:\\d+}/preview")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<CoursePreviewResponse> previewCourse(
            @PathVariable Integer id,
            Authentication authentication) {
        User currentUser = courseCatalogService.resolveUser(authentication);
        CoursePreviewResponse response = coursePreviewService != null
                ? coursePreviewService.previewCourseAsStudent(currentUser, id)
                : null;
        return ResponseEntity.ok(response);
    }
}
