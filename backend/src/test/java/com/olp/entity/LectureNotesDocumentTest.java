package com.olp.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class LectureNotesDocumentTest {

    @Test
    void testEntityLifecycleAndGettersSetters() {
        LectureNotesDocument doc = new LectureNotesDocument();
        doc.setId(10);
        doc.setLessonId(20);
        doc.setUploaderId(30);
        doc.setFileName("consensus_slides.pdf");
        doc.setFileType("application/pdf");
        doc.setFileSizeBytes(1024L * 1024L);
        doc.setStoragePath("/uploads/lecture-notes/test.pdf");
        doc.setExtractedText("Slide 1: Overview\nSlide 2: Invariants");
        doc.setTextExtractionStatus("COMPLETED");
        doc.setPageCount(12);

        LocalDateTime now = LocalDateTime.now();
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);

        assertEquals(10, doc.getId());
        assertEquals(20, doc.getLessonId());
        assertEquals(30, doc.getUploaderId());
        assertEquals("consensus_slides.pdf", doc.getFileName());
        assertEquals("application/pdf", doc.getFileType());
        assertEquals(1024L * 1024L, doc.getFileSizeBytes());
        assertEquals("/uploads/lecture-notes/test.pdf", doc.getStoragePath());
        assertEquals("Slide 1: Overview\nSlide 2: Invariants", doc.getExtractedText());
        assertEquals("COMPLETED", doc.getTextExtractionStatus());
        assertEquals(12, doc.getPageCount());
        assertEquals(now, doc.getCreatedAt());
        assertEquals(now, doc.getUpdatedAt());

        // Test prePersist and preUpdate
        LectureNotesDocument fresh = new LectureNotesDocument();
        fresh.onPrePersist();
        assertNotNull(fresh.getCreatedAt());
        assertNotNull(fresh.getUpdatedAt());
        assertEquals("COMPLETED", fresh.getTextExtractionStatus());

        fresh.onPreUpdate();
        assertNotNull(fresh.getUpdatedAt());
    }
}
