package com.olp.controller;

import com.olp.dto.StudentProfileResponse;
import com.olp.dto.UpdateStudentProfileRequest;
import com.olp.entity.User;
import com.olp.service.StudentProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/student", "/api/students"})
@PreAuthorize("hasRole('student')")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    public StudentProfileController(StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    /** GET /api/student/profile - authenticated student retrieves their own profile. */
    @GetMapping("/profile")
    public ResponseEntity<StudentProfileResponse> getProfile(Authentication authentication) {
        User currentUser = studentProfileService.resolveUser(authentication);
        return ResponseEntity.ok(studentProfileService.getProfile(currentUser));
    }

    /** PUT /api/student/profile - authenticated student updates their own profile. */
    @PutMapping("/profile")
    public ResponseEntity<StudentProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateStudentProfileRequest request) {

        User currentUser = studentProfileService.resolveUser(authentication);
        return ResponseEntity.ok(studentProfileService.updateProfile(currentUser, request));
    }
}