package com.olp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LectureDocumentTextExtractorTest {

    private LectureDocumentTextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new LectureDocumentTextExtractor();
    }

    @Test
    void testExtractPlainTextFile(@TempDir Path tempDir) throws IOException {
        String content = "Introduction to Paxos Consensus Algorithm.\nKey Theorem: Quorum intersection.";
        Path filePath = tempDir.resolve("lecture_notes.txt");
        Files.writeString(filePath, content, StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile(
                "file", "lecture_notes.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8)
        );

        LectureDocumentTextExtractor.ExtractionResult result = extractor.extract(file, filePath.toFile());
        assertNotNull(result);
        assertEquals("COMPLETED", result.status());
        assertEquals(1, result.pageCount());
        assertTrue(result.text().contains("Introduction to Paxos"));
        assertTrue(result.text().contains("Quorum intersection"));
    }

    @Test
    void testExtractMarkdownFile(@TempDir Path tempDir) throws IOException {
        String content = "# Module 4: Distributed Locks\n- Redlock algorithm\n- Safety proofs";
        Path filePath = tempDir.resolve("locks.md");
        Files.writeString(filePath, content, StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile(
                "file", "locks.md", "text/markdown", content.getBytes(StandardCharsets.UTF_8)
        );

        LectureDocumentTextExtractor.ExtractionResult result = extractor.extract(file, filePath.toFile());
        assertEquals("COMPLETED", result.status());
        assertTrue(result.text().contains("Redlock algorithm"));
    }

    @Test
    void testExtractFallbackBinaryContent(@TempDir Path tempDir) throws IOException {
        String content = "General slide text with unprintable symbols \u0001\u0002\u0003.";
        Path filePath = tempDir.resolve("slides.bin");
        Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));

        MockMultipartFile file = new MockMultipartFile(
                "file", "slides.bin", "application/octet-stream", content.getBytes(StandardCharsets.UTF_8)
        );

        LectureDocumentTextExtractor.ExtractionResult result = extractor.extract(file, filePath.toFile());
        assertEquals("COMPLETED", result.status());
        assertTrue(result.text().contains("General slide text"));
    }

    @Test
    void testExtractErrorHandling() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "corrupt.pdf", "application/pdf", "not a real pdf".getBytes()
        );
        File nonExistent = new File("/non/existent/path/corrupt.pdf");

        LectureDocumentTextExtractor.ExtractionResult result = extractor.extract(file, nonExistent);
        assertEquals("FAILED", result.status());
        assertTrue(result.text().contains("Failed to extract text"));
    }
}
