package com.olp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.olp.dto.AssessmentPerformanceSummaryDto;
import com.olp.dto.CourseAssessmentsOverviewResponse;
import com.olp.dto.PageResponse;
import com.olp.dto.StudentAssessmentPerformanceDto;
import com.olp.dto.StudentCoursePerformanceReport;
import com.olp.entity.User;
import com.olp.service.InstructorPerformanceService;

/**
 * Step 57 / FR-I44: Student Performance & Assessment Analytics Controller.
 * Restricts access to course instructors and admins (DR-05).
 * Delivers aggregate views (average, highest, lowest, pass rate) and individual student drilldowns
 * across quizzes and examinations using published results only.
 * Optimized for rapid response times (< 4s per NFR-05).
 */
@RestController
@RequestMapping("/api")
public class InstructorPerformanceController {

    private final InstructorPerformanceService performanceService;

    public InstructorPerformanceController(InstructorPerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    /**
     * GET /api/instructor/courses/{courseId}/analytics/assessments
     * Aggregate view: overview of all quizzes and examinations in a course
     * with average score, highest/lowest marks, pass rate, and score distributions.
     */
    @GetMapping("/instructor/courses/{courseId}/analytics/assessments")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<CourseAssessmentsOverviewResponse> getCourseAssessmentsOverview(
            @PathVariable Integer courseId,
            Authentication authentication) {

        User currentUser = performanceService.resolveUser(authentication);
        CourseAssessmentsOverviewResponse response = performanceService.getCourseAssessmentsOverview(courseId, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/courses/{courseId}/analytics/assessments/{assessmentType}/{assessmentId}
     * Aggregate view for a specific quiz or examination.
     */
    @GetMapping("/instructor/courses/{courseId}/analytics/assessments/{assessmentType}/{assessmentId}")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<AssessmentPerformanceSummaryDto> getAssessmentSummary(
            @PathVariable Integer courseId,
            @PathVariable String assessmentType,
            @PathVariable Integer assessmentId,
            Authentication authentication) {

        User currentUser = performanceService.resolveUser(authentication);
        AssessmentPerformanceSummaryDto response = performanceService.getAssessmentSummary(
                courseId,
                assessmentType,
                assessmentId,
                currentUser
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/courses/{courseId}/analytics/assessments/{assessmentType}/{assessmentId}/students
     * Drilldown view: paginated list of students who attempted this assessment
     * with their best score, latest score, attempt count, and pass/fail status.
     */
    @GetMapping("/instructor/courses/{courseId}/analytics/assessments/{assessmentType}/{assessmentId}/students")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<PageResponse<StudentAssessmentPerformanceDto>> getStudentsPerformanceForAssessment(
            @PathVariable Integer courseId,
            @PathVariable String assessmentType,
            @PathVariable Integer assessmentId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        User currentUser = performanceService.resolveUser(authentication);
        PageResponse<StudentAssessmentPerformanceDto> response = performanceService.getStudentsPerformanceForAssessment(
                courseId,
                assessmentType,
                assessmentId,
                search,
                page,
                size,
                currentUser
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/courses/{courseId}/analytics/assessments/{assessmentType}/{assessmentId}/students/{studentId}
     * Individual view: detailed attempt breakdown (all attempts, scores, timestamps, feedback)
     * for a specific student on this assessment.
     */
    @GetMapping("/instructor/courses/{courseId}/analytics/assessments/{assessmentType}/{assessmentId}/students/{studentId}")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<StudentAssessmentPerformanceDto> getStudentAttemptsForAssessment(
            @PathVariable Integer courseId,
            @PathVariable String assessmentType,
            @PathVariable Integer assessmentId,
            @PathVariable Integer studentId,
            Authentication authentication) {

        User currentUser = performanceService.resolveUser(authentication);
        StudentAssessmentPerformanceDto response = performanceService.getStudentAttemptsForAssessment(
                courseId,
                assessmentType,
                assessmentId,
                studentId,
                currentUser
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/courses/{courseId}/analytics/students/{studentId}/performance
     * Individual view: comprehensive student report card across all quizzes and exams in the course.
     */
    @GetMapping("/instructor/courses/{courseId}/analytics/students/{studentId}/performance")
    @PreAuthorize("hasRole('instructor') or hasRole('admin')")
    public ResponseEntity<StudentCoursePerformanceReport> getStudentCoursePerformanceReport(
            @PathVariable Integer courseId,
            @PathVariable Integer studentId,
            Authentication authentication) {

        User currentUser = performanceService.resolveUser(authentication);
        StudentCoursePerformanceReport response = performanceService.getStudentCoursePerformanceReport(
                courseId,
                studentId,
                currentUser
        );
        return ResponseEntity.ok(response);
    }
}
