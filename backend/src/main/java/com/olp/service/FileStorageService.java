package com.olp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    public static final long MAX_PROFILE_PICTURE_SIZE = 5 * 1024 * 1024L; // 5 MB
    public static final long MAX_CREDENTIAL_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private static final Set<String> ALLOWED_CREDENTIAL_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CREDENTIAL_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp");

    private final Path rootLocation;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
            Files.createDirectories(this.rootLocation.resolve("profile-pictures"));
            Files.createDirectories(this.rootLocation.resolve("credentials"));
        } catch (IOException e) {
            logger.warn("Could not initialize upload storage directories: {}", e.getMessage());
        }
    }

    /**
     * Validates an uploaded profile picture for file emptiness, size, extension,
     * content type, and image decodability.
     */
    public void validateProfilePicture(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Profile picture file cannot be empty");
        }

        if (file.getSize() > MAX_PROFILE_PICTURE_SIZE) {
            throw new IllegalArgumentException("Profile picture size exceeds maximum limit of 5 MB");
        }

        String originalName = file.getOriginalFilename();
        String extension = getFileExtension(originalName);

        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Invalid file format. Profile picture must be a JPEG, PNG, or WEBP image");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Invalid content type for profile picture. Must be image/jpeg, image/png, or image/webp");
        }

        // Verify image integrity by parsing image stream (rejects disguised executable
        // binaries)
        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new IllegalArgumentException("Uploaded file is not a valid decodable image");
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read image content: " + ex.getMessage());
        }
    }

    /**
     * Validates credential documents supporting PDF and image formats (JPEG, PNG,
     * WEBP).
     * Enforces a 10 MB maximum size limit.
     */
    public void validateCredentialFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Credential document file cannot be empty");
        }

        if (file.getSize() > MAX_CREDENTIAL_FILE_SIZE) {
            throw new IllegalArgumentException("Credential document size exceeds maximum limit of 10 MB");
        }

        String originalName = file.getOriginalFilename();
        String extension = getFileExtension(originalName);

        if (!ALLOWED_CREDENTIAL_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Invalid file format. Credential documents must be PDF, JPEG, PNG, or WEBP");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CREDENTIAL_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Invalid content type for credential document. Must be application/pdf, image/jpeg, image/png, or image/webp");
        }
    }

    /**
     * Stores an instructor's profile picture and returns the relative stored path.
     */
    public String storeProfilePicture(Integer instructorId, MultipartFile file) {
        validateProfilePicture(file);

        String sanitizedBase = sanitizeFileName(file.getOriginalFilename());
        String uniqueFileName = UUID.randomUUID() + "_" + sanitizedBase;

        Path targetDir = rootLocation.resolve("profile-pictures").resolve(String.valueOf(instructorId));
        return saveFileToDisk(file, targetDir, uniqueFileName,
                "profile-pictures/" + instructorId + "/" + uniqueFileName);
    }

    /**
     * Stores an instructor's credential document privately and returns the relative
     * stored path.
     */
    public String storeCredential(Integer instructorId, MultipartFile file) {
        validateCredentialFile(file);

        String sanitizedBase = sanitizeFileName(file.getOriginalFilename());
        String uniqueFileName = UUID.randomUUID() + "_" + sanitizedBase;

        Path targetDir = rootLocation.resolve("credentials").resolve(String.valueOf(instructorId));
        return saveFileToDisk(file, targetDir, uniqueFileName, "credentials/" + instructorId + "/" + uniqueFileName);
    }

    /**
     * Loads a stored file as a Spring Resource for streaming/downloading.
     */
    public Resource loadFileAsResource(String relativePath) {
        try {
            Path filePath = rootLocation.resolve(relativePath).normalize();

            // Guard against path traversal attacks (e.g. ../../etc/passwd)
            if (!filePath.startsWith(rootLocation)) {
                throw new IllegalArgumentException("Cannot read file outside designated upload directory");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IllegalArgumentException("File not found or unreadable: " + relativePath);
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid file path reference: " + relativePath, e);
        }
    }

    /**
     * Deletes a physical file from disk storage.
     */
    public void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Path filePath = rootLocation.resolve(relativePath).normalize();
            if (filePath.startsWith(rootLocation)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            logger.warn("Failed to delete physical file {}: {}", relativePath, e.getMessage());
        }
    }

    private String saveFileToDisk(MultipartFile file, Path targetDir, String uniqueFileName, String relativeResult) {
        try {
            Files.createDirectories(targetDir);

            Path destinationPath = targetDir.resolve(uniqueFileName).normalize();
            if (!destinationPath.startsWith(rootLocation)) {
                throw new IllegalArgumentException("Invalid destination file path");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return relativeResult;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store file on disk: " + ex.getMessage(), ex);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "file";
        }
        String clean = StringUtils.cleanPath(fileName);
        // Remove path components if present
        if (clean.contains("/")) {
            clean = clean.substring(clean.lastIndexOf("/") + 1);
        }
        if (clean.contains("\\")) {
            clean = clean.substring(clean.lastIndexOf("\\") + 1);
        }
        // Replace non-alphanumeric (except dot, dash, underscore)
        return clean.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
