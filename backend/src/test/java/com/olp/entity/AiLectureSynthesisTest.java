package com.olp.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AiLectureSynthesisTest {

    @Test
    void testEntityLifecycleAndGettersSetters() {
        AiLectureSynthesis entity = new AiLectureSynthesis();
        entity.setId(1);
        entity.setLessonId(10);
        entity.setNotesDocumentId(20);
        entity.setInstructorId(30);
        entity.setModelProvider("GOOGLE_GEMINI");
        entity.setModelVersion("gemini-1.5-pro");
        entity.setPromptTokensUsed(1200);
        entity.setCompletionTokensUsed(850);
        entity.setExecutionTimeMs(340);
        entity.setExecutiveSummary("Core summary");
        entity.setFullStudyGuideMarkdown("# Full Guide");
        entity.setGlossaryJson("[{\"term\":\"Paxos\"}]");
        entity.setSpokenNuancesJson("[{\"topic\":\"Exam Hint\"}]");
        entity.setQuizQuestionsJson("[{\"id\":1}]");
        entity.setFlashcardsJson("[{\"id\":1}]");
        entity.setReviewStatus("DRAFT");
        entity.setInstructorNotes("Needs review on theorem 3");

        LocalDateTime now = LocalDateTime.now();
        entity.setReviewedAt(now);
        entity.setPublishedToStudents(false);
        entity.setPublishedAt(null);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        assertEquals(1, entity.getId());
        assertEquals(10, entity.getLessonId());
        assertEquals(20, entity.getNotesDocumentId());
        assertEquals(30, entity.getInstructorId());
        assertEquals("GOOGLE_GEMINI", entity.getModelProvider());
        assertEquals("gemini-1.5-pro", entity.getModelVersion());
        assertEquals(1200, entity.getPromptTokensUsed());
        assertEquals(850, entity.getCompletionTokensUsed());
        assertEquals(340, entity.getExecutionTimeMs());
        assertEquals("Core summary", entity.getExecutiveSummary());
        assertEquals("# Full Guide", entity.getFullStudyGuideMarkdown());
        assertEquals("[{\"term\":\"Paxos\"}]", entity.getGlossaryJson());
        assertEquals("[{\"topic\":\"Exam Hint\"}]", entity.getSpokenNuancesJson());
        assertEquals("[{\"id\":1}]", entity.getQuizQuestionsJson());
        assertEquals("[{\"id\":1}]", entity.getFlashcardsJson());
        assertEquals("DRAFT", entity.getReviewStatus());
        assertEquals("Needs review on theorem 3", entity.getInstructorNotes());
        assertEquals(now, entity.getReviewedAt());
        assertFalse(entity.getPublishedToStudents());
        assertNull(entity.getPublishedAt());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());

        // Test prePersist and preUpdate
        AiLectureSynthesis fresh = new AiLectureSynthesis();
        fresh.onPrePersist();
        assertNotNull(fresh.getCreatedAt());
        assertNotNull(fresh.getUpdatedAt());
        assertEquals("DRAFT", fresh.getReviewStatus());
        assertFalse(fresh.getPublishedToStudents());

        fresh.onPreUpdate();
        assertNotNull(fresh.getUpdatedAt());
    }
}
