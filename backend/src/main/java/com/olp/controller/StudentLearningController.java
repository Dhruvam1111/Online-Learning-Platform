package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.service.StudentLearningService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
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

    // FR-S14: Access learning materials (PDFs, notes) for a lecture
    @GetMapping("/lectures/{lectureId}/materials")
    public ResponseEntity<List<LectureMaterialResponse>> getLectureMaterials(
            @PathVariable Integer lectureId,
            Authentication authentication) {
        Integer studentId = getStudentId(authentication);
        return ResponseEntity.ok(studentLearningService.getLectureMaterials(studentId, lectureId));
    }

    // FR-S14, FR-I17: Access learning materials attached to a lesson
    @GetMapping("/lessons/{lessonId}/materials")
    public ResponseEntity<List<LectureMaterialResponse>> getLessonMaterials(
            @PathVariable Integer lessonId,
            Authentication authentication) {
        Integer studentId = getStudentId(authentication);
        return ResponseEntity.ok(studentLearningService.getLessonMaterials(studentId, lessonId));
    }

    // FR-I18: Get lesson details for enrolled student with release gating
    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<LessonResponse> getLesson(
            @PathVariable Integer lessonId,
            Authentication authentication) {
        Integer studentId = getStudentId(authentication);
        return ResponseEntity.ok(studentLearningService.getLessonForStudent(studentId, lessonId));
    }

    // FR-S14, FR-I17: Access learning materials attached to a module
    @GetMapping("/modules/{moduleId}/materials")
    public ResponseEntity<List<LectureMaterialResponse>> getModuleMaterials(
            @PathVariable Integer moduleId,
            Authentication authentication) {
        Integer studentId = getStudentId(authentication);
        return ResponseEntity.ok(studentLearningService.getModuleMaterials(studentId, moduleId));
    }

    // FR-I17: Download material endpoint (enforces active enrollment & isDownloadable = true)
    @GetMapping("/materials/{materialId}/download")
    public ResponseEntity<Resource> downloadMaterial(
            @PathVariable Integer materialId,
            Authentication authentication) {
        Integer studentId = getStudentId(authentication);
        ResourceAndMetadata data = studentLearningService.downloadMaterial(studentId, materialId);

        String contentType = data.getContentType();
        String contentDisposition = "attachment; filename=\"" + data.getFilename() + "\"";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(data.getResource());
    }

    // FR-I17: View material endpoint (embedded viewer streaming; works for view-only materials)
    @GetMapping({"/materials/{materialId}/view", "/materials/{materialId}/stream"})
    public ResponseEntity<Resource> viewMaterial(
            @PathVariable Integer materialId,
            Authentication authentication) {
        Integer studentId = getStudentId(authentication);
        ResourceAndMetadata data = studentLearningService.viewMaterial(studentId, materialId);

        String contentType = data.getContentType();
        String contentDisposition = "inline; filename=\"" + data.getFilename() + "\"";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(data.getResource());
    }

    // FR-I17: Material viewer metadata (does not expose direct download URL for view-only files)
    @GetMapping("/materials/{materialId}/viewer")
    public ResponseEntity<MaterialViewerResponse> getMaterialViewerInfo(
            @PathVariable Integer materialId,
            Authentication authentication) {
        Integer studentId = getStudentId(authentication);
        MaterialViewerResponse viewer = studentLearningService.getMaterialViewerInfo(studentId, materialId);
        return ResponseEntity.ok(viewer);
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