package com.olp.controller;

import com.olp.dto.AiSynthesisResponse;
import com.olp.entity.AiLectureSynthesis;
import com.olp.entity.Lesson;
import com.olp.repository.AiLectureSynthesisRepository;
import com.olp.repository.LessonRepository;
import com.olp.service.LlmOrchestrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/student", "/api/students"})
@PreAuthorize("hasAnyRole('student', 'admin', 'instructor')")
public class StudentStudyGuideController {

    private final AiLectureSynthesisRepository synthesisRepository;
    private final LessonRepository lessonRepository;
    private final LlmOrchestrationService orchestrationService;

    public StudentStudyGuideController(AiLectureSynthesisRepository synthesisRepository,
                                       LessonRepository lessonRepository,
                                       LlmOrchestrationService orchestrationService) {
        this.synthesisRepository = synthesisRepository;
        this.lessonRepository = lessonRepository;
        this.orchestrationService = orchestrationService;
    }

    /**
     * GET /api/student/lessons/{lessonId}/study-guide
     * Read-only access to published study guide, glossary, and flashcards for students.
     * Enforces strict review gating: if no published synthesis exists, returns 404.
     */
    @GetMapping("/lessons/{lessonId}/study-guide")
    public ResponseEntity<AiSynthesisResponse> getPublishedStudyGuide(@PathVariable Integer lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found with id: " + lessonId));

        List<AiLectureSynthesis> publishedList = synthesisRepository.findPublishedByLessonId(lessonId);
        if (publishedList.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AiLectureSynthesis published = publishedList.get(0);
        AiSynthesisResponse response = orchestrationService.mapToResponse(published, lesson.getTitle());
        return ResponseEntity.ok(response);
    }
}
