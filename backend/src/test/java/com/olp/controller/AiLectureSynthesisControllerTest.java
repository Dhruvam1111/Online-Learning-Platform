package com.olp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.repository.*;
import com.olp.service.LectureDocumentTextExtractor;
import com.olp.service.LlmOrchestrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiLectureSynthesisControllerTest {

    @Mock
    private LlmOrchestrationService orchestrationService;

    @Mock
    private LectureDocumentTextExtractor textExtractor;

    @Mock
    private LectureNotesDocumentRepository notesDocumentRepository;

    @Mock
    private AiLectureSynthesisRepository synthesisRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private Authentication authentication;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AiLectureSynthesisController controller;

    private User instructor;
    private Lesson lesson;
    private AiLectureSynthesis draftSynthesis;

    @BeforeEach
    void setUp() {
        instructor = new User();
        instructor.setId(10);
        instructor.setEmail("instructor@olp.com");
        instructor.setRole("instructor");

        lesson = new Lesson();
        lesson.setId(20);
        lesson.setTitle("Consensus Algorithms");

        draftSynthesis = new AiLectureSynthesis();
        draftSynthesis.setId(30);
        draftSynthesis.setLessonId(20);
        draftSynthesis.setInstructorId(10);
        draftSynthesis.setReviewStatus("DRAFT");
        draftSynthesis.setExecutiveSummary("Draft summary");
        draftSynthesis.setFullStudyGuideMarkdown("# Draft guide");
        draftSynthesis.setQuizQuestionsJson("[{\"id\":1,\"questionText\":\"What is consensus?\",\"questionType\":\"MULTIPLE_CHOICE\",\"options\":[\"Agreement\"],\"correctAnswers\":[\"Agreement\"],\"explanation\":\"Core concept\",\"distractorRationales\":{},\"timestamp\":\"00:01:00\",\"bloomLevel\":\"Understand\"}]");
    }

    @Test
    void testUploadNotesDocument_Success() throws Exception {
        when(authentication.getPrincipal()).thenReturn(instructor);
        when(lessonRepository.findById(20)).thenReturn(Optional.of(lesson));
        when(textExtractor.extract(any(), any())).thenReturn(new LectureDocumentTextExtractor.ExtractionResult("Extracted text", 3, "COMPLETED"));
        when(notesDocumentRepository.save(any(LectureNotesDocument.class))).thenAnswer(i -> {
            LectureNotesDocument doc = i.getArgument(0);
            doc.setId(100);
            return doc;
        });

        MockMultipartFile file = new MockMultipartFile("file", "lecture1.pdf", "application/pdf", "dummy pdf content".getBytes());

        ResponseEntity<LectureNotesDocument> res = controller.uploadNotesDocument(20, file, authentication);
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals("lecture1.pdf", res.getBody().getFileName());
        assertEquals("Extracted text", res.getBody().getExtractedText());
    }

    @Test
    void testTriggerSynthesis_Success() {
        when(authentication.getPrincipal()).thenReturn(instructor);

        AiSynthesisResponse expectedResponse = new AiSynthesisResponse();
        expectedResponse.setId(30);
        expectedResponse.setLessonId(20);
        expectedResponse.setLessonTitle("Consensus Algorithms");
        expectedResponse.setReviewStatus("DRAFT");

        when(orchestrationService.generateSynthesis(eq(20), eq(10), any(AiSynthesisRequest.class)))
                .thenReturn(expectedResponse);

        AiSynthesisRequest req = new AiSynthesisRequest();
        ResponseEntity<AiSynthesisResponse> res = controller.triggerSynthesis(20, req, authentication);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals(30, res.getBody().getId());
        assertEquals("DRAFT", res.getBody().getReviewStatus());
    }

    @Test
    void testGetLatestSynthesis_Found() {
        when(lessonRepository.findById(20)).thenReturn(Optional.of(lesson));
        when(synthesisRepository.findByLessonIdOrderByCreatedAtDesc(20)).thenReturn(List.of(draftSynthesis));

        AiSynthesisResponse expectedResponse = new AiSynthesisResponse();
        expectedResponse.setId(30);
        expectedResponse.setLessonId(20);
        expectedResponse.setLessonTitle("Consensus Algorithms");

        when(orchestrationService.mapToResponse(draftSynthesis, "Consensus Algorithms")).thenReturn(expectedResponse);

        ResponseEntity<AiSynthesisResponse> res = controller.getLatestSynthesis(20, authentication);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(30, res.getBody().getId());
    }

    @Test
    void testPublishSynthesis_SuccessWithQuestionExport() throws Exception {
        when(authentication.getPrincipal()).thenReturn(instructor);
        when(synthesisRepository.findById(30)).thenReturn(Optional.of(draftSynthesis));
        when(synthesisRepository.save(any())).thenReturn(draftSynthesis);
        when(lessonRepository.findById(20)).thenReturn(Optional.of(lesson));

        AiSynthesisResponse publishedResponse = new AiSynthesisResponse();
        publishedResponse.setId(30);
        publishedResponse.setReviewStatus("PUBLISHED");
        publishedResponse.setPublishedToStudents(true);

        when(orchestrationService.mapToResponse(any(), any())).thenReturn(publishedResponse);

        AiSynthesisPublishRequest pubReq = new AiSynthesisPublishRequest();
        pubReq.setExportQuestionsToQuestionBank(true);
        pubReq.setTargetTopicTag("Distributed Systems");

        ResponseEntity<AiSynthesisResponse> res = controller.publishSynthesis(30, pubReq, authentication);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertTrue(res.getBody().getPublishedToStudents());
        assertEquals("PUBLISHED", res.getBody().getReviewStatus());

        verify(questionRepository, atLeastOnce()).save(any(Question.class));
    }
}
