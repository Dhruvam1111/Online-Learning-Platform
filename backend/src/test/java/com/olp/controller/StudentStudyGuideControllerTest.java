package com.olp.controller;

import com.olp.dto.AiSynthesisResponse;
import com.olp.entity.AiLectureSynthesis;
import com.olp.entity.Lesson;
import com.olp.repository.AiLectureSynthesisRepository;
import com.olp.repository.LessonRepository;
import com.olp.service.LlmOrchestrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentStudyGuideControllerTest {

    @Mock
    private AiLectureSynthesisRepository synthesisRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LlmOrchestrationService orchestrationService;

    @InjectMocks
    private StudentStudyGuideController controller;

    private Lesson lesson;
    private AiLectureSynthesis publishedSynthesis;

    @BeforeEach
    void setUp() {
        lesson = new Lesson();
        lesson.setId(100);
        lesson.setTitle("Raft Protocol");

        publishedSynthesis = new AiLectureSynthesis();
        publishedSynthesis.setId(50);
        publishedSynthesis.setLessonId(100);
        publishedSynthesis.setPublishedToStudents(true);
        publishedSynthesis.setReviewStatus("PUBLISHED");
    }

    @Test
    void testGetPublishedStudyGuide_Found() {
        when(lessonRepository.findById(100)).thenReturn(Optional.of(lesson));
        when(synthesisRepository.findPublishedByLessonId(100)).thenReturn(List.of(publishedSynthesis));

        AiSynthesisResponse expected = new AiSynthesisResponse();
        expected.setId(50);
        expected.setLessonTitle("Raft Protocol");
        expected.setPublishedToStudents(true);

        when(orchestrationService.mapToResponse(publishedSynthesis, "Raft Protocol")).thenReturn(expected);

        ResponseEntity<AiSynthesisResponse> res = controller.getPublishedStudyGuide(100);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals(50, res.getBody().getId());
        assertTrue(res.getBody().getPublishedToStudents());
    }

    @Test
    void testGetPublishedStudyGuide_NotFoundWhenUnpublished() {
        when(lessonRepository.findById(100)).thenReturn(Optional.of(lesson));
        when(synthesisRepository.findPublishedByLessonId(100)).thenReturn(List.of());

        ResponseEntity<AiSynthesisResponse> res = controller.getPublishedStudyGuide(100);
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }
}
