package com.olp.controller;

import com.olp.dto.InstructorProfileResponse;
import com.olp.dto.UpdateInstructorProfileRequest;
import com.olp.entity.User;
import com.olp.service.InstructorProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/instructor", "/api/instructors"})
@PreAuthorize("hasRole('instructor')")
public class InstructorProfileController {

    private final InstructorProfileService instructorProfileService;

    public InstructorProfileController(InstructorProfileService instructorProfileService) {
        this.instructorProfileService = instructorProfileService;
    }

    /**
     * GET /api/instructor/profile
     * Authenticated instructor retrieves their own profile.
     * Prevents selecting another instructor's profile by supplying a different user ID.
     */
    @GetMapping("/profile")
    public ResponseEntity<InstructorProfileResponse> getProfile(
            Authentication authentication,
            @RequestParam(name = "id", required = false) Integer queryId,
            @RequestParam(name = "instructorId", required = false) Integer queryInstructorId,
            @RequestParam(name = "userId", required = false) Integer queryUserId) {

        User currentUser = instructorProfileService.resolveUser(authentication);

        if ((queryId != null && !queryId.equals(currentUser.getId())) ||
            (queryInstructorId != null && !queryInstructorId.equals(currentUser.getId())) ||
            (queryUserId != null && !queryUserId.equals(currentUser.getId()))) {
            throw new AccessDeniedException("Cannot access another instructor's profile");
        }

        InstructorProfileResponse response = instructorProfileService.getProfile(currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/instructor/profile
     * Authenticated instructor updates their editable profile fields
     * (qualifications, expertise areas, bio, name).
     */
    @PutMapping("/profile")
    public ResponseEntity<InstructorProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateInstructorProfileRequest request) {

        User currentUser = instructorProfileService.resolveUser(authentication);
        InstructorProfileResponse response = instructorProfileService.updateProfile(currentUser, request);
        return ResponseEntity.ok(response);
    }
}
