package com.olp.controller;

import com.olp.dto.ContentDeclarationRequest;
import com.olp.dto.ContentDeclarationResponse;
import com.olp.dto.TakedownReplaceRequest;
import com.olp.dto.TakedownResponseRequest;
import com.olp.entity.User;
import com.olp.service.ContentDeclarationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for Content Ownership Declaration & Takedown Lifecycle (FR-I64, Step 14).
 * Accessible by authenticated instructors and administrators.
 */
@RestController
@RequestMapping({"/api/instructor/declarations", "/api/instructors/declarations"})
@PreAuthorize("hasAnyRole('instructor', 'admin')")
public class ContentDeclarationController {

    private final ContentDeclarationService declarationService;

    @Autowired
    public ContentDeclarationController(ContentDeclarationService declarationService) {
        this.declarationService = declarationService;
    }

    /**
     * POST /api/instructor/declarations
     * Submit an intellectual property and content ownership declaration.
     */
    @PostMapping
    public ResponseEntity<ContentDeclarationResponse> declareContent(
            Authentication authentication,
            @Valid @RequestBody ContentDeclarationRequest request) {
        User user = declarationService.resolveUser(authentication);
        ContentDeclarationResponse response = declarationService.declareContent(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/instructor/declarations/course/{courseId}
     * List all declarations associated with a course.
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<ContentDeclarationResponse>> getDeclarationsByCourse(
            @PathVariable Integer courseId,
            Authentication authentication) {
        User user = declarationService.resolveUser(authentication);
        List<ContentDeclarationResponse> response = declarationService.getDeclarationsByCourse(courseId, user);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/declarations/lesson/{lessonId}
     * Retrieve active declaration for a specific lesson (or fallback to course-level).
     */
    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<ContentDeclarationResponse> getDeclarationByLesson(
            @PathVariable Integer lessonId,
            Authentication authentication) {
        User user = declarationService.resolveUser(authentication);
        ContentDeclarationResponse response = declarationService.getDeclarationByLesson(lessonId, user);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/declarations/{id}
     * Retrieve declaration by unique ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ContentDeclarationResponse> getDeclarationById(
            @PathVariable Integer id,
            Authentication authentication) {
        User user = declarationService.resolveUser(authentication);
        ContentDeclarationResponse response = declarationService.getDeclarationById(id, user);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/instructor/declarations/{id}/respond
     * Instructor responds to a takedown notice with counter-notice or explanation.
     */
    @PostMapping("/{id}/respond")
    public ResponseEntity<ContentDeclarationResponse> respondToTakedown(
            @PathVariable Integer id,
            Authentication authentication,
            @Valid @RequestBody TakedownResponseRequest request) {
        User user = declarationService.resolveUser(authentication);
        ContentDeclarationResponse response = declarationService.respondToTakedown(id, user, request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/instructor/declarations/{id}/remove
     * Voluntarily remove flagged content in response to takedown notice.
     */
    @PostMapping("/{id}/remove")
    public ResponseEntity<ContentDeclarationResponse> removeContent(
            @PathVariable Integer id,
            Authentication authentication) {
        User user = declarationService.resolveUser(authentication);
        ContentDeclarationResponse response = declarationService.removeContent(id, user);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/instructor/declarations/{id}/replace
     * Replace flagged content with licensed or corrected version.
     */
    @PostMapping("/{id}/replace")
    public ResponseEntity<ContentDeclarationResponse> replaceContent(
            @PathVariable Integer id,
            Authentication authentication,
            @RequestBody(required = false) TakedownReplaceRequest request) {
        User user = declarationService.resolveUser(authentication);
        ContentDeclarationResponse response = declarationService.replaceContent(id, user, request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/declarations/check?courseId=...&lessonId=...
     * Fast check whether a lesson or course has a valid active ownership declaration.
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkDeclarationStatus(
            @RequestParam Integer courseId,
            @RequestParam(required = false) Integer lessonId,
            Authentication authentication) {
        User user = declarationService.resolveUser(authentication);
        Map<String, Object> result = declarationService.checkDeclarationStatus(courseId, lessonId, user);
        return ResponseEntity.ok(result);
    }
}

