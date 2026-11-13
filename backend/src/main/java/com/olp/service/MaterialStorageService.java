package com.olp.service;

import com.olp.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Storage service managing supplementary learning materials on disk (FR-I14).
 */
@Service
public class MaterialStorageService {

    private static final Logger logger = LoggerFactory.getLogger(MaterialStorageService.class);

    public static final long MAX_MATERIAL_FILE_SIZE = 50 * 1024 * 1024L; // 50 MB

    public static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt"
    );

    public static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain",
            "application/octet-stream"
    );

    private final Path materialsLocation;

    public MaterialStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.materialsLocation = Paths.get(uploadDir, "materials").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.materialsLocation);
        } catch (IOException e) {
            logger.warn("Could not initialize supplementary materials directory: {}", e.getMessage());
        }
    }

    /**
     * Validates file size, non-emptiness, extension, and MIME type.
     */
    public void validateMaterial(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Material file cannot be empty");
        }

        if (file.getSize() > MAX_MATERIAL_FILE_SIZE) {
            throw new IllegalArgumentException(String.format("File size %d bytes exceeds the maximum allowed limit of %d bytes (50 MB)",
                    file.getSize(), MAX_MATERIAL_FILE_SIZE));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Original filename is required");
        }

        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Disallowed file extension: ." + extension + ". Allowed: " + ALLOWED_EXTENSIONS);
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String lowerType = contentType.toLowerCase(Locale.ROOT);
            if (!ALLOWED_MIME_TYPES.contains(lowerType)) {
                throw new IllegalArgumentException("Disallowed MIME type: " + contentType);
            }
        }
    }

    /**
     * Persists material file to disk and returns the relative/absolute file URL.
     */
    public String storeMaterial(MultipartFile file) throws IOException {
        validateMaterial(file);

        String originalName = file.getOriginalFilename();
        String safeName = originalName != null ? originalName.replaceAll("[^a-zA-Z0-9._-]", "_") : "material";
        String storedFilename = UUID.randomUUID().toString() + "_" + safeName;
        Path targetPath = materialsLocation.resolve(storedFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        return targetPath.toString().replace('\\', '/');
    }

    /**
     * Loads a stored material file as a Spring Resource for viewing or downloading (FR-I17).
     */
    public Resource loadMaterialAsResource(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException("Material file reference cannot be empty");
        }
        try {
            Path filePath = Paths.get(fileUrl).normalize();
            if (!filePath.isAbsolute()) {
                filePath = materialsLocation.resolve(filePath).normalize();
            }

            // Path traversal guard
            if (!filePath.toAbsolutePath().startsWith(materialsLocation.toAbsolutePath())) {
                throw new IllegalArgumentException("Cannot access files outside designated materials storage directory");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Material file not found or unreadable on disk: " + fileUrl);
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid file path URI: " + fileUrl, e);
        }
    }

    /**
     * Delete stored material file if it exists.
     */
    public void deleteMaterialFile(String fileUrl) {
        if (fileUrl != null && !fileUrl.isBlank()) {
            try {
                Path path = Paths.get(fileUrl);
                Files.deleteIfExists(path);
            } catch (Exception e) {
                logger.warn("Could not delete material file at {}: {}", fileUrl, e.getMessage());
            }
        }
    }
}

