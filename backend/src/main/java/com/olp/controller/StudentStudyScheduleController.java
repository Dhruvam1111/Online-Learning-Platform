package com.olp.controller;

import com.olp.dto.StudySessionRequest;
import com.olp.dto.StudySessionResponse;
import com.olp.dto.StudySuggestionResponse;
import com.olp.service.StudentStudyScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/student/study-schedule")
@PreAuthorize("hasRole('student')")
public class StudentStudyScheduleController {
    private final StudentStudyScheduleService service;

    public StudentStudyScheduleController(StudentStudyScheduleService service) { this.service = service; }

    @GetMapping("/sessions")
    public List<StudySessionResponse> list(@RequestParam OffsetDateTime from,
                                           @RequestParam OffsetDateTime to,
                                           Authentication authentication) {
        return service.list(authentication, from, to);
    }

    @PostMapping("/sessions")
    public ResponseEntity<StudySessionResponse> create(@Valid @RequestBody StudySessionRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(authentication, request));
    }

    @PutMapping("/sessions/{id}")
    public StudySessionResponse update(@PathVariable Integer id, @Valid @RequestBody StudySessionRequest request,
                                       Authentication authentication) {
        return service.update(authentication, id, request);
    }

    @PatchMapping("/sessions/{id}/completion")
    public StudySessionResponse complete(@PathVariable Integer id, @RequestParam boolean completed,
                                         Authentication authentication) {
        return service.markCompleted(authentication, id, completed);
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id, Authentication authentication) {
        service.delete(authentication, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/suggestions")
    public List<StudySuggestionResponse> suggestions(@RequestParam(defaultValue = "14") int days,
                                                      Authentication authentication) {
        return service.suggestions(authentication, days);
    }
}
