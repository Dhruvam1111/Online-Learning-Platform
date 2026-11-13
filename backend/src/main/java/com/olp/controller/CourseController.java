package com.olp.controller;

import com.olp.dto.CertificateResponse;
import com.olp.dto.CompletionEvaluationResult;
import com.olp.dto.CourseDetailResponse;
import com.olp.dto.CourseInstructorResponse;
import com.olp.dto.CoursePreviewResponse;
import com.olp.dto.CoursePricingResponse;
import com.olp.dto.CourseReviewStatusResponse;
import com.olp.dto.CourseSubmissionRequest;
import com.olp.dto.CourseSubmissionResponse;
import com.olp.dto.EnrollmentResponse;
import com.olp.dto.LectureResponse;
import com.olp.dto.PublicCourseResponse;
import com.olp.entity.User;
import com.olp.service.CourseCatalogService;
import com.olp.service.CourseCompletionService;
import com.olp.service.CoursePreviewService;
import com.olp.service.CourseSubmissionService;
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
    private final CourseSubmissionService courseSubmissionService;
    private final CourseCompletionService courseCompletionService;

    public CourseController(CourseCatalogService courseCatalogService) {
        this(courseCatalogService, null, null, null);
    }

    public CourseController(
            CourseCatalogService courseCatalogService,
            CoursePreviewService coursePreviewService) {
        this(courseCatalogService, coursePreviewService, null, null);
    }

    public CourseController(
            CourseCatalogService courseCatalogService,
            CoursePreviewService coursePreviewService,
            CourseSubmissionService courseSubmissionService) {
        this(courseCatalogService, coursePreviewService, courseSubmissionService, null);
    }

    @Autowired
    public CourseController(
            CourseCatalogService courseCatalogService,
            @Autowired(required = false) CoursePreviewService coursePreviewService,
            @Autowired(required = false) CourseSubmissionService courseSubmissionService,
            @Autowired(required = false) CourseCompletionService courseCompletionService) {
        this.courseCatalogService = courseCatalogService;
        this.coursePreviewService = coursePreviewService;
        this.courseSubmissionService = courseSubmissionService;
        this.courseCompletionService = courseCompletionService;
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

    /**
     * POST /api/courses/{id}/submit
     * FR-I08: Submit course for review.
     */
    @PostMapping("/{id:\\d+}/submit")
    @PreAuthorize("hasRole('instructor')")
    public ResponseEntity<CourseSubmissionResponse> submitCourse(
            @PathVariable Integer id,
            @RequestBody(required = false) CourseSubmissionRequest request,
            Authentication authentication) {
        User currentUser = courseCatalogService.resolveUser(authentication);
        if (courseSubmissionService == null) {
            throw new IllegalStateException("CourseSubmissionService is not configured");
        }
        CourseSubmissionResponse response = courseSubmissionService.submitCourseForReview(
                currentUser, id, request != null ? request : new CourseSubmissionRequest());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/courses/{id}/submissions
     * FR-I08: Retrieve course submission history.
     */
    @GetMapping("/{id:\\d+}/submissions")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<List<CourseSubmissionResponse>> getSubmissions(
            @PathVariable Integer id,
            Authentication authentication) {
        User currentUser = courseCatalogService.resolveUser(authentication);
        if (courseSubmissionService == null) {
            throw new IllegalStateException("CourseSubmissionService is not configured");
        }
        List<CourseSubmissionResponse> response = courseSubmissionService.getSubmissionHistory(currentUser, id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/courses/{id}/review-status
     * FR-I71: Retrieve course review status.
     */
    @GetMapping("/{id:\\d+}/review-status")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<CourseReviewStatusResponse> getReviewStatus(
            @PathVariable Integer id,
            Authentication authentication) {
        User currentUser = courseCatalogService.resolveUser(authentication);
        if (courseSubmissionService == null) {
            throw new IllegalStateException("CourseSubmissionService is not configured");
        }
        CourseReviewStatusResponse response = courseSubmissionService.getReviewStatus(currentUser, id);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/courses/{id}/resubmit
     * FR-I71: Resubmit course for review after changes requested.
     */
    @PostMapping("/{id:\\d+}/resubmit")
    @PreAuthorize("hasRole('instructor')")
    public ResponseEntity<CourseSubmissionResponse> resubmitCourse(
            @PathVariable Integer id,
            @RequestBody(required = false) CourseSubmissionRequest request,
            Authentication authentication) {
        User currentUser = courseCatalogService.resolveUser(authentication);
        if (courseSubmissionService == null) {
            throw new IllegalStateException("CourseSubmissionService is not configured");
        }
        CourseSubmissionResponse response = courseSubmissionService.resubmitCourse(
                currentUser, id, request != null ? request : new CourseSubmissionRequest());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/courses/{id}/certificate-eligibility
     * FR-I56 / FR-S77: Check certificate eligibility and completion criteria for authenticated student.
     */
    @GetMapping("/{id:\\d+}/certificate-eligibility")
    public ResponseEntity<CompletionEvaluationResult> checkCertificateEligibility(
            @PathVariable Integer id,
            Authentication authentication) {
        User currentUser = courseCatalogService.resolveUser(authentication);
        if (courseCompletionService == null) {
            return ResponseEntity.notFound().build();
        }
        CompletionEvaluationResult result = courseCompletionService.evaluate(currentUser.getId(), id);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/courses/{id}/claim-certificate
     * FR-I56 / FR-S77: Claim course certificate if all completion criteria are satisfied (DR-03).
     */
    @PostMapping("/{id:\\d+}/claim-certificate")
    public ResponseEntity<CertificateResponse> claimCertificate(
            @PathVariable Integer id,
            Authentication authentication) {
        User currentUser = courseCatalogService.resolveUser(authentication);
        if (courseCompletionService == null) {
            return ResponseEntity.notFound().build();
        }
        CertificateResponse cert = courseCompletionService.issueCertificate(currentUser.getId(), id);
        return ResponseEntity.ok(cert);
    }

    /**
     * GET /api/courses/certificates/verify/{certificateCode}
     * DR-02: Public certificate verification endpoint under course namespace.
     */
    @GetMapping({ "/certificates/verify/{certificateCode}", "/certificates/{certificateCode:[A-Za-z0-9\\-]+}" })
    public ResponseEntity<CertificateResponse> verifyCertificate(
            @PathVariable String certificateCode) {
        if (courseCompletionService == null) {
            return ResponseEntity.notFound().build();
        }
        CertificateResponse cert = courseCompletionService.verifyCertificate(certificateCode);
        return ResponseEntity.ok(cert);
    }
}
