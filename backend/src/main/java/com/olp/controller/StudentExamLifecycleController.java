package com.olp.controller;

import com.olp.dto.*;
import com.olp.dto.StudentExamDtos.*;
import com.olp.entity.User;
import com.olp.service.StudentExamLifecycleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/student/exams")
@PreAuthorize("hasRole('student')")
public class StudentExamLifecycleController {
    private final StudentExamLifecycleService service;
    public StudentExamLifecycleController(StudentExamLifecycleService service) { this.service = service; }
    private User student(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof User u))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        return u;
    }
    @GetMapping
    public List<ExamListItem> list(@RequestParam(required = false) String state, Authentication auth) {
        return service.list(student(auth), state);
    }
    @GetMapping("/{examId}/eligibility")
    public ExamEligibilityResponse eligibility(@PathVariable Integer examId, Authentication auth) {
        return service.eligibility(student(auth), examId);
    }
    @PostMapping("/{examId}/start")
    public ExamStartResponse start(@PathVariable Integer examId, Authentication auth) {
        return service.start(student(auth), examId);
    }
    @GetMapping("/attempts/{attemptId}")
    public AttemptState attempt(@PathVariable Integer attemptId, Authentication auth) {
        return service.attempt(student(auth), attemptId);
    }
    @PutMapping("/attempts/{attemptId}/answers")
    public SavedAnswer save(@PathVariable Integer attemptId, @Valid @RequestBody StudentExamAnswerRequest request, Authentication auth) {
        return service.save(student(auth), attemptId, request);
    }
    @PostMapping("/attempts/{attemptId}/submit")
    public AttemptState submit(@PathVariable Integer attemptId, Authentication auth) {
        return service.submit(student(auth), attemptId);
    }
    @GetMapping("/attempts/{attemptId}/result")
    public ExamResult result(@PathVariable Integer attemptId, Authentication auth) {
        return service.result(student(auth), attemptId);
    }
}
