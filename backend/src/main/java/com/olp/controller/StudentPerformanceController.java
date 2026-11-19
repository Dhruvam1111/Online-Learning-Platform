package com.olp.controller;

import com.olp.dto.StudentPerformanceDtos.*;
import com.olp.entity.User;
import com.olp.service.StudentPerformanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Student-only reporting APIs for FR-S46..FR-S51. */
@RestController
@RequestMapping("/api/student/performance")
@PreAuthorize("hasRole('student')")
public class StudentPerformanceController {
    private final StudentPerformanceService service;
    public StudentPerformanceController(StudentPerformanceService service) { this.service = service; }

    @GetMapping("/overview")
    public ResponseEntity<Overview> overview(Authentication auth) {
        return ResponseEntity.ok(service.overview(current(auth)));
    }
    @GetMapping("/results")
    public ResponseEntity<List<Result>> history(@RequestParam(required=false) Integer courseId,
                                               Authentication auth) {
        return ResponseEntity.ok(service.history(current(auth), courseId));
    }
    @GetMapping("/topics")
    public ResponseEntity<List<Topic>> topics(@RequestParam(required=false) Integer courseId,
                                              Authentication auth) {
        return ResponseEntity.ok(service.topics(current(auth), courseId));
    }
    @GetMapping("/practice-topics")
    public ResponseEntity<List<Topic>> practiceTopics(@RequestParam(required=false) Integer courseId,
                                                      Authentication auth) {
        return ResponseEntity.ok(service.practiceTopics(current(auth), courseId));
    }
    @GetMapping("/trend")
    public ResponseEntity<List<TrendPoint>> trend(@RequestParam(required=false) Integer courseId,
                                                  Authentication auth) {
        return ResponseEntity.ok(service.trend(current(auth), courseId));
    }
    private User current(Authentication auth) { return service.resolveStudent(auth); }
}
