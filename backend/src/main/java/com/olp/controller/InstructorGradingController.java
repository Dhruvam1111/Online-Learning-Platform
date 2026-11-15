package com.olp.controller;

import com.olp.dto.AnswerFeedbackRequest;
import com.olp.dto.AnswerFeedbackResponse;
import com.olp.dto.DescriptiveSubmissionResponse;
import com.olp.dto.GradeSubmissionRequest;
import com.olp.dto.GradeSubmissionResponse;
import com.olp.dto.PageResponse;
import com.olp.dto.PublishResultResponse;
import com.olp.dto.SubmissionFeedbackRequest;
import com.olp.dto.SubmissionFeedbackResponse;
import com.olp.entity.User;
import com.olp.service.DescriptiveGradingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Instructor grading queue, evaluating descriptive answers,
 * and publishing results (FR-I30, FR-I31, FR-I34).
 */
@RestController
@RequestMapping({ "/api/instructor/grading", "/api/instructors/grading" })
@PreAuthorize("hasRole('instructor') or hasRole('admin')")
public class InstructorGradingController {

    private final DescriptiveGradingService descriptiveGradingService;

    @Autowired
    public InstructorGradingController(DescriptiveGradingService descriptiveGradingService) {
        this.descriptiveGradingService = descriptiveGradingService;
    }

    /**
     * GET /api/instructor/grading/queue or /api/instructor/grading
     * Lists descriptive submissions in the grading queue for courses associated with the instructor.
     * Displays ungraded submissions first. Supports filtering by course, quiz, and status.
     */
    @GetMapping({ "", "/queue" })
    public ResponseEntity<?> getGradingQueue(
            Authentication authentication,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) Integer quizId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        User currentUser = descriptiveGradingService.resolveUser(authentication);

        if (page != null || size != null) {
            int p = page != null ? page : 0;
            int s = size != null ? size : 20;
            PageResponse<DescriptiveSubmissionResponse> pagedResult = descriptiveGradingService
                    .getGradingQueuePaged(currentUser, courseId, quizId, status, p, s);
            return ResponseEntity.ok(pagedResult);
        } else {
            List<DescriptiveSubmissionResponse> listResult = descriptiveGradingService
                    .getGradingQueue(currentUser, courseId, quizId, status);
            return ResponseEntity.ok(listResult);
        }
    }

    /**
     * GET /api/instructor/grading/queue/paged
     * Explicit paginated grading queue endpoint.
     */
    @GetMapping("/queue/paged")
    public ResponseEntity<PageResponse<DescriptiveSubmissionResponse>> getGradingQueuePaged(
            Authentication authentication,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) Integer quizId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        User currentUser = descriptiveGradingService.resolveUser(authentication);
        PageResponse<DescriptiveSubmissionResponse> pagedResult = descriptiveGradingService
                .getGradingQueuePaged(currentUser, courseId, quizId, status, page, size);
        return ResponseEntity.ok(pagedResult);
    }

    /**
     * GET /api/instructor/grading/queue/{answerId} or /api/instructor/grading/{answerId}
     * Retrieves full submission details for evaluation.
     */
    @GetMapping({ "/{answerId:\\d+}", "/queue/{answerId:\\d+}", "/submissions/{answerId:\\d+}" })
    public ResponseEntity<DescriptiveSubmissionResponse> getSubmissionDetails(
            Authentication authentication,
            @PathVariable Integer answerId) {

        User currentUser = descriptiveGradingService.resolveUser(authentication);
        DescriptiveSubmissionResponse response = descriptiveGradingService.getSubmissionDetails(currentUser, answerId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST or PUT /api/instructor/grading/{answerId}
     * Evaluates a descriptive submission:
     * - Validates non-negative marks and bounds (awarded <= maxMarks).
     * - Saves awarded marks, grader ID, timestamp, and feedback.
     * - Sets status to EVALUATED_UNPUBLISHED by default (or PUBLISHED if requested).
     */
    @RequestMapping(
            value = { "/{answerId:\\d+}", "/submissions/{answerId:\\d+}" },
            method = { RequestMethod.POST, RequestMethod.PUT }
    )
    public ResponseEntity<GradeSubmissionResponse> gradeSubmission(
            Authentication authentication,
            @PathVariable Integer answerId,
            @Valid @RequestBody GradeSubmissionRequest request) {

        User currentUser = descriptiveGradingService.resolveUser(authentication);
        GradeSubmissionResponse response = descriptiveGradingService.gradeSubmission(currentUser, answerId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/instructor/grading/{answerId}/publish
     * Publishes evaluation result for a single submission, making it visible to the student.
     */
    @PostMapping({ "/{answerId:\\d+}/publish", "/submissions/{answerId:\\d+}/publish" })
    public ResponseEntity<PublishResultResponse> publishAnswerResult(
            Authentication authentication,
            @PathVariable Integer answerId) {

        User currentUser = descriptiveGradingService.resolveUser(authentication);
        PublishResultResponse response = descriptiveGradingService.publishAnswerResult(currentUser, answerId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/instructor/grading/quizzes/{quizId}/publish
     * Publishes all evaluated unpublished submissions for a given quiz.
     */
    @PostMapping("/quizzes/{quizId:\\d+}/publish")
    public ResponseEntity<PublishResultResponse> publishQuizResults(
            Authentication authentication,
            @PathVariable Integer quizId) {

        User currentUser = descriptiveGradingService.resolveUser(authentication);
        PublishResultResponse response = descriptiveGradingService.publishQuizResults(currentUser, quizId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/instructor/grading/attempts/{attemptId}/publish
     * Publishes all evaluated unpublished submissions for a specific student attempt.
     */
    @PostMapping("/attempts/{attemptId:\\d+}/publish")
    public ResponseEntity<PublishResultResponse> publishAttemptResults(
            Authentication authentication,
            @PathVariable Integer attemptId) {

        User currentUser = descriptiveGradingService.resolveUser(authentication);
        PublishResultResponse response = descriptiveGradingService.publishAttemptResults(currentUser, attemptId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST or PUT /api/instructor/grading/answers/{answerId}/feedback or /{answerId}/feedback
     * Adds or updates answer-level feedback for a descriptive answer (FR-I31).
     * Prevents feedback modification after results are published.
     */
    @RequestMapping(
            value = { "/{answerId:\\d+}/feedback", "/answers/{answerId:\\d+}/feedback", "/submissions/{answerId:\\d+}/feedback" },
            method = { RequestMethod.POST, RequestMethod.PUT }
    )
    public ResponseEntity<AnswerFeedbackResponse> addOrUpdateAnswerFeedback(
            Authentication authentication,
            @PathVariable Integer answerId,
            @Valid @RequestBody AnswerFeedbackRequest request) {

        User currentUser = descriptiveGradingService.resolveUser(authentication);
        AnswerFeedbackResponse response = descriptiveGradingService.addOrUpdateAnswerFeedback(currentUser, answerId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST or PUT /api/instructor/grading/attempts/{attemptId}/feedback or /attempts/{attemptId}/overall-feedback
     * Adds or updates submission-level overall feedback for an attempt (FR-I31).
     * Prevents feedback modification after results are published.
     */
    @RequestMapping(
            value = { "/attempts/{attemptId:\\d+}/feedback", "/attempts/{attemptId:\\d+}/overall-feedback", "/submissions/attempts/{attemptId:\\d+}/feedback" },
            method = { RequestMethod.POST, RequestMethod.PUT }
    )
    public ResponseEntity<SubmissionFeedbackResponse> addOrUpdateSubmissionFeedback(
            Authentication authentication,
            @PathVariable Integer attemptId,
            @Valid @RequestBody SubmissionFeedbackRequest request) {

        User currentUser = descriptiveGradingService.resolveUser(authentication);
        SubmissionFeedbackResponse response = descriptiveGradingService.addOrUpdateSubmissionFeedback(currentUser, attemptId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/grading/attempts/{attemptId}/feedback
     * Retrieves submission-level overall feedback (FR-I31).
     */
    @GetMapping({ "/attempts/{attemptId:\\d+}/feedback", "/submissions/attempts/{attemptId:\\d+}/feedback" })
    public ResponseEntity<SubmissionFeedbackResponse> getSubmissionFeedback(
            Authentication authentication,
            @PathVariable Integer attemptId) {

        User currentUser = descriptiveGradingService.resolveUser(authentication);
        SubmissionFeedbackResponse response = descriptiveGradingService.getSubmissionFeedback(currentUser, attemptId);
        return ResponseEntity.ok(response);
    }
}
