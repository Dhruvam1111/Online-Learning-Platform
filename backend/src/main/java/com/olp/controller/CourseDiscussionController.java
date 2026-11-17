package com.olp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.olp.dto.DiscussionModerationLogResponse;
import com.olp.dto.DiscussionModerationRequest;
import com.olp.dto.DiscussionQuestionRequest;
import com.olp.dto.DiscussionReplyRequest;
import com.olp.dto.DiscussionReplyResponse;
import com.olp.dto.DiscussionThreadResponse;
import com.olp.entity.User;
import com.olp.service.CourseDiscussionService;
import com.olp.service.NotificationService;

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

    // =========================================================================
    // Step 29 / FR-I40: Instructor Q&A Forum & Discussion Endpoints
    // =========================================================================

    @GetMapping("/instructor/courses/{courseId}/discussions")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<List<DiscussionThreadResponse>> getInstructorCourseDiscussions(
            @PathVariable Integer courseId,
            @RequestParam(required = false, defaultValue = "false") Boolean unansweredOnly,
            @RequestParam(required = false) Integer lectureId,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        List<DiscussionThreadResponse> threads = discussionService.getCourseDiscussionsForInstructor(
                courseId,
                unansweredOnly,
                lectureId,
                currentUser
        );
        return ResponseEntity.ok(threads);
    }

    @PostMapping("/instructor/discussions/{discussionId}/replies")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<DiscussionReplyResponse> postInstructorReply(
            @PathVariable Integer discussionId,
            @RequestBody DiscussionReplyRequest request,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        DiscussionReplyResponse reply = discussionService.addReply(discussionId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(reply);
    }

    @PutMapping("/instructor/discussions/{discussionId}/answered")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<DiscussionThreadResponse> toggleDiscussionAnswered(
            @PathVariable Integer discussionId,
            @RequestBody(required = false) Map<String, Boolean> body,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        Boolean answered = body != null ? body.get("answered") : null;
        DiscussionThreadResponse updated = discussionService.toggleAnswered(discussionId, answered, currentUser);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/instructor/discussions/{discussionId}/pin")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<DiscussionThreadResponse> toggleDiscussionPinned(
            @PathVariable Integer discussionId,
            @RequestBody(required = false) Map<String, Boolean> body,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        Boolean pinned = body != null ? body.get("pinned") : null;
        DiscussionThreadResponse updated = discussionService.togglePinned(discussionId, pinned, currentUser);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/instructor/discussions/replies/{replyId}/official")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<DiscussionReplyResponse> toggleOfficialAnswer(
            @PathVariable Integer replyId,
            @RequestBody(required = false) Map<String, Boolean> body,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        Boolean officialAnswer = body != null ? body.get("officialAnswer") : null;
        DiscussionReplyResponse updated = discussionService.toggleOfficialAnswer(replyId, officialAnswer, currentUser);
        return ResponseEntity.ok(updated);
    }

    // =========================================================================
    // Step 30 / FR-I42 & FR-A11: Instructor Discussion Moderation & Escalation
    // =========================================================================

    @PutMapping("/instructor/discussions/{discussionId}/moderate")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<DiscussionThreadResponse> moderateDiscussion(
            @PathVariable Integer discussionId,
            @RequestBody DiscussionModerationRequest request,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        DiscussionThreadResponse moderated = discussionService.moderateDiscussion(discussionId, request, currentUser);
        return ResponseEntity.ok(moderated);
    }

    @PutMapping("/instructor/discussions/replies/{replyId}/moderate")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<DiscussionReplyResponse> moderateReply(
            @PathVariable Integer replyId,
            @RequestBody DiscussionModerationRequest request,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        DiscussionReplyResponse moderated = discussionService.moderateReply(replyId, request, currentUser);
        return ResponseEntity.ok(moderated);
    }

    @GetMapping("/instructor/courses/{courseId}/discussions/moderation-logs")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<List<DiscussionModerationLogResponse>> getCourseModerationLogs(
            @PathVariable Integer courseId,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        List<DiscussionModerationLogResponse> logs = discussionService.getCourseModerationLogs(courseId, currentUser);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/admin/discussions/escalations")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<List<DiscussionModerationLogResponse>> getAdminEscalatedLogs(
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        return ResponseEntity.ok(discussionService.getAdminEscalatedLogs(currentUser));
    }

    // =========================================================================
    // Student / Shared Course Discussion Endpoints (FR-S63 / FR-S64)
    // =========================================================================

    @GetMapping("/courses/{courseId}/discussions")
    public ResponseEntity<List<DiscussionThreadResponse>> getCourseDiscussions(
            @PathVariable Integer courseId,
            @RequestParam(required = false, defaultValue = "false") Boolean unansweredOnly,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        return ResponseEntity.ok(discussionService.getCourseDiscussionsForViewer(courseId, unansweredOnly, currentUser));
    }

    @PostMapping("/courses/{courseId}/discussions")
    public ResponseEntity<DiscussionThreadResponse> createCourseDiscussion(
            @PathVariable Integer courseId,
            @RequestBody DiscussionQuestionRequest request,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        DiscussionThreadResponse created = discussionService.createQuestion(courseId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/discussions/{discussionId}")
    public ResponseEntity<DiscussionThreadResponse> getDiscussionThread(
            @PathVariable Integer discussionId,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        return ResponseEntity.ok(discussionService.getDiscussionThread(discussionId, currentUser));
    }

    @PostMapping("/discussions/{discussionId}/replies")
    public ResponseEntity<DiscussionReplyResponse> createDiscussionReply(
            @PathVariable Integer discussionId,
            @RequestBody DiscussionReplyRequest request,
            Authentication authentication) {
        User currentUser = notificationService.resolveUser(authentication);
        DiscussionReplyResponse reply = discussionService.addReply(discussionId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(reply);
    }
}
