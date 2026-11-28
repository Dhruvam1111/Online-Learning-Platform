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
        String filename = "";
        try {
            if (file != null && file.getOriginalFilename() != null) {
                filename = file.getOriginalFilename().toLowerCase();
            } else if (savedFile != null) {
                filename = savedFile.getName().toLowerCase();
            }
            String contentType = (file != null && file.getContentType() != null)
                    ? file.getContentType().toLowerCase()
                    : "";

            if (file == null && savedFile == null) {
                throw new IllegalArgumentException("No document source provided for extraction");
            }

            if (contentType.contains("pdf") || filename.endsWith(".pdf")) {
                return extractPdf(file, savedFile);
            } else if (filename.endsWith(".md") || filename.endsWith(".markdown") ||
                       filename.endsWith(".txt") || filename.endsWith(".tex") ||
                       contentType.startsWith("text/")) {
                return extractPlainText(file, savedFile);
            } else {
                // Prefer savedFile if present (MultipartFile temp file may already be moved via transferTo)
                byte[] bytes = readDocumentBytes(file, savedFile);
                String text = new String(bytes, StandardCharsets.UTF_8)
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

    private ExtractionResult extractPdf(MultipartFile file, File savedFile) throws IOException {
        try (PDDocument document = (savedFile != null)
                ? Loader.loadPDF(savedFile)
                : Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            int pages = document.getNumberOfPages();
            return new ExtractionResult(sanitizeText(text), pages, "COMPLETED");
        }
    }

    private ExtractionResult extractPlainText(MultipartFile file, File savedFile) throws IOException {
        byte[] bytes = readDocumentBytes(file, savedFile);
        String text = new String(bytes, StandardCharsets.UTF_8);
        return new ExtractionResult(sanitizeText(text), 1, "COMPLETED");
    }

    private byte[] readDocumentBytes(MultipartFile file, File savedFile) throws IOException {
        if (savedFile != null && savedFile.exists()) {
            return java.nio.file.Files.readAllBytes(savedFile.toPath());
        }
        if (file != null) {
            return file.getBytes();
        }
        throw new IOException("Saved file does not exist and multipart file is unavailable");
    }

    private String sanitizeText(String text) {
        if (text == null) return "";
        return text.replace("\u0000", "").trim();
    }
}
