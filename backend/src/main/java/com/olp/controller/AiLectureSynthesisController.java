package com.olp.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.repository.*;
import com.olp.service.LectureDocumentTextExtractor;
import com.olp.service.LlmOrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping({"/api/instructor", "/api/instructors"})
@PreAuthorize("hasAnyRole('instructor', 'admin')")
public class AiLectureSynthesisController {

    private static final Logger log = LoggerFactory.getLogger(AiLectureSynthesisController.class);

    private final LlmOrchestrationService orchestrationService;
    private final LectureDocumentTextExtractor textExtractor;
    private final LectureNotesDocumentRepository notesDocumentRepository;
    private final AiLectureSynthesisRepository synthesisRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.upload.dir:uploads}")
    private String uploadBaseDir = "uploads";

    public AiLectureSynthesisController(LlmOrchestrationService orchestrationService,
                                        LectureDocumentTextExtractor textExtractor,
                                        LectureNotesDocumentRepository notesDocumentRepository,
                                        AiLectureSynthesisRepository synthesisRepository,
                                        LessonRepository lessonRepository,
                                        UserRepository userRepository,
                                        QuestionRepository questionRepository,
                                        ObjectMapper objectMapper) {
        this.orchestrationService = orchestrationService;
        this.textExtractor = textExtractor;
        this.notesDocumentRepository = notesDocumentRepository;
        this.synthesisRepository = synthesisRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * POST /api/instructor/lessons/{lessonId}/notes-documents
     * Ingests uploaded lecture notes (PDF, Markdown, text, PPTX), extracts text, and stores document metadata.
     */
    @PostMapping(value = "/lessons/{lessonId}/notes-documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LectureNotesDocument> uploadNotesDocument(
            @PathVariable Integer lessonId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {

        User currentUser = resolveUser(authentication);
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found with id: " + lessonId));

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty");
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "notes";
        String extension = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String uniqueName = UUID.randomUUID() + extension;

        Path targetDir = Paths.get(uploadBaseDir, "lecture-notes");
        Files.createDirectories(targetDir);
        Path targetPath = targetDir.resolve(uniqueName);

        File savedFile = targetPath.toFile();
        file.transferTo(savedFile);

        LectureDocumentTextExtractor.ExtractionResult extraction = textExtractor.extract(file, savedFile);

        LectureNotesDocument doc = new LectureNotesDocument();
        doc.setLessonId(lessonId);
        doc.setUploaderId(currentUser.getId());
        doc.setFileName(originalFilename);
        doc.setFileType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        doc.setFileSizeBytes(file.getSize());
        doc.setStoragePath(targetPath.toString());
        doc.setExtractedText(extraction.text());
        doc.setTextExtractionStatus(extraction.status());
        doc.setPageCount(extraction.pageCount());

        LectureNotesDocument saved = notesDocumentRepository.save(doc);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * GET /api/instructor/lessons/{lessonId}/notes-documents
     * Lists uploaded lecture notes documents for the lesson.
     */
    @GetMapping("/lessons/{lessonId}/notes-documents")
    public ResponseEntity<List<LectureNotesDocument>> getNotesDocuments(@PathVariable Integer lessonId) {
        List<LectureNotesDocument> docs = notesDocumentRepository.findByLessonIdOrderByCreatedAtDesc(lessonId);
        return ResponseEntity.ok(docs);
    }

    /**
     * POST /api/instructor/lessons/{lessonId}/ai-synthesis
     * Triggers the hierarchical 4-tier context pyramid assembly and AI synthesis generation.
     */
    @PostMapping("/lessons/{lessonId}/ai-synthesis")
    public ResponseEntity<AiSynthesisResponse> triggerSynthesis(
            @PathVariable Integer lessonId,
            @RequestBody(required = false) AiSynthesisRequest request,
            Authentication authentication) {

        User currentUser = resolveUser(authentication);
        AiSynthesisRequest req = request != null ? request : new AiSynthesisRequest();

        AiSynthesisResponse response = orchestrationService.generateSynthesis(lessonId, currentUser.getId(), req);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/instructor/lessons/{lessonId}/ai-synthesis
     * Retrieves the latest synthesis (draft or published) for the specified lesson.
     */
    @GetMapping("/lessons/{lessonId}/ai-synthesis")
    public ResponseEntity<AiSynthesisResponse> getLatestSynthesis(
            @PathVariable Integer lessonId,
            Authentication authentication) {

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found with id: " + lessonId));

        List<AiLectureSynthesis> syntheses = synthesisRepository.findByLessonIdOrderByCreatedAtDesc(lessonId);
        if (syntheses.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AiLectureSynthesis latest = syntheses.get(0);
        AiSynthesisResponse response = orchestrationService.mapToResponse(latest, lesson.getTitle());
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/instructor/ai-syntheses/{synthesisId}
     * Saves edits and updates review content for a synthesis draft.
     */
    @PutMapping("/ai-syntheses/{synthesisId}")
    public ResponseEntity<AiSynthesisResponse> updateSynthesisDraft(
            @PathVariable Integer synthesisId,
            @RequestBody AiSynthesisReviewRequest request,
            Authentication authentication) throws Exception {

        User currentUser = resolveUser(authentication);
        AiLectureSynthesis entity = synthesisRepository.findById(synthesisId)
                .orElseThrow(() -> new IllegalArgumentException("Synthesis not found with id: " + synthesisId));

        verifyOwnership(currentUser, entity);

        if (request.getExecutiveSummary() != null) entity.setExecutiveSummary(request.getExecutiveSummary());
        if (request.getFullStudyGuideMarkdown() != null) entity.setFullStudyGuideMarkdown(request.getFullStudyGuideMarkdown());
        if (request.getGlossary() != null) entity.setGlossaryJson(objectMapper.writeValueAsString(request.getGlossary()));
        if (request.getSpokenNuances() != null) entity.setSpokenNuancesJson(objectMapper.writeValueAsString(request.getSpokenNuances()));
        if (request.getQuizQuestions() != null) entity.setQuizQuestionsJson(objectMapper.writeValueAsString(request.getQuizQuestions()));
        if (request.getFlashcards() != null) entity.setFlashcardsJson(objectMapper.writeValueAsString(request.getFlashcards()));
        if (request.getInstructorNotes() != null) entity.setInstructorNotes(request.getInstructorNotes());

        entity.setReviewedAt(LocalDateTime.now());
        AiLectureSynthesis updated = synthesisRepository.save(entity);

        Lesson lesson = lessonRepository.findById(updated.getLessonId()).orElse(null);
        String title = lesson != null ? lesson.getTitle() : "Lesson " + updated.getLessonId();

        return ResponseEntity.ok(orchestrationService.mapToResponse(updated, title));
    }

    /**
     * POST /api/instructor/ai-syntheses/{synthesisId}/publish
     * Approves and publishes the synthesis to enrolled students (FR-I55 Human-in-the-Loop review gate).
     * Optionally exports approved questions directly into the central Question Bank.
     */
    @PostMapping("/ai-syntheses/{synthesisId}/publish")
    public ResponseEntity<AiSynthesisResponse> publishSynthesis(
            @PathVariable Integer synthesisId,
            @RequestBody(required = false) AiSynthesisPublishRequest request,
            Authentication authentication) throws Exception {

        User currentUser = resolveUser(authentication);
        AiLectureSynthesis entity = synthesisRepository.findById(synthesisId)
                .orElseThrow(() -> new IllegalArgumentException("Synthesis not found with id: " + synthesisId));

        verifyOwnership(currentUser, entity);

        AiSynthesisPublishRequest req = request != null ? request : new AiSynthesisPublishRequest();

        entity.setReviewStatus("PUBLISHED");
        entity.setPublishedToStudents(true);
        entity.setPublishedAt(LocalDateTime.now());
        entity.setReviewedAt(LocalDateTime.now());

        AiLectureSynthesis published = synthesisRepository.save(entity);

        // Optional Question Bank export
        if (Boolean.TRUE.equals(req.getExportQuestionsToQuestionBank()) &&
            published.getQuizQuestionsJson() != null && !published.getQuizQuestionsJson().isBlank()) {
            exportQuestionsToBank(published, currentUser, req.getTargetTopicTag());
        }

        Lesson lesson = lessonRepository.findById(published.getLessonId()).orElse(null);
        String title = lesson != null ? lesson.getTitle() : "Lesson " + published.getLessonId();

        return ResponseEntity.ok(orchestrationService.mapToResponse(published, title));
    }

    private void exportQuestionsToBank(AiLectureSynthesis synthesis, User instructor, String targetTopic) {
        try {
            Lesson lesson = lessonRepository.findById(synthesis.getLessonId()).orElse(null);
            Course course = (lesson != null && lesson.getSection() != null && lesson.getSection().getModule() != null)
                    ? lesson.getSection().getModule().getCourse()
                    : null;

            List<AiQuizQuestionDto> questions = objectMapper.readValue(
                    synthesis.getQuizQuestionsJson(),
                    new TypeReference<List<AiQuizQuestionDto>>() {}
            );

            for (AiQuizQuestionDto qDto : questions) {
                Question question = new Question();
                question.setCourse(course);
                question.setCreatedBy(instructor);
                question.setQuestionText(qDto.questionText());

                try {
                    question.setQuestionType(QuestionType.valueOf(qDto.questionType().toUpperCase()));
                } catch (Exception ex) {
                    question.setQuestionType(QuestionType.MULTIPLE_CHOICE);
                }

                question.setOptionsJson(objectMapper.writeValueAsString(qDto.options()));
                question.setCorrectAnswer(String.join(", ", qDto.correctAnswers()));
                question.setExplanation(qDto.explanation());
                question.setDefaultMarks(BigDecimal.ONE);
                question.setTopic(targetTopic != null && !targetTopic.isBlank() ? targetTopic : (lesson != null ? lesson.getTitle() : "AI Generated"));
                question.setDifficulty(qDto.bloomLevel() != null ? qDto.bloomLevel().toUpperCase() : "MEDIUM");

                questionRepository.save(question);
            }
            log.info("Exported {} AI quiz questions into Question Bank for synthesis {}", questions.size(), synthesis.getId());
        } catch (Exception e) {
            log.error("Failed to export AI quiz questions to question bank", e);
        }
    }

    private void verifyOwnership(User user, AiLectureSynthesis entity) {
        if (!"admin".equalsIgnoreCase(user.getRole()) && !entity.getInstructorId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to manage this synthesis draft");
        }
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AccessDeniedException("User is not authenticated");
        }
        if (authentication.getPrincipal() instanceof User u) {
            return u;
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("User not found: " + authentication.getName()));
    }
}
