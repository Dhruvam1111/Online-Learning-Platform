package com.olp.controller;

import com.olp.dto.InitiateVideoUploadRequest;
import com.olp.dto.VideoChunkResponse;
import com.olp.dto.VideoUploadResponse;
import com.olp.entity.User;
import com.olp.entity.VideoUploadStatus;
import com.olp.service.VideoUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoUploadControllerTest {

    @Mock
    private VideoUploadService videoUploadService;

    @InjectMocks
    private VideoUploadController videoUploadController;

    private User instructor;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        instructor = new User("Dr. Chen", "chen@olp.com", "hash", "instructor");
        instructor.setId(10);
        authentication = new UsernamePasswordAuthenticationToken(instructor, null);
    }

    @Test
    void testInitiateUpload() {
        InitiateVideoUploadRequest request = new InitiateVideoUploadRequest();
        request.setLessonId(401);
        request.setFilename("lecture.mp4");
        request.setFileType("video/mp4");
        request.setTotalBytes(10000000L);
        request.setChunkSize(5000000L);

        VideoUploadResponse expected = new VideoUploadResponse();
        expected.setUploadId("upload-123");
        expected.setLessonId(401);
        expected.setStatus(VideoUploadStatus.INITIATED);

        when(videoUploadService.resolveUser(authentication)).thenReturn(instructor);
        when(videoUploadService.initiateUpload(instructor, request)).thenReturn(expected);

        ResponseEntity<VideoUploadResponse> response = videoUploadController.initiateUpload(authentication, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("upload-123", response.getBody().getUploadId());
        verify(videoUploadService, times(1)).initiateUpload(instructor, request);
    }

    @Test
    void testUploadChunk() {
        MockMultipartFile file = new MockMultipartFile("file", "chunk0.part", "application/octet-stream", new byte[500]);
        VideoChunkResponse expected = new VideoChunkResponse("upload-123", 0, 500L, 1000L, 50.0, VideoUploadStatus.UPLOADING, false, 1);

        when(videoUploadService.resolveUser(authentication)).thenReturn(instructor);
        when(videoUploadService.uploadChunk(instructor, "upload-123", 0, file)).thenReturn(expected);

        ResponseEntity<VideoChunkResponse> response = videoUploadController.uploadChunk("upload-123", 0, file, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getChunkIndex());
        assertFalse(response.getBody().isCompleted());
        assertEquals(1, response.getBody().getNextExpectedChunkIndex());
    }

    @Test
    void testResumeUpload() {
        VideoChunkResponse expected = new VideoChunkResponse("upload-123", 0, 500L, 1000L, 50.0, VideoUploadStatus.UPLOADING, false, 1);

        when(videoUploadService.resolveUser(authentication)).thenReturn(instructor);
        when(videoUploadService.resumeUpload(instructor, "upload-123")).thenReturn(expected);

        ResponseEntity<VideoChunkResponse> response = videoUploadController.resumeUpload("upload-123", authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getNextExpectedChunkIndex());
    }

    @Test
    void testGetUploadStatus() {
        VideoUploadResponse expected = new VideoUploadResponse();
        expected.setUploadId("upload-123");
        expected.setStatus(VideoUploadStatus.UPLOADING);
        expected.setProgressPercent(75.0);

        when(videoUploadService.resolveUser(authentication)).thenReturn(instructor);
        when(videoUploadService.getUploadStatus(instructor, "upload-123")).thenReturn(expected);

        ResponseEntity<VideoUploadResponse> response = videoUploadController.getUploadStatus("upload-123", authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(75.0, response.getBody().getProgressPercent());
    }

    @Test
    void testGetVideoByLesson() {
        VideoUploadResponse expected = new VideoUploadResponse();
        expected.setUploadId("upload-123");
        expected.setLessonId(401);
        expected.setStatus(VideoUploadStatus.READY);

        when(videoUploadService.resolveUser(authentication)).thenReturn(instructor);
        when(videoUploadService.getVideoByLesson(instructor, 401)).thenReturn(expected);

        ResponseEntity<VideoUploadResponse> response = videoUploadController.getVideoByLesson(401, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("upload-123", response.getBody().getUploadId());
    }
}

