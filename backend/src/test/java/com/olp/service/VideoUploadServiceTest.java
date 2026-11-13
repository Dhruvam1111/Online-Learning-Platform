package com.olp.service;

import com.olp.dto.InitiateVideoUploadRequest;
import com.olp.dto.VideoChunkResponse;
import com.olp.dto.VideoUploadResponse;
import com.olp.entity.*;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.LessonRepository;
import com.olp.repository.UserRepository;
import com.olp.repository.VideoUploadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoUploadServiceTest {

    @Mock
    private VideoUploadRepository videoUploadRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseInstructorRepository courseInstructorRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VideoStorageService videoStorageService;
    @Mock
    private VideoTranscodeService videoTranscodeService;
    @Mock
    private ContentDeclarationService contentDeclarationService;

    @InjectMocks
    private VideoUploadService videoUploadService;

    private User instructor;
    private User coInstructor;
    private User otherInstructor;
    private User admin;
    private User student;
    private Course course;
    private CourseModule module;
    private CourseSection section;
    private Lesson videoLesson;
    private Lesson materialLesson;

    @BeforeEach
    void setUp() {
        instructor = new User("Dr. Chen", "chen@olp.com", "hash", "instructor");
        instructor.setId(10);

        coInstructor = new User("Dr. Co", "co@olp.com", "hash", "instructor");
        coInstructor.setId(11);

        otherInstructor = new User("Dr. Stranger", "stranger@olp.com", "hash", "instructor");
        otherInstructor.setId(12);

        admin = new User("Admin", "admin@olp.com", "hash", "admin");
        admin.setId(1);

        student = new User("Student", "student@olp.com", "hash", "student");
        student.setId(20);

        course = new Course();
        course.setId(100);
        course.setTitle("Web Systems");
        course.setInstructor(instructor);

        module = new CourseModule();
        module.setId(200);
        module.setCourse(course);

        section = new CourseSection();
        section.setId(300);
        section.setModule(module);

        videoLesson = new Lesson();
        videoLesson.setId(401);
        videoLesson.setTitle("HTTP Protocol Deep Dive");
        videoLesson.setLessonType(LessonType.VIDEO);
        videoLesson.setSection(section);

        materialLesson = new Lesson();
        materialLesson.setId(402);
        materialLesson.setTitle("HTTP Reference Sheet");
        materialLesson.setLessonType(LessonType.MATERIAL);
        materialLesson.setSection(section);
    }

    @Test
    void testInitiateUpload_Success_OwnerInstructor() throws IOException {
        InitiateVideoUploadRequest request = new InitiateVideoUploadRequest();
        request.setLessonId(401);
        request.setFilename("lecture1.mp4");
        request.setFileType("video/mp4");
        request.setTotalBytes(15728640L); // 15 MB
        request.setChunkSize(5242880L);  // 5 MB -> 3 chunks

        when(lessonRepository.findById(401)).thenReturn(Optional.of(videoLesson));
        doNothing().when(contentDeclarationService).assertDeclared(401);
        doNothing().when(videoStorageService).initSession(anyString());
        when(videoUploadRepository.save(any(VideoUpload.class))).thenAnswer(i -> i.getArgument(0));

        VideoUploadResponse response = videoUploadService.initiateUpload(instructor, request);

        assertNotNull(response);
        assertNotNull(response.getUploadId());
        assertEquals(401, response.getLessonId());
        assertEquals(3, response.getTotalChunks());
        assertEquals(VideoUploadStatus.INITIATED, response.getStatus());
        verify(contentDeclarationService, times(1)).assertDeclared(401);
        verify(videoStorageService, times(1)).initSession(anyString());
        verify(videoUploadRepository, times(1)).save(any(VideoUpload.class));
    }

    @Test
    void testInitiateUpload_Success_CoInstructor() throws IOException {
        InitiateVideoUploadRequest request = new InitiateVideoUploadRequest();
        request.setLessonId(401);
        request.setFilename("lecture1.mp4");
        request.setFileType("video/mp4");
        request.setTotalBytes(10000000L);
        request.setChunkSize(5000000L);

        when(lessonRepository.findById(401)).thenReturn(Optional.of(videoLesson));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 11)).thenReturn(true);
        doNothing().when(contentDeclarationService).assertDeclared(401);
        when(videoUploadRepository.save(any(VideoUpload.class))).thenAnswer(i -> i.getArgument(0));

        VideoUploadResponse response = videoUploadService.initiateUpload(coInstructor, request);
        assertNotNull(response);
        assertEquals(2, response.getTotalChunks());
    }

    @Test
    void testInitiateUpload_Success_Admin() throws IOException {
        InitiateVideoUploadRequest request = new InitiateVideoUploadRequest();
        request.setLessonId(401);
        request.setFilename("lecture1.mp4");
        request.setFileType("video/mp4");
        request.setTotalBytes(5000000L);
        request.setChunkSize(5000000L);

        when(lessonRepository.findById(401)).thenReturn(Optional.of(videoLesson));
        doNothing().when(contentDeclarationService).assertDeclared(401);
        when(videoUploadRepository.save(any(VideoUpload.class))).thenAnswer(i -> i.getArgument(0));

        VideoUploadResponse response = videoUploadService.initiateUpload(admin, request);
        assertNotNull(response);
        assertEquals(1, response.getTotalChunks());
    }

    @Test
    void testInitiateUpload_Fails_Unauthenticated() {
        InitiateVideoUploadRequest request = new InitiateVideoUploadRequest();
        request.setLessonId(401);
        assertThrows(AccessDeniedException.class, () -> videoUploadService.initiateUpload(null, request));
    }

    @Test
    void testInitiateUpload_Fails_Student() {
        InitiateVideoUploadRequest request = new InitiateVideoUploadRequest();
        request.setLessonId(401);
        assertThrows(AccessDeniedException.class, () -> videoUploadService.initiateUpload(student, request));
    }

    @Test
    void testInitiateUpload_Fails_UnauthorizedInstructor() {
        InitiateVideoUploadRequest request = new InitiateVideoUploadRequest();
        request.setLessonId(401);
        when(lessonRepository.findById(401)).thenReturn(Optional.of(videoLesson));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 12)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> videoUploadService.initiateUpload(otherInstructor, request));
    }

    @Test
    void testInitiateUpload_Fails_LessonNotFound() {
        InitiateVideoUploadRequest request = new InitiateVideoUploadRequest();
        request.setLessonId(999);
        when(lessonRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> videoUploadService.initiateUpload(instructor, request));
    }

    @Test
    void testInitiateUpload_Fails_NonVideoLesson() {
        InitiateVideoUploadRequest request = new InitiateVideoUploadRequest();
        request.setLessonId(402);
        when(lessonRepository.findById(402)).thenReturn(Optional.of(materialLesson));

        assertThrows(IllegalArgumentException.class, () -> videoUploadService.initiateUpload(instructor, request));
    }

    @Test
    void testInitiateUpload_Fails_ContentDeclarationBlocked() {
        InitiateVideoUploadRequest request = new InitiateVideoUploadRequest();
        request.setLessonId(401);
        when(lessonRepository.findById(401)).thenReturn(Optional.of(videoLesson));
        doThrow(new IllegalStateException("Content upload blocked: No valid content ownership declaration found"))
                .when(contentDeclarationService).assertDeclared(401);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> videoUploadService.initiateUpload(instructor, request));
        assertTrue(ex.getMessage().contains("Content upload blocked"));
    }

    @Test
    void testUploadChunk_IntermediateChunk() throws IOException {
        VideoUpload upload = new VideoUpload("sess-1", videoLesson, instructor, "video.mp4", "video/mp4", 1500L, 500L, 3);
        when(videoUploadRepository.findById("sess-1")).thenReturn(Optional.of(upload));
        when(videoStorageService.saveChunk(eq("sess-1"), eq(0), any(InputStream.class))).thenReturn(500L);
        when(videoStorageService.getUploadedChunkIndices("sess-1")).thenReturn(List.of(0));
        when(videoUploadRepository.save(any(VideoUpload.class))).thenAnswer(i -> i.getArgument(0));

        MockMultipartFile chunkFile = new MockMultipartFile("file", "chunk0.part", "application/octet-stream", new byte[500]);
        VideoChunkResponse response = videoUploadService.uploadChunk(instructor, "sess-1", 0, chunkFile);

        assertNotNull(response);
        assertEquals(0, response.getChunkIndex());
        assertEquals(500L, response.getReceivedBytes());
        assertEquals(VideoUploadStatus.UPLOADING, response.getStatus());
        assertFalse(response.isCompleted());
        assertEquals(1, response.getNextExpectedChunkIndex());
    }

    @Test
    void testUploadChunk_FinalChunkAssemblesAndTranscodes() throws IOException {
        VideoUpload upload = new VideoUpload("sess-1", videoLesson, instructor, "video.mp4", "video/mp4", 1000L, 500L, 2);
        when(videoUploadRepository.findById("sess-1")).thenReturn(Optional.of(upload));
        when(videoStorageService.saveChunk(eq("sess-1"), eq(1), any(InputStream.class))).thenReturn(500L);
        when(videoStorageService.getUploadedChunkIndices("sess-1")).thenReturn(List.of(0, 1));
        when(videoStorageService.assembleChunks("sess-1", "video.mp4", 2)).thenReturn("/uploads/videos/sess-1_video.mp4");
        when(videoUploadRepository.save(any(VideoUpload.class))).thenAnswer(i -> i.getArgument(0));

        MockMultipartFile chunkFile = new MockMultipartFile("file", "chunk1.part", "application/octet-stream", new byte[500]);
        VideoChunkResponse response = videoUploadService.uploadChunk(instructor, "sess-1", 1, chunkFile);

        assertNotNull(response);
        assertTrue(response.isCompleted());
        assertEquals(VideoUploadStatus.COMPLETED, response.getStatus());
        assertEquals(1000L, response.getReceivedBytes());
        assertEquals(100.0, response.getProgressPercent());
        verify(videoStorageService, times(1)).assembleChunks("sess-1", "video.mp4", 2);
        verify(lessonRepository, times(1)).save(videoLesson);
        assertEquals("/uploads/videos/sess-1_video.mp4", videoLesson.getContentUrl());
        verify(videoTranscodeService, times(1)).enqueueTranscode("sess-1");
    }

    @Test
    void testUploadChunk_Fails_AlreadyCompleted() {
        VideoUpload upload = new VideoUpload("sess-1", videoLesson, instructor, "video.mp4", "video/mp4", 1000L, 500L, 2);
        upload.setStatus(VideoUploadStatus.COMPLETED);
        when(videoUploadRepository.findById("sess-1")).thenReturn(Optional.of(upload));

        MockMultipartFile chunkFile = new MockMultipartFile("file", "chunk0.part", "application/octet-stream", new byte[500]);
        assertThrows(IllegalStateException.class, () -> videoUploadService.uploadChunk(instructor, "sess-1", 0, chunkFile));
    }

    @Test
    void testUploadChunk_Fails_InvalidChunkIndex() {
        VideoUpload upload = new VideoUpload("sess-1", videoLesson, instructor, "video.mp4", "video/mp4", 1000L, 500L, 2);
        when(videoUploadRepository.findById("sess-1")).thenReturn(Optional.of(upload));

        MockMultipartFile chunkFile = new MockMultipartFile("file", "chunk0.part", "application/octet-stream", new byte[500]);
        assertThrows(IllegalArgumentException.class, () -> videoUploadService.uploadChunk(instructor, "sess-1", 5, chunkFile));
    }

    @Test
    void testUploadChunk_Fails_EmptyFile() {
        VideoUpload upload = new VideoUpload("sess-1", videoLesson, instructor, "video.mp4", "video/mp4", 1000L, 500L, 2);
        when(videoUploadRepository.findById("sess-1")).thenReturn(Optional.of(upload));

        MockMultipartFile emptyFile = new MockMultipartFile("file", "chunk0.part", "application/octet-stream", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> videoUploadService.uploadChunk(instructor, "sess-1", 0, emptyFile));
    }

    @Test
    void testResumeUpload_Success() {
        VideoUpload upload = new VideoUpload("sess-1", videoLesson, instructor, "video.mp4", "video/mp4", 1500L, 500L, 3);
        upload.setReceivedBytes(500L);
        upload.setStatus(VideoUploadStatus.UPLOADING);

        when(videoUploadRepository.findById("sess-1")).thenReturn(Optional.of(upload));
        when(videoStorageService.getUploadedChunkIndices("sess-1")).thenReturn(List.of(0));

        VideoChunkResponse response = videoUploadService.resumeUpload(instructor, "sess-1");
        assertNotNull(response);
        assertEquals(1, response.getNextExpectedChunkIndex());
        assertFalse(response.isCompleted());
    }

    @Test
    void testGetUploadStatus_Success() {
        VideoUpload upload = new VideoUpload("sess-1", videoLesson, instructor, "video.mp4", "video/mp4", 1500L, 500L, 3);
        upload.setReceivedBytes(1000L);
        upload.setStatus(VideoUploadStatus.UPLOADING);

        when(videoUploadRepository.findById("sess-1")).thenReturn(Optional.of(upload));

        VideoUploadResponse response = videoUploadService.getUploadStatus(instructor, "sess-1");
        assertNotNull(response);
        assertEquals("sess-1", response.getUploadId());
        assertEquals(VideoUploadStatus.UPLOADING, response.getStatus());
        assertEquals(66.67, response.getProgressPercent());
    }

    @Test
    void testGetVideoByLesson_Success() {
        VideoUpload upload = new VideoUpload("sess-1", videoLesson, instructor, "video.mp4", "video/mp4", 1500L, 500L, 3);
        when(lessonRepository.findById(401)).thenReturn(Optional.of(videoLesson));
        when(videoUploadRepository.findFirstByLessonIdOrderByCreatedAtDesc(401)).thenReturn(Optional.of(upload));

        VideoUploadResponse response = videoUploadService.getVideoByLesson(instructor, 401);
        assertNotNull(response);
        assertEquals("sess-1", response.getUploadId());
    }

    @Test
    void testGetVideoByLesson_NotFound() {
        when(lessonRepository.findById(401)).thenReturn(Optional.of(videoLesson));
        when(videoUploadRepository.findFirstByLessonIdOrderByCreatedAtDesc(401)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> videoUploadService.getVideoByLesson(instructor, 401));
    }

    @Test
    void testResolveUser_PrincipalIsUser() {
        Authentication auth = new UsernamePasswordAuthenticationToken(instructor, null);
        User resolved = videoUploadService.resolveUser(auth);
        assertEquals(instructor, resolved);
    }

    @Test
    void testResolveUser_PrincipalIsString_FindsByEmail() {
        Authentication auth = new UsernamePasswordAuthenticationToken("chen@olp.com", null);
        when(userRepository.findByEmail("chen@olp.com")).thenReturn(Optional.of(instructor));

        User resolved = videoUploadService.resolveUser(auth);
        assertEquals(instructor, resolved);
    }
}

