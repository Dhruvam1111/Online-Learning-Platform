package com.olp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Storage service handling chunked, resumable file operations on disk.
 */
@Service
public class VideoStorageService {

    private static final Logger logger = LoggerFactory.getLogger(VideoStorageService.class);

    private final Path videoRootLocation;
    private final Path tempRootLocation;

    public VideoStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.videoRootLocation = Paths.get(uploadDir, "videos").toAbsolutePath().normalize();
        this.tempRootLocation = this.videoRootLocation.resolve("temp");
        try {
            Files.createDirectories(this.videoRootLocation);
            Files.createDirectories(this.tempRootLocation);
        } catch (IOException e) {
            logger.warn("Could not initialize video upload directories: {}", e.getMessage());
        }
    }

    public void initSession(String uploadId) throws IOException {
        Path sessionDir = tempRootLocation.resolve(uploadId);
        Files.createDirectories(sessionDir);
    }

    public long saveChunk(String uploadId, int chunkIndex, InputStream inputStream) throws IOException {
        Path sessionDir = tempRootLocation.resolve(uploadId);
        if (!Files.exists(sessionDir)) {
            Files.createDirectories(sessionDir);
        }
        Path chunkFile = sessionDir.resolve(String.format("chunk_%05d.part", chunkIndex));
        Files.copy(inputStream, chunkFile, StandardCopyOption.REPLACE_EXISTING);
        return Files.size(chunkFile);
    }

    public List<Integer> getUploadedChunkIndices(String uploadId) {
        Path sessionDir = tempRootLocation.resolve(uploadId);
        List<Integer> indices = new ArrayList<>();
        if (!Files.exists(sessionDir)) {
            return indices;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sessionDir, "chunk_*.part")) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                // extract index from chunk_00000.part
                String numStr = name.replace("chunk_", "").replace(".part", "");
                try {
                    indices.add(Integer.parseInt(numStr));
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            logger.warn("Error reading chunk indices for upload {}: {}", uploadId, e.getMessage());
        }

        indices.sort(Comparator.naturalOrder());
        return indices;
    }

    public String assembleChunks(String uploadId, String originalFilename, int totalChunks) throws IOException {
        Path sessionDir = tempRootLocation.resolve(uploadId);
        String safeName = uploadId + "_" + originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path destinationFile = videoRootLocation.resolve(safeName);

        try (OutputStream outputStream = Files.newOutputStream(destinationFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (int i = 0; i < totalChunks; i++) {
                Path chunkFile = sessionDir.resolve(String.format("chunk_%05d.part", i));
                if (!Files.exists(chunkFile)) {
                    throw new IllegalStateException("Missing expected chunk: " + i + " for upload ID: " + uploadId);
                }
                Files.copy(chunkFile, outputStream);
            }
        }

        // Clean up temp chunk files
        cleanSession(uploadId);

        return destinationFile.toString().replace('\\', '/');
    }

    public void cleanSession(String uploadId) {
        Path sessionDir = tempRootLocation.resolve(uploadId);
        if (Files.exists(sessionDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(sessionDir)) {
                for (Path p : stream) {
                    Files.deleteIfExists(p);
                }
                Files.deleteIfExists(sessionDir);
            } catch (IOException e) {
                logger.warn("Could not clean temp upload session {}: {}", uploadId, e.getMessage());
            }
        }
    }
}

