package com.olp.controller;

import com.olp.dto.StudentQuestionReportCreateRequest;
import com.olp.dto.StudentQuestionReportView;
import com.olp.entity.User;
import com.olp.service.StudentQuestionReportingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Student-only Phase 2 APIs. Instructor correction endpoints remain unchanged. */
@RestController
@RequestMapping("/api/student/question-reports")
@PreAuthorize("hasRole('student')")
public class StudentQuestionReportingController {
    private final StudentQuestionReportingService service;

    public StudentQuestionReportingController(StudentQuestionReportingService service) {
        this.service = service;
    }

    @GetMapping("/reasons")
    public List<String> reasons() {
        return List.of("AMBIGUOUS", "INCORRECT", "INCOMPLETE", "OTHER");
    }

    @PostMapping
    public ResponseEntity<StudentQuestionReportView> submit(@Valid @RequestBody StudentQuestionReportCreateRequest request,
                                                           Authentication authentication) {
        User student = service.currentStudent(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.submit(student, request));
    }

    @GetMapping
    public List<StudentQuestionReportView> history(Authentication authentication) {
        return service.history(service.currentStudent(authentication));
    }

    @GetMapping("/{reportId}")
    public StudentQuestionReportView status(@PathVariable Integer reportId, Authentication authentication) {
        return service.status(service.currentStudent(authentication), reportId);
    }

    @GetMapping("/notifications")
    public List<StudentQuestionReportView> resolved(Authentication authentication) {
        return service.resolutionFeed(service.currentStudent(authentication));
    }
}
