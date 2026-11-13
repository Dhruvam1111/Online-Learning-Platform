package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.service.StudentLearningService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/learning")
@PreAuthorize("hasRole('student')")
public class StudentLearningController {

    private final StudentLearningService studentLearningService;

    public StudentLearningController(StudentLearningService studentLearningService) {
        this.studentLearningService = studentLearningService;
    }

    private Integer getStudentId(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return user.getId();
    }

    // FR-S13: Save playback position (resume point)
    @PostMapping("/lectures/{lectureId}/playback-position")
    public ResponseEntity<Map<String, Object>> updatePlaybackPosition(
            @PathVariable Integer lectureId,
            @Valid @RequestBody PlaybackPositionRequest request,
            Authentication authentication) {
        Integer studentId = getStudentId(authentication);
        return ResponseEntity.ok(studentLearningService.updatePlaybackPosition(studentId, lectureId, request.getPlaybackPositionSeconds()));
    }

    // FR-S13: Get playback position
    @GetMapping("/lectures/{lectureId}/playback-position")
    public ResponseEntity<Map<String, Object>> getPlaybackPosition(
            @PathVariable Integer lectureId,
            Authentication authentication) {
        Integer studentId = getStudentId(authentication);
        return ResponseEntity.ok(studentLearningService.getPlaybackPosition(studentId, lectureId));
    }

    // FR-S14: Access & download learning materials (PDFs, notes)
    @GetMapping("/lectures/{lectureId}/materials")
    public ResponseEntity<List<LectureMaterialResponse>> getLectureMaterials(
            @PathVariable Integer lectureId,
            Authentication authentication) {
        Integer studentId = getStudentId(authentication);
        return ResponseEntity.ok(studentLearningService.getLectureMaterials(studentId, lectureId));
    }

    // FR-S17: Toggle lecture bookmark
    @PostMapping("/lectures/{lectureId}/bookmark")
    public ResponseEntity<Map<String, Object>> toggleLectureBookmark(
            @PathVariable Integer lectureId,
            Authentication authentication) {
        Integer studentId = getStudentId(authentication);
        return ResponseEntity.ok(studentLearningService.toggleLectureBookmark(studentId, lectureId));
    }

    // FR-S17: View all bookmarked lectures
    @GetMapping("/bookmarks/lectures")
    public ResponseEntity<List<LectureBookmarkResponse>> getBookmarkedLectures(Authentication authentication) {
        Integer studentId = getStudentId(authentication);
        return ResponseEntity.ok(studentLearningService.getBookmarkedLectures(studentId));
    }

    // FR-S18-Content: Search for topics or keywords in enrolled course lectures
    @GetMapping("/courses/{courseId}/lectures/search")
    public ResponseEntity<List<LectureSearchResult>> searchCourseLectures(
            @PathVariable Integer courseId,
            @RequestParam String q,
            Authentication authentication) {
        Integer studentId = getStudentId(authentication);
        return ResponseEntity.ok(studentLearningService.searchCourseLectures(studentId, courseId, q));
    }
}