package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/register
     * Public — no token required.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     * Public — no token required.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/logout
     * Authenticated — requires a valid token.
     *
     * Since we use stateless JWT (no server-side session), there's nothing
     * to invalidate on the server. The client simply discards its token.
     * This endpoint exists so the frontend has a consistent API to call.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Stateless JWT: nothing to do server-side.
        // The client should delete its stored token.
        return ResponseEntity.ok().body(java.util.Map.of("message", "Logged out successfully"));
    }

    /**
     * GET /api/auth/me
     * Authenticated — requires a valid token.
     * Returns the current user's info.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        UserResponse response = authService.getCurrentUser(currentUser);
        return ResponseEntity.ok(response);
    }
}
