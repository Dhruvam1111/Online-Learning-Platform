package com.olp.controller;

import com.olp.dto.DiscussionQuestionRequest;
import com.olp.dto.DiscussionReplyRequest;
import com.olp.dto.DiscussionThreadResponse;
import com.olp.entity.User;
import com.olp.service.CourseDiscussionService;
import com.olp.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Step 29 / FR-I40 & FR-S63/S64: Course Q&A Discussions Controller.
 */
@RestController
@RequestMapping("/api")
public class CourseDiscussionController {

    private final CourseDiscussionService discussionService;
    private final NotificationService notificationService;

    public CourseDiscussionController(
            CourseDiscussionService discussionService,
            NotificationService notificationService) {
        this.discussionService = discussionService;
        this.notificationService = notificationService;
    }

    /**
     * GET /api/instructor/courses/{courseId}/discussions?unansweredOnly=false
     * FR-I40: List student questions per course for the instructor, with optional unanswered filter.
     */
    @GetMapping("/instructor/courses/{courseId}/discussions")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<List<DiscussionThreadResponse>> getInstructorCourseDiscussions(
            @PathVariable Integer courseId,
            @RequestParam(name = "unansweredOnly", defaultValue = "false") boolean unansweredOnly,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        List<DiscussionThreadResponse> threads = discussionService.getInstructorCourseDiscussions(
                currentUser.getId(),
                courseId,
                unansweredOnly
        );
        return ResponseEntity.ok(threads);
    }

    /**
     * POST /api/instructor/courses/{courseId}/discussions/{discussionId}/replies
     * FR-I40: Instructor replies to a student question and notifies the student.
     */
    @PostMapping("/instructor/courses/{courseId}/discussions/{discussionId}/replies")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<DiscussionThreadResponse> replyAsInstructor(
            @PathVariable Integer courseId,
            @PathVariable Integer discussionId,
            @Valid @RequestBody DiscussionReplyRequest request,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        DiscussionThreadResponse updated = discussionService.replyAsInstructor(
                currentUser.getId(),
                courseId,
                discussionId,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(updated);
    }

    /**
     * PATCH / PUT /api/instructor/courses/{courseId}/discussions/{discussionId}/answered
     * FR-I40: Mark or toggle a student question as answered/unanswered.
     */
    @RequestMapping(
            value = "/instructor/courses/{courseId}/discussions/{discussionId}/answered",
            method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<DiscussionThreadResponse> toggleAnswered(
            @PathVariable Integer courseId,
            @PathVariable Integer discussionId,
            @RequestBody(required = false) Map<String, Boolean> body,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        Boolean answered = body != null ? body.get("answered") : null;
        DiscussionThreadResponse updated = discussionService.setAnsweredStatus(
                currentUser.getId(),
                courseId,
                discussionId,
                answered
        );
        return ResponseEntity.ok(updated);
    }

    /**
     * PATCH / PUT /api/instructor/courses/{courseId}/discussions/{discussionId}/pin
     * FR-I40: Pin or unpin a student question at the top of the course Q&A forum.
     */
    @RequestMapping(
            value = "/instructor/courses/{courseId}/discussions/{discussionId}/pin",
            method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<DiscussionThreadResponse> togglePinned(
            @PathVariable Integer courseId,
            @PathVariable Integer discussionId,
            @RequestBody(required = false) Map<String, Boolean> body,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        Boolean pinned = body != null ? body.get("pinned") : null;
        DiscussionThreadResponse updated = discussionService.setPinnedStatus(
                currentUser.getId(),
                courseId,
                discussionId,
                pinned
        );
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/instructor/courses/{courseId}/discussions/{discussionId}
     * FR-I40: Delete a discussion thread.
     */
    @DeleteMapping("/instructor/courses/{courseId}/discussions/{discussionId}")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<Map<String, String>> deleteDiscussion(
            @PathVariable Integer courseId,
            @PathVariable Integer discussionId,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        discussionService.deleteDiscussion(currentUser.getId(), courseId, discussionId);
        return ResponseEntity.ok(Map.of("message", "Discussion deleted successfully"));
    }

    /**
     * GET /api/courses/{courseId}/discussions?unansweredOnly=false&lectureId=...
     * FR-S63/S64: View Q&A discussion threads for a course.
     */
    @GetMapping("/courses/{courseId}/discussions")
    public ResponseEntity<List<DiscussionThreadResponse>> getCourseDiscussions(
            @PathVariable Integer courseId,
            @RequestParam(name = "unansweredOnly", defaultValue = "false") boolean unansweredOnly,
            @RequestParam(name = "lectureId", required = false) Integer lectureId) {
        return ResponseEntity.ok(discussionService.getCourseDiscussions(courseId, unansweredOnly, lectureId));
    }

    /**
     * POST /api/courses/{courseId}/discussions
     * FR-S63: Enrolled student (or instructor) posts a question in the course Q&A forum.
     */
    @PostMapping("/courses/{courseId}/discussions")
    public ResponseEntity<DiscussionThreadResponse> createCourseQuestion(
            @PathVariable Integer courseId,
            @Valid @RequestBody DiscussionQuestionRequest request,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        DiscussionThreadResponse created = discussionService.createQuestion(
                currentUser.getId(),
                courseId,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * POST /api/courses/{courseId}/discussions/{discussionId}/replies
     * FR-S64: Enrolled student (or instructor) replies to a discussion thread.
     */
    @PostMapping("/courses/{courseId}/discussions/{discussionId}/replies")
    public ResponseEntity<DiscussionThreadResponse> addReplyToDiscussion(
            @PathVariable Integer courseId,
            @PathVariable Integer discussionId,
            @Valid @RequestBody DiscussionReplyRequest request,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        DiscussionThreadResponse updated = discussionService.addReply(
                currentUser.getId(),
                courseId,
                discussionId,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(updated);
    }
}

