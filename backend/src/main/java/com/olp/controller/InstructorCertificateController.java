package com.olp.controller;

import com.olp.dto.CertificateResponse;
import com.olp.dto.PageResponse;
import com.olp.entity.User;
import com.olp.service.CourseCompletionService;
import com.olp.service.InstructorCourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for viewing course completion certificates issued for instructor courses (FR-I57, DR-05).
 * All certificate operations exposed here are strictly read-only.
 */
@RestController
@RequestMapping({ "/api/instructor/certificates", "/api/instructors/certificates" })
@PreAuthorize("hasRole('instructor') or hasRole('admin')")
public class InstructorCertificateController {

    private final CourseCompletionService courseCompletionService;
    private final InstructorCourseService instructorCourseService;

    @Autowired
    public InstructorCertificateController(
            CourseCompletionService courseCompletionService,
            InstructorCourseService instructorCourseService) {
        this.courseCompletionService = courseCompletionService;
        this.instructorCourseService = instructorCourseService;
    }

    /**
     * GET /api/instructor/certificates
     * FR-I57: List certificates issued for courses associated with the authenticated instructor.
     * Supports optional filtering by courseId and issue date range (startDate/fromDate, endDate/toDate).
     * If pagination parameters (page/size) are provided, returns a PageResponse; otherwise returns a List.
     */
    @GetMapping
    public ResponseEntity<?> getIssuedCertificates(
            Authentication authentication,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        User currentUser = instructorCourseService.resolveUser(authentication);
        String start = startDate != null ? startDate : fromDate;
        String end = endDate != null ? endDate : toDate;

        if (page != null || size != null) {
            int p = page != null ? page : 0;
            int s = size != null ? size : 10;
            PageResponse<CertificateResponse> response = courseCompletionService.getIssuedCertificates(
                    currentUser, courseId, start, end, p, s);
            return ResponseEntity.ok(response);
        } else {
            List<CertificateResponse> response = courseCompletionService.getIssuedCertificatesList(
                    currentUser, courseId, start, end);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * GET /api/instructor/certificates/paged
     * FR-I57: Explicitly paginated certificate query.
     */
    @GetMapping("/paged")
    public ResponseEntity<PageResponse<CertificateResponse>> getIssuedCertificatesPaged(
            Authentication authentication,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User currentUser = instructorCourseService.resolveUser(authentication);
        String start = startDate != null ? startDate : fromDate;
        String end = endDate != null ? endDate : toDate;

        PageResponse<CertificateResponse> response = courseCompletionService.getIssuedCertificates(
                currentUser, courseId, start, end, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/certificates/all
     * FR-I57: Explicitly unpaginated certificate list query.
     */
    @GetMapping("/all")
    public ResponseEntity<List<CertificateResponse>> getIssuedCertificatesAll(
            Authentication authentication,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String toDate) {

        User currentUser = instructorCourseService.resolveUser(authentication);
        String start = startDate != null ? startDate : fromDate;
        String end = endDate != null ? endDate : toDate;

        List<CertificateResponse> response = courseCompletionService.getIssuedCertificatesList(
                currentUser, courseId, start, end);
        return ResponseEntity.ok(response);
    }
}
