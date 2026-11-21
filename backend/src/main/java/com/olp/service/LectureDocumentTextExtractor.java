package com.olp.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class LectureDocumentTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(LectureDocumentTextExtractor.class);

    public record ExtractionResult(String text, int pageCount, String status) {}

    public ExtractionResult extract(MultipartFile file, File savedFile) {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";

        try {
            if (contentType.contains("pdf") || filename.endsWith(".pdf")) {
                return extractPdf(savedFile);
            } else if (filename.endsWith(".md") || filename.endsWith(".markdown") ||
                       filename.endsWith(".txt") || filename.endsWith(".tex") ||
                       contentType.startsWith("text/")) {
                return extractPlainText(savedFile);
            } else {
                // Best effort text extraction for slides or docx formats
                String text = new String(file.getBytes(), StandardCharsets.UTF_8)
                        .replaceAll("[^\\x20-\\x7E\\r\\n\\t]", " ")
                        .replaceAll("\\s{2,}", " ")
                        .trim();
                return new ExtractionResult(text, 1, "COMPLETED");
            }
        } catch (Exception e) {
            log.error("Failed to extract text from document: {}", filename, e);
            return new ExtractionResult("Failed to extract text: " + e.getMessage(), 0, "FAILED");
        }
    }

    private ExtractionResult extractPdf(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            int pages = document.getNumberOfPages();
            return new ExtractionResult(sanitizeText(text), pages, "COMPLETED");
        }
    }

    private ExtractionResult extractPlainText(File file) throws IOException {
        byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
        String text = new String(bytes, StandardCharsets.UTF_8);
        return new ExtractionResult(sanitizeText(text), 1, "COMPLETED");
    }

    private String sanitizeText(String text) {
        if (text == null) return "";
        return text.replace("\u0000", "").trim();
    }
}
