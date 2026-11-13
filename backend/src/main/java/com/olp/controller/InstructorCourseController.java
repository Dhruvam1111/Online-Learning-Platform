package com.olp.controller;

import com.olp.dto.ApiResponse;
import com.olp.dto.CompletionEvaluationResult;
import com.olp.dto.CourseAuditResponse;
import com.olp.dto.CourseCompletionCriteriaRequest;
import com.olp.dto.CourseCompletionCriteriaResponse;
import com.olp.dto.CourseDraftRequest;
import com.olp.dto.CourseInstructorResponse;
import com.olp.dto.CoursePreviewResponse;
import com.olp.dto.CoursePricingRequest;
import com.olp.dto.CoursePricingResponse;
import com.olp.dto.CourseResponse;
import com.olp.dto.CourseReviewStatusResponse;
import com.olp.dto.CourseSubmissionRequest;
import com.olp.dto.CourseSubmissionResponse;
import com.olp.dto.CreateCourseRequest;
import com.olp.dto.DeleteCourseRequest;
import com.olp.dto.PageResponse;
import com.olp.dto.UpdateCourseRequest;
import com.olp.dto.UpdateCourseStatusRequest;
import com.olp.entity.User;
import com.olp.service.CourseCompletionService;
import com.olp.service.InstructorCourseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({ "/api/instructor/courses", "/api/instructors/courses" })
@PreAuthorize("hasRole('instructor')")
public class InstructorCourseController {

    private final InstructorCourseService instructorCourseService;
    private final CourseCompletionService courseCompletionService;

    public InstructorCourseController(InstructorCourseService instructorCourseService) {
        this(instructorCourseService, null);
    }

    @Autowired
    public InstructorCourseController(
            InstructorCourseService instructorCourseService,
            @Autowired(required = false) CourseCompletionService courseCompletionService) {
        this.instructorCourseService = instructorCourseService;
        this.courseCompletionService = courseCompletionService;
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
     * FR-I60: List courses associated with the authenticated instructor.
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
     * GET /api/instructor/courses/my-courses (or /my, or /api/instructor/courses?page=0&size=10&status=...)
     * FR-I60: Paginated and status-filtered My Courses list (owner + co-instructor) with enrollment counts.
     */
    @GetMapping({ "/my-courses", "/my" })
    public ResponseEntity<PageResponse<CourseResponse>> getMyCourses(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "instructorId", required = false) Integer queryInstructorId,
            @RequestParam(name = "userId", required = false) Integer queryUserId) {

        User currentUser = instructorCourseService.resolveUser(authentication);

        if ((queryInstructorId != null && !queryInstructorId.equals(currentUser.getId())) ||
                (queryUserId != null && !queryUserId.equals(currentUser.getId()))) {
            throw new AccessDeniedException("Cannot access another instructor's courses");
        }

        PageResponse<CourseResponse> response = instructorCourseService.getMyCourses(
                currentUser, status, search, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/courses?page=0&size=10&status=...
     * FR-I60: Paginated My Courses query support directly on the /courses endpoint.
     */
    @GetMapping(params = "page")
    public ResponseEntity<PageResponse<CourseResponse>> getCoursesPaged(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "instructorId", required = false) Integer queryInstructorId,
            @RequestParam(name = "userId", required = false) Integer queryUserId) {
        return getMyCourses(authentication, status, search, page, size, queryInstructorId, queryUserId);
    }

    /**
     * GET /api/instructor/courses?status=PUBLISHED
     * FR-I60: Status-filtered My Courses query on /courses when page parameter is omitted.
     */
    @GetMapping(params = { "status", "!page" })
    public ResponseEntity<PageResponse<CourseResponse>> getCoursesByStatus(
            Authentication authentication,
            @RequestParam String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "instructorId", required = false) Integer queryInstructorId,
            @RequestParam(name = "userId", required = false) Integer queryUserId) {
        return getMyCourses(authentication, status, search, 0, size, queryInstructorId, queryUserId);
    }

    /**
     * GET /api/instructor/courses/drafts
     * FR-I07: List only DRAFT courses associated with the authenticated instructor.
     */
    @GetMapping("/drafts")
    public ResponseEntity<List<CourseResponse>> getDrafts(Authentication authentication) {
        User currentUser = instructorCourseService.resolveUser(authentication);
        List<CourseResponse> drafts = instructorCourseService.getInstructorDrafts(currentUser);
        return ResponseEntity.ok(drafts);
    }

    /**
     * GET /api/instructor/courses/{courseId}
     * FR-I07: Retrieve a specific course (including DRAFT) for an associated
     * instructor.
     */
    @GetMapping("/{courseId:\\d+}")
    public ResponseEntity<CourseResponse> getCourseById(
            Authentication authentication,
            @PathVariable Integer courseId) {
        User currentUser = instructorCourseService.resolveUser(authentication);
        CourseResponse response = instructorCourseService.getInstructorCourseById(currentUser, courseId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/instructor/courses/draft
     * FR-I07: Save a new course draft (or update an existing draft if id is
     * provided)
     * without requiring all mandatory course fields.
     */
    @PostMapping("/draft")
    public ResponseEntity<CourseResponse> saveDraft(
            Authentication authentication,
            @Valid @RequestBody(required = false) CourseDraftRequest request) {
        User currentUser = instructorCourseService.resolveUser(authentication);
        boolean isNew = request == null || request.getId() == null;
        CourseResponse response = instructorCourseService.saveDraft(currentUser, request);
        return ResponseEntity.status(isNew ? HttpStatus.CREATED : HttpStatus.OK).body(response);
    }

    /**
     * PUT / PATCH /api/instructor/courses/{courseId}/draft
     * FR-I07: Partially update an existing course and save it as a draft.
     */
    @RequestMapping(value = "/{courseId}/draft", method = { RequestMethod.PUT, RequestMethod.PATCH })
    public ResponseEntity<CourseResponse> updateDraft(
            Authentication authentication,
            @PathVariable Integer courseId,
            @Valid @RequestBody(required = false) CourseDraftRequest request) {
        User currentUser = instructorCourseService.resolveUser(authentication);
        CourseResponse response = instructorCourseService.saveDraft(currentUser, courseId, request);
        return ResponseEntity.ok(response);
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
     * FR-I09 & FR-I59: Change course status for an associated course (ARCHIVED
     * restricted to OWNER).
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

    /**
     * GET /api/instructor/courses/{courseId}/instructors
     * FR-I59: Retrieve all instructors associated with a course, including roles
     * and basic profile info.
     */
    @GetMapping("/{courseId:\\d+}/instructors")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<List<CourseInstructorResponse>> getCourseInstructors(
            Authentication authentication,
            @PathVariable Integer courseId) {

        User currentUser = instructorCourseService.resolveUser(authentication);
        List<CourseInstructorResponse> instructors = instructorCourseService.getCourseInstructors(currentUser,
                courseId);
        return ResponseEntity.ok(instructors);
    }

    /**
     * DELETE /api/instructor/courses/{courseId}/instructors/{instructorId}
     * FR-I59: Remove a co-instructor from a course (restricted to OWNER only).
     */
    @DeleteMapping("/{courseId:\\d+}/instructors/{instructorId:\\d+}")
    public ResponseEntity<ApiResponse> removeCourseInstructor(
            Authentication authentication,
            @PathVariable Integer courseId,
            @PathVariable Integer instructorId) {

        User currentUser = instructorCourseService.resolveUser(authentication);
        instructorCourseService.removeCourseInstructor(currentUser, courseId, instructorId);
        return ResponseEntity.ok(new ApiResponse(true, "Co-instructor removed from course successfully"));
    }

    /**
     * POST / PATCH /api/instructor/courses/{courseId}/unpublish
     * FR-I09: Unpublish a published course, removing it from the public catalogue while
     * preserving access for enrolled students and blocking new enrollments.
     */
    @RequestMapping(value = "/{courseId:\\d+}/unpublish", method = { RequestMethod.POST, RequestMethod.PATCH })
    public ResponseEntity<CourseResponse> unpublishCourse(
            Authentication authentication,
            @PathVariable Integer courseId) {

        User currentUser = instructorCourseService.resolveUser(authentication);
        CourseResponse response = instructorCourseService.unpublishCourse(currentUser, courseId);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH / POST /api/instructor/courses/{courseId}/archive
     * FR-I59: Archive a course (restricted to OWNER only).
     */
    @RequestMapping(value = "/{courseId:\\d+}/archive", method = { RequestMethod.PATCH, RequestMethod.POST })
    public ResponseEntity<CourseResponse> archiveCourse(
            Authentication authentication,
            @PathVariable Integer courseId) {

        User currentUser = instructorCourseService.resolveUser(authentication);
        CourseResponse response = instructorCourseService.archiveCourse(currentUser, courseId);
        return ResponseEntity.ok(response);
    }

    /**
     * Overloaded method for direct service/controller invocations without query/body parameters.
     */
    public ResponseEntity<ApiResponse> deleteCourse(
            Authentication authentication,
            Integer courseId) {
        return deleteCourse(authentication, courseId, null, null, null);
    }

    /**
     * DELETE /api/instructor/courses/{courseId}
     * POST /api/instructor/courses/{courseId}/delete
     * FR-I12 / FR-I59: Soft-delete a course (restricted to OWNER only).
     */
    @RequestMapping(value = { "/{courseId:\\d+}", "/{courseId:\\d+}/delete" }, method = { RequestMethod.DELETE, RequestMethod.POST })
    public ResponseEntity<ApiResponse> deleteCourse(
            Authentication authentication,
            @PathVariable Integer courseId,
            @RequestParam(value = "confirm", required = false) Boolean confirm,
            @RequestParam(value = "confirmed", required = false) Boolean confirmed,
            @RequestBody(required = false) DeleteCourseRequest request) {

        User currentUser = instructorCourseService.resolveUser(authentication);
        Boolean isConfirmed = confirm != null ? confirm : confirmed;
        String reason = null;
        if (request != null) {
            if (isConfirmed == null) {
                isConfirmed = request.getConfirm();
            }
            reason = request.getReason();
        }

        if (isConfirmed != null || reason != null) {
            instructorCourseService.deleteCourse(currentUser, courseId, isConfirmed, reason);
        } else {
            instructorCourseService.deleteCourse(currentUser, courseId);
        }
        return ResponseEntity.ok(new ApiResponse(true, "Course deleted successfully"));
    }

    /**
     * GET /api/instructor/courses/{courseId}/pricing
     * FR-I10: Retrieve pricing breakdown, active discounts, taxes, and final payable amount.
     */
    @GetMapping("/{courseId:\\d+}/pricing")
    public ResponseEntity<CoursePricingResponse> getCoursePricing(
            Authentication authentication,
            @PathVariable Integer courseId) {
        User currentUser = instructorCourseService.resolveUser(authentication);
        CoursePricingResponse response = instructorCourseService.getCoursePricing(currentUser, courseId);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/instructor/courses/{courseId}/pricing
     * FR-I10: Configure course price, discount rules, validity dates, and enrollment limits.
     */
    @PutMapping("/{courseId:\\d+}/pricing")
    public ResponseEntity<CoursePricingResponse> updateCoursePricing(
            Authentication authentication,
            @PathVariable Integer courseId,
            @Valid @RequestBody CoursePricingRequest request) {
        User currentUser = instructorCourseService.resolveUser(authentication);
        CoursePricingResponse response = instructorCourseService.updateCoursePricing(currentUser, courseId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/courses/{courseId}/preview
     * FR-I73: Preview course as student (renders draft data, modules, lessons, and pricing
     * in strictly read-only mode with a preview indicator banner).
     */
    @GetMapping("/{courseId:\\d+}/preview")
    public ResponseEntity<CoursePreviewResponse> previewCourse(
            Authentication authentication,
            @PathVariable Integer courseId) {
        User currentUser = instructorCourseService.resolveUser(authentication);
        CoursePreviewResponse response = instructorCourseService.previewCourseAsStudent(currentUser, courseId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/instructor/courses/{courseId}/submit
     * FR-I08: Submit course for admin review.
     */
    @PostMapping("/{courseId:\\d+}/submit")
    public ResponseEntity<CourseSubmissionResponse> submitCourse(
            Authentication authentication,
            @PathVariable Integer courseId,
            @RequestBody(required = false) CourseSubmissionRequest request) {
        User currentUser = instructorCourseService.resolveUser(authentication);
        CourseSubmissionResponse response = instructorCourseService.submitCourseForReview(
                currentUser, courseId, request != null ? request : new CourseSubmissionRequest());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/courses/{courseId}/submissions
     * FR-I08: Retrieve course submission history.
     */
    @GetMapping("/{courseId:\\d+}/submissions")
    public ResponseEntity<List<CourseSubmissionResponse>> getCourseSubmissions(
            Authentication authentication,
            @PathVariable Integer courseId) {
        User currentUser = instructorCourseService.resolveUser(authentication);
        List<CourseSubmissionResponse> response = instructorCourseService.getCourseSubmissions(currentUser, courseId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/courses/{courseId}/review-status
     * FR-I71: Retrieve course review status, feedback comments, and resubmission eligibility.
     */
    @GetMapping("/{courseId:\\d+}/review-status")
    public ResponseEntity<CourseReviewStatusResponse> getReviewStatus(
            Authentication authentication,
            @PathVariable Integer courseId) {
        User currentUser = instructorCourseService.resolveUser(authentication);
        CourseReviewStatusResponse response = instructorCourseService.getCourseReviewStatus(currentUser, courseId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/instructor/courses/{courseId}/resubmit
     * FR-I71: Resubmit course for admin review after changes have been requested.
     */
    @PostMapping("/{courseId:\\d+}/resubmit")
    public ResponseEntity<CourseSubmissionResponse> resubmitCourse(
            Authentication authentication,
            @PathVariable Integer courseId,
            @RequestBody(required = false) CourseSubmissionRequest request) {
        User currentUser = instructorCourseService.resolveUser(authentication);
        CourseSubmissionResponse response = instructorCourseService.resubmitCourse(
                currentUser, courseId, request != null ? request : new CourseSubmissionRequest());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/courses/{courseId}/audits
     * FR-I09: Retrieve immutable course audit history tracking edits, status changes, and unpublish actions.
     */
    @GetMapping("/{courseId:\\d+}/audits")
    public ResponseEntity<List<CourseAuditResponse>> getCourseAudits(
            Authentication authentication,
            @PathVariable Integer courseId) {
        User currentUser = instructorCourseService.resolveUser(authentication);
        List<CourseAuditResponse> response = instructorCourseService.getCourseAudits(currentUser, courseId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/courses/{courseId}/completion-criteria
     * FR-I56: Retrieve configured certificate completion criteria for a course.
     */
    @GetMapping("/{courseId:\\d+}/completion-criteria")
    public ResponseEntity<CourseCompletionCriteriaResponse> getCompletionCriteria(
            Authentication authentication,
            @PathVariable Integer courseId) {
        User currentUser = instructorCourseService.resolveUser(authentication);
        if (courseCompletionService == null) {
            return ResponseEntity.notFound().build();
        }
        CourseCompletionCriteriaResponse response = courseCompletionService.getCompletionCriteria(courseId, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT / POST /api/instructor/courses/{courseId}/completion-criteria
     * FR-I56: Configure or update certificate completion criteria (DR-03).
     */
    @RequestMapping(value = "/{courseId:\\d+}/completion-criteria", method = { RequestMethod.PUT, RequestMethod.POST })
    public ResponseEntity<CourseCompletionCriteriaResponse> configureCompletionCriteria(
            Authentication authentication,
            @PathVariable Integer courseId,
            @Valid @RequestBody CourseCompletionCriteriaRequest request) {
        User currentUser = instructorCourseService.resolveUser(authentication);
        if (courseCompletionService == null) {
            return ResponseEntity.notFound().build();
        }
        CourseCompletionCriteriaResponse response = courseCompletionService.configureCompletionCriteria(currentUser, courseId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/courses/{courseId}/completion-evaluation/{studentId}
     * FR-I56 / FR-S77: Evaluate a student's completion eligibility for the course.
     */
    @GetMapping("/{courseId:\\d+}/completion-evaluation/{studentId:\\d+}")
    public ResponseEntity<CompletionEvaluationResult> evaluateStudentCompletion(
            Authentication authentication,
            @PathVariable Integer courseId,
            @PathVariable Integer studentId) {
        User currentUser = instructorCourseService.resolveUser(authentication);
        if (courseCompletionService == null) {
            return ResponseEntity.notFound().build();
        }
        CompletionEvaluationResult result = courseCompletionService.evaluate(studentId, courseId);
        return ResponseEntity.ok(result);
    }
}
