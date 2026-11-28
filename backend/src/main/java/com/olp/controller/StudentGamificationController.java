package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.service.StudentGamificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for student gamification, points, streaks, badges, leaderboards, and challenges
 * (FR-S66, FR-S67, FR-S68, FR-S69, FR-S70).
 */
@RestController
@RequestMapping("/api/student/gamification")
public class StudentGamificationController {

    private final StudentGamificationService gamificationService;

    public StudentGamificationController(StudentGamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return user;
    }

    @GetMapping("/profile")
    public ResponseEntity<GamificationProfileResponse> getProfile(Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(gamificationService.getProfile(user));
    }

    @GetMapping("/badges")
    public ResponseEntity<List<BadgeResponse>> getBadges(Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(gamificationService.getAllBadgesForStudent(user));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard(
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(gamificationService.getLeaderboard(user, limit));
    }

    @GetMapping("/challenges")
    public ResponseEntity<List<LearningChallengeResponse>> getChallenges(Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(gamificationService.getActiveChallenges(user));
    }

    @PostMapping("/activity")
    @PreAuthorize("hasRole('student') or hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<GamificationProfileResponse> recordActivity(
            @Valid @RequestBody RecordActivityRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        GamificationProfileResponse response = gamificationService.recordActivity(
                user, request.getActivityType(), request.getReferenceId(), request.getDescription()
        );
        return ResponseEntity.ok(response);
    }
}
