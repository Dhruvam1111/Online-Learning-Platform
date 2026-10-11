package com.olp.controller;

import com.olp.dto.UpdateProfileRequest;
import com.olp.dto.UserResponse;
import com.olp.entity.User;
import com.olp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // =========================================================================
    // DEVELOPER A: GET /api/users/profile
    // =========================================================================

    /**
     * GET /api/users/profile
     * Authenticated endpoint — requires valid Bearer token.
     * Developer A: Profile retrieval.
     */
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        UserResponse response = userService.getProfile(currentUser);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // DEVELOPER B: PUT /api/users/profile
    // =========================================================================

    /**
     * PUT /api/users/profile
     * Authenticated endpoint — requires valid Bearer token.
     * Developer B: Profile update & validation.
     */
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        User currentUser = (User) authentication.getPrincipal();
        UserResponse response = userService.updateProfile(currentUser, request);
        return ResponseEntity.ok(response);
    }
}
