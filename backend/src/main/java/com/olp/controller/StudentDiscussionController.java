package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.service.CourseDiscussionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for course discussions, student questions, and instructor replies (FR-S63, FR-S64).
 */
@RestController
@RequestMapping("/api/student")
public class StudentDiscussionController {

    private final CourseDiscussionService discussionService;

    public StudentDiscussionController(CourseDiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return user;
    }

    @GetMapping("/courses/{courseId}/discussions")
    public ResponseEntity<List<CourseDiscussionResponse>> getDiscussions(
            @PathVariable Integer courseId,
            @RequestParam(required = false) String category,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(discussionService.getDiscussions(user, courseId, category));
    }

    @PostMapping("/courses/{courseId}/discussions")
    public ResponseEntity<CourseDiscussionResponse> createDiscussion(
            @PathVariable Integer courseId,
            @Valid @RequestBody CourseDiscussionRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        CourseDiscussionResponse response = discussionService.createDiscussion(user, courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/discussions/{discussionId}")
    public ResponseEntity<Map<String, Object>> getDiscussionDetails(
            @PathVariable Integer discussionId,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(discussionService.getDiscussionDetails(user, discussionId));
    }

    @PostMapping("/discussions/{discussionId}/replies")
    public ResponseEntity<DiscussionReplyResponse> replyToDiscussion(
            @PathVariable Integer discussionId,
            @Valid @RequestBody DiscussionReplyRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        DiscussionReplyResponse response = discussionService.replyToDiscussion(user, discussionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/discussions/{discussionId}/upvote")
    public ResponseEntity<CourseDiscussionResponse> upvoteDiscussion(
            @PathVariable Integer discussionId,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(discussionService.upvoteDiscussion(user, discussionId));
    }
}
