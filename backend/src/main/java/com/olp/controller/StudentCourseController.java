package com.olp.controller;

import com.olp.dto.MyCourseResponse;
import com.olp.entity.User;
import com.olp.service.MyCoursesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@PreAuthorize("hasRole('student')")
public class StudentCourseController {

    private final MyCoursesService myCoursesService;

    public StudentCourseController(MyCoursesService myCoursesService) {
        this.myCoursesService = myCoursesService;
    }

    /** GET /api/students/my-courses - courses the logged-in student is enrolled in. */
    @GetMapping("/my-courses")
    public ResponseEntity<List<MyCourseResponse>> getMyCourses(Authentication authentication) {
        User currentUser = myCoursesService.resolveUser(authentication);
        return ResponseEntity.ok(myCoursesService.getMyCourses(currentUser));
    }
}