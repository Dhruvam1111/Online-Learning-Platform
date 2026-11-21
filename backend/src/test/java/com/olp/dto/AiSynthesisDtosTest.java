package com.olp.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AiSynthesisDtosTest {

    @Test
    void testAiSynthesisRequest() {
        AiSynthesisRequest req = new AiSynthesisRequest();
        req.setNotesDocumentId(10);
        req.setTranscriptText("00:01:00 - Intro");
        req.setPreferredModel("OPENAI_GPT");
        req.setTemperature(0.5);
        req.setIncludeQuizQuestions(true);
        req.setQuestionCount(8);
        req.setIncludeFlashcards(true);
        req.setCustomFocusInstructions("Focus on edge cases");

        assertEquals(10, req.getNotesDocumentId());
        assertEquals("00:01:00 - Intro", req.getTranscriptText());
        assertEquals("OPENAI_GPT", req.getPreferredModel());
        assertEquals(0.5, req.getTemperature());
        assertTrue(req.getIncludeQuizQuestions());
        assertEquals(8, req.getQuestionCount());
        assertTrue(req.getIncludeFlashcards());
        assertEquals("Focus on edge cases", req.getCustomFocusInstructions());
    }

    @Test
    void testAiSynthesisPublishAndReviewRequests() {
        AiSynthesisPublishRequest pub = new AiSynthesisPublishRequest();
        pub.setPublishStudyGuide(true);
        pub.setExportQuestionsToQuestionBank(true);
        pub.setTargetTopicTag("Raft Consensus");
        pub.setPublishFlashcards(true);

        assertTrue(pub.getPublishStudyGuide());
        assertTrue(pub.getExportQuestionsToQuestionBank());
        assertEquals("Raft Consensus", pub.getTargetTopicTag());
        assertTrue(pub.getPublishFlashcards());

        AiSynthesisReviewRequest rev = new AiSynthesisReviewRequest();
        rev.setExecutiveSummary("Updated summary");
        rev.setFullStudyGuideMarkdown("# Updated guide");
        rev.setInstructorNotes("Approved by instructor");
        rev.setGlossary(List.of(new GlossaryItemDto("Term", "Def", "Intuition", "00:01:00")));
        rev.setSpokenNuances(List.of(new SpokenNuanceDto("Topic", "Insight", "Note", "00:02:00")));
        rev.setQuizQuestions(List.of(new AiQuizQuestionDto(1, "Q?", "MULTIPLE_CHOICE", List.of("A"), List.of("A"), "Exp", Map.of(), "00:01:00", "Understand")));
        rev.setFlashcards(List.of(new FlashcardDto(1, "Front", "Back", "Topic")));

        assertEquals("Updated summary", rev.getExecutiveSummary());
        assertEquals("# Updated guide", rev.getFullStudyGuideMarkdown());
        assertEquals("Approved by instructor", rev.getInstructorNotes());
        assertEquals(1, rev.getGlossary().size());
        assertEquals(1, rev.getSpokenNuances().size());
        assertEquals(1, rev.getQuizQuestions().size());
        assertEquals(1, rev.getFlashcards().size());
    }

    @Test
    void testAiSynthesisResponse() {
        AiSynthesisResponse resp = new AiSynthesisResponse();
        resp.setId(100);
        resp.setLessonId(200);
        resp.setLessonTitle("Distributed Transactions");
        resp.setNotesDocumentId(300);
        resp.setInstructorId(400);
        resp.setModelProvider("SOCRATES_OS_PYTHON");
        resp.setModelVersion("v1.0");
        resp.setPromptTokensUsed(500);
        resp.setCompletionTokensUsed(400);
        resp.setExecutionTimeMs(150);
        resp.setExecutiveSummary("Exec summary");
        resp.setFullStudyGuideMarkdown("# Markdown Guide");
        resp.setReviewStatus("PUBLISHED");
        LocalDateTime now = LocalDateTime.now();
        resp.setReviewedAt(now);
        resp.setPublishedToStudents(true);
        resp.setPublishedAt(now);
        resp.setInstructorNotes("Notes");
        resp.setCreatedAt(now);
        resp.setUpdatedAt(now);

        assertEquals(100, resp.getId());
        assertEquals(200, resp.getLessonId());
        assertEquals("Distributed Transactions", resp.getLessonTitle());
        assertEquals(300, resp.getNotesDocumentId());
        assertEquals(400, resp.getInstructorId());
        assertEquals("SOCRATES_OS_PYTHON", resp.getModelProvider());
        assertEquals("v1.0", resp.getModelVersion());
        assertEquals(500, resp.getPromptTokensUsed());
        assertEquals(400, resp.getCompletionTokensUsed());
        assertEquals(150, resp.getExecutionTimeMs());
        assertEquals("Exec summary", resp.getExecutiveSummary());
        assertEquals("# Markdown Guide", resp.getFullStudyGuideMarkdown());
        assertEquals("PUBLISHED", resp.getReviewStatus());
        assertEquals(now, resp.getReviewedAt());
        assertTrue(resp.getPublishedToStudents());
        assertEquals(now, resp.getPublishedAt());
        assertEquals("Notes", resp.getInstructorNotes());
        assertEquals(now, resp.getCreatedAt());
        assertEquals(now, resp.getUpdatedAt());
    }
}
