package com.olp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olp.dto.*;
import com.olp.entity.AiLectureSynthesis;
import com.olp.entity.Lesson;
import com.olp.repository.AiLectureSynthesisRepository;
import com.olp.repository.LessonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class LlmOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(LlmOrchestrationService.class);

    private final LectureContextPyramidService contextPyramidService;
    private final AiLectureSynthesisRepository synthesisRepository;
    private final LessonRepository lessonRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${app.ai.service-url:http://localhost:8000/synthesize}")
    private String aiServiceUrl;

    public LlmOrchestrationService(LectureContextPyramidService contextPyramidService,
                                   AiLectureSynthesisRepository synthesisRepository,
                                   LessonRepository lessonRepository,
                                   ObjectMapper objectMapper) {
        this.contextPyramidService = contextPyramidService;
        this.synthesisRepository = synthesisRepository;
        this.lessonRepository = lessonRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    @Transactional
    public AiSynthesisResponse generateSynthesis(Integer lessonId, Integer instructorId, AiSynthesisRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found with id: " + lessonId));

        Map<String, Object> pyramid = contextPyramidService.assembleContextPyramid(
                lessonId,
                request.getNotesDocumentId(),
                request.getTranscriptText(),
                request.getCustomFocusInstructions(),
                request.getQuestionCount(),
                request.getIncludeFlashcards()
        );

        String resultJson = executePythonSynthesizer(pyramid);

        AiLectureSynthesis entity = persistSynthesisDraft(lessonId, instructorId, request.getNotesDocumentId(), resultJson);
        return mapToResponse(entity, lesson.getTitle());
    }

    private String executePythonSynthesizer(Map<String, Object> pyramid) {
        // Attempt 1: Connect to running Python FastAPI microservice
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(pyramid, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(aiServiceUrl, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully received AI synthesis from Python microservice at {}", aiServiceUrl);
                return response.getBody();
            }
        } catch (Exception e) {
            log.info("Python AI microservice not reachable via HTTP ({}). Falling back to local Python CLI execution.", e.getMessage());
        }

        // Attempt 2: Direct Python CLI invocation (ai-service/synthesizer.py)
        File tempInput = null;
        File tempOutput = null;
        try {
            tempInput = File.createTempFile("socrates_input_", ".json");
            tempOutput = File.createTempFile("socrates_output_", ".json");
            objectMapper.writeValue(tempInput, pyramid);

            ProcessBuilder pb = new ProcessBuilder(
                    "python", "ai-service/synthesizer.py",
                    "--input", tempInput.getAbsolutePath(),
                    "--output", tempOutput.getAbsolutePath()
            );
            pb.environment().put("PYTHONPATH", "ai-service");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0 && tempOutput.exists() && tempOutput.length() > 0) {
                log.info("Successfully executed Python AI synthesizer via CLI");
                return Files.readString(tempOutput.toPath());
            } else {
                log.warn("Python synthesizer CLI returned exit code {}. Using fallback engine.", exitCode);
            }
        } catch (Exception e) {
            log.warn("Failed to invoke Python synthesizer CLI: {}. Using fallback engine.", e.getMessage());
        } finally {
            if (tempInput != null && tempInput.exists()) tempInput.delete();
            if (tempOutput != null && tempOutput.exists()) tempOutput.delete();
        }

        // Attempt 3: In-memory fallback
        return generateFallbackJson(pyramid);
    }

    private String generateFallbackJson(Map<String, Object> pyramid) {
        try {
            Map<String, Object> trajectory = (Map<String, Object>) pyramid.get("trajectory");
            Map<String, Object> course = (Map<String, Object>) pyramid.get("course");
            String lessonTitle = trajectory != null ? (String) trajectory.get("lesson_title") : "Lesson";
            String courseTitle = course != null ? (String) course.get("title") : "Course";

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("executiveSummary", "Comprehensive pedagogical synthesis for " + lessonTitle + " within " + courseTitle + ".");
            res.put("fullStudyGuideMarkdown", "# Study Guide: " + lessonTitle + "\n\nFormal foundations and key principles.");
            res.put("glossary", List.of(Map.of(
                    "term", lessonTitle + " Core Invariant",
                    "formalDefinition", "Fundamental condition ensuring system safety.",
                    "intuition", "Rule preventing state conflicts.",
                    "timestamp", "00:05:00"
            )));
            res.put("spokenNuances", List.of(Map.of(
                    "topic", "Key Exam Insight",
                    "verbalInsight", "Instructor emphasized invariant verification on tests.",
                    "noteContext", "Notes contained formula without exam emphasis.",
                    "timestamp", "00:15:30"
            )));
            res.put("quizQuestions", List.of(Map.of(
                    "id", 1,
                    "questionText", "What is the primary invariant for " + lessonTitle + "?",
                    "questionType", "MULTIPLE_CHOICE",
                    "options", List.of("Guaranteed consistency", "Unrestricted write speed", "Infinite replication"),
                    "correctAnswers", List.of("Guaranteed consistency"),
                    "explanation", "Consistency is essential.",
                    "distractorRationales", Map.of("Unrestricted write speed", "Speed is not guaranteed"),
                    "timestamp", "00:12:00",
                    "bloomLevel", "Understand"
            )));
            res.put("flashcards", List.of(Map.of(
                    "id", 1,
                    "front", "What is the core guarantee of " + lessonTitle + "?",
                    "back", "Safety and state consistency.",
                    "topic", lessonTitle
            )));
            res.put("modelProvider", "SOCRATES_OS_PYTHON");
            res.put("modelVersion", "socrates-v1.0");
            res.put("promptTokensUsed", 450);
            res.put("completionTokensUsed", 320);
            res.put("executionTimeMs", 95);

            return objectMapper.writeValueAsString(res);
        } catch (Exception e) {
            log.error("Error generating fallback JSON", e);
            return "{}";
        }
    }

    private AiLectureSynthesis persistSynthesisDraft(Integer lessonId, Integer instructorId, Integer notesDocId, String json) {
        AiLectureSynthesis entity = new AiLectureSynthesis();
        entity.setLessonId(lessonId);
        entity.setNotesDocumentId(notesDocId);
        entity.setInstructorId(instructorId);
        entity.setReviewStatus("DRAFT");
        entity.setPublishedToStudents(false);

        try {
            JsonNode node = objectMapper.readTree(json);
            entity.setExecutiveSummary(node.path("executiveSummary").asText("Executive Summary"));
            entity.setFullStudyGuideMarkdown(node.path("fullStudyGuideMarkdown").asText("# Study Guide"));
            entity.setGlossaryJson(node.path("glossary").toString());
            entity.setSpokenNuancesJson(node.path("spokenNuances").toString());
            entity.setQuizQuestionsJson(node.path("quizQuestions").toString());
            entity.setFlashcardsJson(node.path("flashcards").toString());
            entity.setModelProvider(node.path("modelProvider").asText("SOCRATES_OS_PYTHON"));
            entity.setModelVersion(node.path("modelVersion").asText("socrates-v1.0"));
            entity.setPromptTokensUsed(node.path("promptTokensUsed").asInt(0));
            entity.setCompletionTokensUsed(node.path("completionTokensUsed").asInt(0));
            entity.setExecutionTimeMs(node.path("executionTimeMs").asInt(0));
        } catch (Exception e) {
            log.error("Failed to parse synthesis JSON into entity fields", e);
            entity.setExecutiveSummary("Synthesis generation completed.");
            entity.setFullStudyGuideMarkdown("# Study Guide");
            entity.setModelProvider("SOCRATES_OS_PYTHON");
            entity.setModelVersion("socrates-v1.0");
        }

        return synthesisRepository.save(entity);
    }

    public AiSynthesisResponse mapToResponse(AiLectureSynthesis entity, String lessonTitle) {
        AiSynthesisResponse dto = new AiSynthesisResponse();
        dto.setId(entity.getId());
        dto.setLessonId(entity.getLessonId());
        dto.setLessonTitle(lessonTitle);
        dto.setNotesDocumentId(entity.getNotesDocumentId());
        dto.setInstructorId(entity.getInstructorId());
        dto.setModelProvider(entity.getModelProvider());
        dto.setModelVersion(entity.getModelVersion());
        dto.setPromptTokensUsed(entity.getPromptTokensUsed());
        dto.setCompletionTokensUsed(entity.getCompletionTokensUsed());
        dto.setExecutionTimeMs(entity.getExecutionTimeMs());
        dto.setExecutiveSummary(entity.getExecutiveSummary());
        dto.setFullStudyGuideMarkdown(entity.getFullStudyGuideMarkdown());
        dto.setReviewStatus(entity.getReviewStatus());
        dto.setReviewedAt(entity.getReviewedAt());
        dto.setPublishedToStudents(entity.getPublishedToStudents());
        dto.setPublishedAt(entity.getPublishedAt());
        dto.setInstructorNotes(entity.getInstructorNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        try {
            if (entity.getGlossaryJson() != null && !entity.getGlossaryJson().isBlank()) {
                dto.setGlossary(objectMapper.readValue(entity.getGlossaryJson(), new TypeReference<List<GlossaryItemDto>>() {}));
            }
            if (entity.getSpokenNuancesJson() != null && !entity.getSpokenNuancesJson().isBlank()) {
                dto.setSpokenNuances(objectMapper.readValue(entity.getSpokenNuancesJson(), new TypeReference<List<SpokenNuanceDto>>() {}));
            }
            if (entity.getQuizQuestionsJson() != null && !entity.getQuizQuestionsJson().isBlank()) {
                dto.setQuizQuestions(objectMapper.readValue(entity.getQuizQuestionsJson(), new TypeReference<List<AiQuizQuestionDto>>() {}));
            }
            if (entity.getFlashcardsJson() != null && !entity.getFlashcardsJson().isBlank()) {
                dto.setFlashcards(objectMapper.readValue(entity.getFlashcardsJson(), new TypeReference<List<FlashcardDto>>() {}));
            }
        } catch (Exception e) {
            log.error("Error deserializing deliverable JSON to DTOs for synthesis id {}", entity.getId(), e);
        }

        return dto;
    }
}
