package com.olp.controller;

import com.olp.dto.QuestionReportRequest;
import com.olp.dto.ReportedQuestionInboxResponse;
import com.olp.entity.AssessmentType;
import com.olp.entity.ReportedQuestionStatus;
import com.olp.entity.User;
import com.olp.service.ReportedQuestionAlertService;
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
 * REST controller for question report submissions, instructor alert inbox,
 * and escalation management (FR-I35, Step 52).
 */
@RestController
public class ReportedQuestionAlertController {

    private final ReportedQuestionAlertService alertService;

    @Autowired
    public ReportedQuestionAlertController(ReportedQuestionAlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * Student reports an assessment question with reason, optional explanation and attemptId (FR-I35, FR-S41).
     */
    @PostMapping("/api/student/assessments/{assessmentType}/{assessmentId}/questions/{questionId}/report")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ReportedQuestionInboxResponse> reportQuestion(
            @PathVariable AssessmentType assessmentType,
            @PathVariable Integer assessmentId,
            @PathVariable Integer questionId,
            @Valid @RequestBody QuestionReportRequest request,
            Authentication authentication) {
        User student = alertService.resolveUser(authentication);
        ReportedQuestionInboxResponse response = alertService.reportQuestion(
                student, assessmentType, assessmentId, questionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieve reported questions inbox for instructor with optional course and status filters (FR-I35, Step 52).
     */
    @GetMapping("/api/instructor/reported-questions/inbox")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<ReportedQuestionInboxResponse>> getInstructorInbox(
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) ReportedQuestionStatus status,
            Authentication authentication) {
        User instructor = alertService.resolveUser(authentication);
        List<ReportedQuestionInboxResponse> inbox = alertService.getInstructorInbox(instructor, courseId, status);
        return ResponseEntity.ok(inbox);
    }

    /**
     * Trigger SLA escalation check for overdue unresolved reports reaching administration (FR-I35, FR-A15).
     */
    @PostMapping("/api/instructor/reported-questions/escalate-unresolved")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> escalateUnresolvedReports(Authentication authentication) {
        alertService.resolveUser(authentication);
        int escalatedCount = alertService.escalateUnresolvedReports();
        return ResponseEntity.ok(Map.of(
                "message", "Escalation check completed",
                "escalatedCount", escalatedCount
        ));
    }
}
