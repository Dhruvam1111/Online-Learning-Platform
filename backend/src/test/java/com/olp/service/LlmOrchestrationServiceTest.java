package com.olp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olp.dto.AiSynthesisRequest;
import com.olp.dto.AiSynthesisResponse;
import com.olp.entity.AiLectureSynthesis;
import com.olp.entity.Lesson;
import com.olp.repository.AiLectureSynthesisRepository;
import com.olp.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmOrchestrationServiceTest {

    @Mock
    private LectureContextPyramidService contextPyramidService;

    @Mock
    private AiLectureSynthesisRepository synthesisRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private LlmOrchestrationService orchestrationService;

    private Lesson lesson;

    @BeforeEach
    void setUp() {
        lesson = new Lesson();
        lesson.setId(101);
        lesson.setTitle("Consensus State Machines");
    }

    @Test
    void testGenerateSynthesis_Success() {
        when(lessonRepository.findById(101)).thenReturn(Optional.of(lesson));

        Map<String, Object> pyramid = Map.of(
                "course", Map.of("title", "Distributed Systems"),
                "trajectory", Map.of("lesson_title", "Consensus State Machines", "lesson_index", 5, "total_lessons_in_course", 20),
                "transcript_text", "00:02:10 - Welcome to consensus state machines.",
                "extracted_notes_text", "- Invariants and safety guarantees."
        );
        when(contextPyramidService.assembleContextPyramid(any(), any(), any(), any(), any(), any()))
                .thenReturn(pyramid);

        when(synthesisRepository.save(any(AiLectureSynthesis.class))).thenAnswer(invocation -> {
            AiLectureSynthesis entity = invocation.getArgument(0);
            entity.setId(555);
            return entity;
        });

        AiSynthesisRequest req = new AiSynthesisRequest();
        req.setNotesDocumentId(202);
        req.setQuestionCount(3);

        AiSynthesisResponse response = orchestrationService.generateSynthesis(101, 77, req);

        assertNotNull(response);
        assertEquals(555, response.getId());
        assertEquals("Consensus State Machines", response.getLessonTitle());
        assertEquals("DRAFT", response.getReviewStatus());
        assertFalse(response.getPublishedToStudents());
        assertNotNull(response.getExecutiveSummary());
        assertNotNull(response.getFullStudyGuideMarkdown());
    }
}
