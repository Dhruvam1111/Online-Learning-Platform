package com.olp.controller;

import com.olp.dto.ExamAttemptMonitoringSummaryResponse;
import com.olp.dto.ExamMonitoringTimelineResponse;
import com.olp.dto.MonitoringEventResponse;
import com.olp.dto.RecordMonitoringEventRequest;
import com.olp.entity.User;
import com.olp.service.ExamMonitoringLogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for reviewing exam monitoring logs, event timelines, and recording proctoring events (FR-I29, Step 55).
 */
@RestController
@RequestMapping("/api")
public class ExamMonitoringLogController {

    private final ExamMonitoringLogService monitoringLogService;

    @Autowired
    public ExamMonitoringLogController(ExamMonitoringLogService monitoringLogService) {
        this.monitoringLogService = monitoringLogService;
    }

    /**
     * Get monitoring logs and attempt summaries for an examination (FR-I29, Step 55).
     */
    @GetMapping("/instructor/exams/{examId}/monitoring-logs")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<ExamAttemptMonitoringSummaryResponse>> getExamMonitoringLogs(
            @PathVariable Integer examId,
            @RequestParam(required = false, defaultValue = "false") Boolean flaggedOnly,
            Authentication authentication) {
        User currentUser = monitoringLogService.resolveUser(authentication);
        List<ExamAttemptMonitoringSummaryResponse> response = monitoringLogService.getExamAttemptMonitoringList(currentUser, examId, flaggedOnly);
        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed event timeline for a specific exam attempt in chronological order (FR-I29, Step 55).
     */
    @GetMapping("/instructor/exam-attempts/{attemptId}/monitoring-timeline")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ExamMonitoringTimelineResponse> getAttemptMonitoringTimeline(
            @PathVariable Integer attemptId,
            Authentication authentication) {
        User currentUser = monitoringLogService.resolveUser(authentication);
        ExamMonitoringTimelineResponse response = monitoringLogService.getAttemptTimeline(currentUser, attemptId);
        return ResponseEntity.ok(response);
    }

    /**
     * Ingestion endpoint for student exam client to record proctoring events (FR-I29, Step 55).
     */
    @PostMapping("/student/exam-attempts/{attemptId}/monitoring-events")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<MonitoringEventResponse> recordMonitoringEvent(
            @PathVariable Integer attemptId,
            @Valid @RequestBody RecordMonitoringEventRequest request,
            Authentication authentication) {
        User currentUser = monitoringLogService.resolveUser(authentication);
        MonitoringEventResponse response = monitoringLogService.recordMonitoringEvent(currentUser, attemptId, request);
        return ResponseEntity.ok(response);
    }
}
