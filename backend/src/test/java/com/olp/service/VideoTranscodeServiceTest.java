package com.olp.service;

import com.olp.entity.*;
import com.olp.event.VideoProcessingStatusEvent;
import com.olp.repository.LessonRepository;
import com.olp.repository.VideoUploadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoTranscodeServiceTest {

    @Mock
    private VideoUploadRepository videoUploadRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private VideoTranscodeService videoTranscodeService;

    private User instructor;
    private Lesson lesson;
    private VideoUpload upload;

    @BeforeEach
    void setUp() {
        instructor = new User("Dr. Chen", "chen@olp.com", "hash", "instructor");
        instructor.setId(10);

        lesson = new Lesson();
        lesson.setId(401);
        lesson.setTitle("Introduction to Streams");
        lesson.setLessonType(LessonType.VIDEO);

        upload = new VideoUpload("upload-42", lesson, instructor, "streams.mp4", "video/mp4", 10000L, 5000L, 2);
        upload.setStatus(VideoUploadStatus.COMPLETED);
    }

    @Test
    void testEnqueueTranscode_Success_SetsReadyAndNotifiesInstructor() {
        when(videoUploadRepository.findById("upload-42")).thenReturn(Optional.of(upload));
        when(videoUploadRepository.save(any(VideoUpload.class))).thenAnswer(i -> i.getArgument(0));

        CompletableFuture<VideoUpload> future = videoTranscodeService.enqueueTranscode("upload-42");
        VideoUpload result = future.join();

        assertNotNull(result);
        assertEquals(VideoUploadStatus.READY, result.getStatus());
        assertEquals("/media/hls/upload-42/master.m3u8", result.getHlsUrl());
        assertEquals("/media/dash/upload-42/manifest.mpd", result.getDashUrl());
        assertEquals("/media/hls/upload-42/master.m3u8", lesson.getContentUrl());

        verify(emailService, times(1)).sendVideoProcessingStatusEmail(
                eq("chen@olp.com"), eq("Dr. Chen"), eq("streams.mp4"), eq("Introduction to Streams"), eq("READY"), isNull());
        verify(eventPublisher, times(1)).publishEvent(any(VideoProcessingStatusEvent.class));
    }

    @Test
    void testEnqueueTranscode_NotFound_ReturnsNullSafely() {
        when(videoUploadRepository.findById("unknown")).thenReturn(Optional.empty());

        CompletableFuture<VideoUpload> future = videoTranscodeService.enqueueTranscode("unknown");
        VideoUpload result = future.join();

        assertNull(result);
        verify(videoUploadRepository, never()).save(any());
        verify(emailService, never()).sendVideoProcessingStatusEmail(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
