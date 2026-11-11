package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.service.CourseStructureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller exposing REST APIs for Course Structure Management (FR-I06).
 * Handles Course → Module → Section → Lesson hierarchy CRUD and reordering.
 */
@RestController
@RequestMapping({"/api/instructor/courses/{courseId}", "/api/instructors/courses/{courseId}"})
@PreAuthorize("hasRole('instructor') or hasRole('admin')")
public class CourseStructureController {

    private final CourseStructureService courseStructureService;

    public CourseStructureController(CourseStructureService courseStructureService) {
        this.courseStructureService = courseStructureService;
    }

    // =========================================================================
    // FULL STRUCTURE TREE
    // =========================================================================

    @GetMapping("/structure")
    public ResponseEntity<CourseStructureResponse> getCourseStructure(
            @PathVariable Integer courseId,
            Authentication authentication) {
        User currentUser = courseStructureService.resolveUser(authentication);
        CourseStructureResponse response = courseStructureService.getCourseStructure(currentUser, courseId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // MODULES
    // =========================================================================

    @PostMapping("/modules")
    public ResponseEntity<CourseModuleResponse> createModule(
            @PathVariable Integer courseId,
            @Valid @RequestBody CourseModuleRequest request,
            Authentication authentication) {
        User currentUser = courseStructureService.resolveUser(authentication);
        CourseModuleResponse response = courseStructureService.createModule(currentUser, courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/modules")
    public ResponseEntity<List<CourseModuleResponse>> getModules(
            @PathVariable Integer courseId,
            Authentication authentication) {
        User currentUser = courseStructureService.resolveUser(authentication);
        List<CourseModuleResponse> response = courseStructureService.getModules(currentUser, courseId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/modules/{moduleId}")
    public ResponseEntity<CourseModuleResponse> getModule(
            @PathVariable Integer courseId,
            @PathVariable Integer moduleId,
            Authentication authentication) {
        User currentUser = courseStructureService.resolveUser(authentication);
        CourseModuleResponse response = courseStructureService.getModule(currentUser, courseId, moduleId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/modules/{moduleId}")
    public ResponseEntity<CourseModuleResponse> updateModule(
            @PathVariable Integer courseId,
            @PathVariable Integer moduleId,
            @Valid @RequestBody CourseModuleRequest request,
            Authentication authentication) {
        User currentUser = courseStructureService.resolveUser(authentication);
        CourseModuleResponse response = courseStructureService.updateModule(currentUser, courseId, moduleId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/modules/{moduleId}")
    public ResponseEntity<ApiResponse> deleteModule(
            @PathVariable Integer courseId,
            @PathVariable Integer moduleId,
            Authentication authentication) {
        User currentUser = courseStructureService.resolveUser(authentication);
        ApiResponse response = courseStructureService.deleteModule(currentUser, courseId, moduleId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/modules/reorder")
    public ResponseEntity<List<CourseModuleResponse>> reorderModules(
            @PathVariable Integer courseId,
            @Valid @RequestBody ReorderStructureRequest request,
            Authentication authentication) {
        User currentUser = courseStructureService.resolveUser(authentication);
        List<CourseModuleResponse> response = courseStructureService.reorderModules(currentUser, courseId, request);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // SECTIONS
    // =========================================================================

    @PostMapping("/modules/{moduleId}/sections")
    public ResponseEntity<CourseSectionResponse> createSection(
            @PathVariable Integer courseId,
            @PathVariable Integer moduleId,
            @Valid @RequestBody CourseSectionRequest request,
            Authentication authentication) {
        User currentUser = courseStructureService.resolveUser(authentication);
        CourseSectionResponse response = courseStructureService.createSection(currentUser, courseId, moduleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/modules/{moduleId}/sections")
    public ResponseEntity<List<CourseSectionResponse>> getSections(
            @PathVariable Integer courseId,
            @PathVariable Integer moduleId,
            Authentication authentication) {
        User currentUser = courseStructureService.resolveUser(authentication);
        List<CourseSectionResponse> response = courseStructureService.getSections(currentUser, courseId, moduleId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/modules/{moduleId}/sections/{sectionId}")
    public ResponseEntity<CourseSectionResponse> getSection(
            @PathVariable Integer courseId,
            @PathVariable Integer moduleId,
            @PathVariable Integer sectionId,
            Authentication authentication) {
        User currentUser = courseStructureService.resolveUser(authentication);
        CourseSectionResponse response = courseStructureService.getSection(currentUser, courseId, moduleId, sectionId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/modules/{moduleId}/sections/{sectionId}")
    public ResponseEntity<CourseSectionResponse> updateSection(
            @PathVariable Integer courseId,
            @PathVariable Integer moduleId,
            @PathVariable Integer sectionId,
            @Valid @RequestBody CourseSectionRequest request,
            Authentication authentication) {
        User currentUser = courseStructureService.resolveUser(authentication);
        CourseSectionResponse response = courseStructureService.updateSection(currentUser, courseId, moduleId, sectionId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/modules/{moduleId}/sections/{sectionId}")
    public ResponseEntity<ApiResponse> deleteSection(
            @PathVariable Integer courseId,
            @PathVariable Integer moduleId,
            @PathVariable Integer sectionId,
            Authentication authentication) {
        User currentUser = courseStructureService.resolveUser(authentication);
        ApiResponse response = courseStructureService.deleteSection(currentUser, courseId, moduleId, sectionId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/modules/{moduleId}/sections/reorder")
    public ResponseEntity<List<CourseSectionResponse>> reorderSections(
            @PathVariable Integer courseId,
            @PathVariable Integer moduleId,
            @Valid @RequestBody ReorderStructureRequest request,
            Authentication authentication) {
        User currentUser = courseStructureService.resolveUser(authentication);
        List<CourseSectionResponse> response = courseStructureService.reorderSections(currentUser, courseId, moduleId, request);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // LESSONS
    // =========================================================================

    @PostMapping("/modules/{moduleId}/sections/{sectionId}/lessons")
    public ResponseEntity<LessonResponse> createLesson(
            @PathVariable Integer courseId,
            @PathVariable Integer moduleId,
            @PathVariable Integer sectionId,
            @Valid @RequestBody LessonRequest request,
            Authentication authentication) {
        User currentUser = courseStructureService.resolveUser(authentication);
        LessonResponse response = courseStructureService.createLesson(currentUser, courseId, moduleId, sectionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/modules/{moduleId}/sections/{sectionId}/lessons")
    public ResponseEntity<List<LessonResponse>> getLessons(
            @PathVariable Integer courseId,
            @PathVariable Integer moduleId,
            @PathVariable Integer sectionId,
            Authentication authentication) {
        User currentUser = courseStructureService.resolveUser(authentication);
        List<LessonResponse> response = courseStructureService.getLessons(currentUser, courseId, moduleId, sectionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/modules/{moduleId}/sections/{sectionId}/lessons/{lessonId}")
    public ResponseEntity<LessonResponse> getLesson(
            @PathVariable Integer courseId,
            @PathVariable Integer moduleId,
            @PathVariable Integer sectionId,
            @PathVariable Integer lessonId,
            Authentication authentication) {
        User currentUser = courseStructureService.resolveUser(authentication);
        LessonResponse response = courseStructureService.getLesson(currentUser, courseId, moduleId, sectionId, lessonId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/modules/{moduleId}/sections/{sectionId}/lessons/{lessonId}")
    public ResponseEntity<LessonResponse> updateLesson(
            @PathVariable Integer courseId,
            @PathVariable Integer moduleId,
            @PathVariable Integer sectionId,
            @PathVariable Integer lessonId,
            @Valid @RequestBody LessonRequest request,
            Authentication authentication) {
        User currentUser = courseStructureService.resolveUser(authentication);
        LessonResponse response = courseStructureService.updateLesson(currentUser, courseId, moduleId, sectionId, lessonId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/modules/{moduleId}/sections/{sectionId}/lessons/{lessonId}")
    public ResponseEntity<ApiResponse> deleteLesson(
            @PathVariable Integer courseId,
            @PathVariable Integer moduleId,
            @PathVariable Integer sectionId,
            @PathVariable Integer lessonId,
            Authentication authentication) {
        User currentUser = courseStructureService.resolveUser(authentication);
        ApiResponse response = courseStructureService.deleteLesson(currentUser, courseId, moduleId, sectionId, lessonId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/modules/{moduleId}/sections/{sectionId}/lessons/reorder")
    public ResponseEntity<List<LessonResponse>> reorderLessons(
            @PathVariable Integer courseId,
            @PathVariable Integer moduleId,
            @PathVariable Integer sectionId,
            @Valid @RequestBody ReorderStructureRequest request,
            Authentication authentication) {
        User currentUser = courseStructureService.resolveUser(authentication);
        List<LessonResponse> response = courseStructureService.reorderLessons(currentUser, courseId, moduleId, sectionId, request);
        return ResponseEntity.ok(response);
    }
}

