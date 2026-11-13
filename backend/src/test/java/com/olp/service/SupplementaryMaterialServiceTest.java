package com.olp.service;

import com.olp.dto.SupplementaryMaterialResponse;
import com.olp.entity.*;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplementaryMaterialServiceTest {

    @Mock
    private LectureMaterialRepository materialRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private CourseModuleRepository moduleRepository;
    @Mock
    private LectureRepository lectureRepository;
    @Mock
    private CourseInstructorRepository courseInstructorRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MaterialStorageService materialStorageService;
    @Mock
    private VirusScanner virusScanner;
    @Mock
    private ContentDeclarationService contentDeclarationService;

    @InjectMocks
    private SupplementaryMaterialService materialService;

    private User instructor;
    private User coInstructor;
    private User otherInstructor;
    private User admin;
    private User student;
    private Course course;
    private CourseModule module;
    private CourseSection section;
    private Lesson lesson;

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
        course.setTitle("Distributed Systems");
        course.setInstructor(instructor);

        module = new CourseModule();
        module.setId(200);
        module.setCourse(course);

        section = new CourseSection();
        section.setId(300);
        section.setModule(module);

        lesson = new Lesson();
        lesson.setId(400);
        lesson.setTitle("Raft Consensus Algorithm");
        lesson.setLessonType(LessonType.MATERIAL);
        lesson.setSection(section);
    }

    @Test
    void testUploadMaterial_Success_LessonAttachment_ViewOnlyDefault() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "raft_paper.pdf", "application/pdf", new byte[1024]);

        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        doNothing().when(contentDeclarationService).assertDeclared(400);
        doNothing().when(materialStorageService).validateMaterial(file);
        when(virusScanner.scan(any(InputStream.class), eq("raft_paper.pdf")))
                .thenReturn(VirusScanner.ScanResult.clean());
        when(materialStorageService.storeMaterial(file))
                .thenReturn("/uploads/materials/uuid_raft_paper.pdf");
        when(materialRepository.save(any(LectureMaterial.class)))
                .thenAnswer(i -> {
                    LectureMaterial m = i.getArgument(0);
                    m.setId(1);
                    return m;
                });

        SupplementaryMaterialResponse response = materialService.uploadMaterial(
                instructor, 400, null, null, "Raft Paper Reading", file);

        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals(400, response.getLessonId());
        assertEquals("Raft Paper Reading", response.getTitle());
        assertEquals("raft_paper.pdf", response.getOriginalFilename());
        assertEquals("pdf", response.getFileType());
        assertEquals(1024L, response.getFileSizeBytes());
        // Acceptance criteria: Must be view-only (isDownloadable = false) by default
        assertFalse(response.getIsDownloadable());
        assertEquals("CLEAN", response.getScanStatus());
        verify(contentDeclarationService, times(1)).assertDeclared(400);
        verify(materialStorageService, times(1)).storeMaterial(file);
    }

    @Test
    void testUploadMaterial_Success_ModuleAttachment() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "module_slides.pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", new byte[2048]);

        when(moduleRepository.findById(200)).thenReturn(Optional.of(module));
        doNothing().when(materialStorageService).validateMaterial(file);
        when(virusScanner.scan(any(InputStream.class), eq("module_slides.pptx")))
                .thenReturn(VirusScanner.ScanResult.clean());
        when(materialStorageService.storeMaterial(file))
                .thenReturn("/uploads/materials/uuid_module_slides.pptx");
        when(materialRepository.save(any(LectureMaterial.class)))
                .thenAnswer(i -> i.getArgument(0));

        SupplementaryMaterialResponse response = materialService.uploadMaterial(
                instructor, null, 200, null, null, file);

        assertNotNull(response);
        assertEquals(200, response.getModuleId());
        assertEquals("module_slides.pptx", response.getTitle());
        assertFalse(response.getIsDownloadable());
    }

    @Test
    void testUploadMaterial_Success_CoInstructor() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", new byte[100]);

        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 11)).thenReturn(true);
        doNothing().when(contentDeclarationService).assertDeclared(400);
        when(virusScanner.scan(any(InputStream.class), anyString()))
                .thenReturn(VirusScanner.ScanResult.clean());
        when(materialStorageService.storeMaterial(file))
                .thenReturn("/uploads/materials/uuid_notes.txt");
        when(materialRepository.save(any(LectureMaterial.class))).thenAnswer(i -> i.getArgument(0));

        SupplementaryMaterialResponse response = materialService.uploadMaterial(
                coInstructor, 400, null, null, null, file);

        assertNotNull(response);
    }

    @Test
    void testUploadMaterial_Success_Admin() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "admin_guide.pdf", "application/pdf", new byte[500]);

        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        doNothing().when(contentDeclarationService).assertDeclared(400);
        when(virusScanner.scan(any(InputStream.class), anyString()))
                .thenReturn(VirusScanner.ScanResult.clean());
        when(materialStorageService.storeMaterial(file))
                .thenReturn("/uploads/materials/uuid_admin_guide.pdf");
        when(materialRepository.save(any(LectureMaterial.class))).thenAnswer(i -> i.getArgument(0));

        SupplementaryMaterialResponse response = materialService.uploadMaterial(
                admin, 400, null, null, null, file);

        assertNotNull(response);
    }

    @Test
    void testUploadMaterial_Fails_Unauthenticated() {
        MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[100]);
        assertThrows(AccessDeniedException.class,
                () -> materialService.uploadMaterial(null, 400, null, null, null, file));
    }

    @Test
    void testUploadMaterial_Fails_Student() {
        MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[100]);
        assertThrows(AccessDeniedException.class,
                () -> materialService.uploadMaterial(student, 400, null, null, null, file));
    }

    @Test
    void testUploadMaterial_Fails_UnauthorizedInstructor() {
        MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[100]);
        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 12)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> materialService.uploadMaterial(otherInstructor, 400, null, null, null, file));
    }

    @Test
    void testUploadMaterial_Fails_NoTargetSpecified() {
        MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[100]);
        assertThrows(IllegalArgumentException.class,
                () -> materialService.uploadMaterial(instructor, null, null, null, null, file));
    }

    @Test
    void testUploadMaterial_Fails_LessonNotFound() {
        MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[100]);
        when(lessonRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> materialService.uploadMaterial(instructor, 999, null, null, null, file));
    }

    @Test
    void testUploadMaterial_Fails_DeclarationBlocked() {
        MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[100]);
        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        doThrow(new IllegalStateException("Content upload blocked: No valid content ownership declaration found"))
                .when(contentDeclarationService).assertDeclared(400);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> materialService.uploadMaterial(instructor, 400, null, null, null, file));
        assertTrue(ex.getMessage().contains("Content upload blocked"));
    }

    @Test
    void testUploadMaterial_Fails_DisallowedFile() {
        MockMultipartFile file = new MockMultipartFile("file", "bad.exe", "application/x-msdownload", new byte[100]);
        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        doNothing().when(contentDeclarationService).assertDeclared(400);
        doThrow(new IllegalArgumentException("Disallowed file extension: .exe"))
                .when(materialStorageService).validateMaterial(file);

        assertThrows(IllegalArgumentException.class,
                () -> materialService.uploadMaterial(instructor, 400, null, null, null, file));
    }

    @Test
    void testUploadMaterial_Fails_VirusDetected() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "eicar.txt", "text/plain", new byte[100]);
        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        doNothing().when(contentDeclarationService).assertDeclared(400);
        doNothing().when(materialStorageService).validateMaterial(file);
        when(virusScanner.scan(any(InputStream.class), eq("eicar.txt")))
                .thenReturn(VirusScanner.ScanResult.infected("EICAR-Test-Signature"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> materialService.uploadMaterial(instructor, 400, null, null, null, file));
        assertTrue(ex.getMessage().contains("Malicious content detected"));
        // Verify infected file is never stored
        verify(materialStorageService, never()).storeMaterial(any());
        verify(materialRepository, never()).save(any());
    }

    @Test
    void testGetMaterialsByLesson_Success() {
        LectureMaterial mat = new LectureMaterial(400, null, null, 10, "Title", "f.pdf", "/p/f.pdf", "pdf", "application/pdf", 100L, false);
        mat.setId(5);

        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        when(materialRepository.findByLessonId(400)).thenReturn(List.of(mat));

        List<SupplementaryMaterialResponse> list = materialService.getMaterialsByLesson(instructor, 400);
        assertEquals(1, list.size());
        assertEquals(5, list.get(0).getId());
    }

    @Test
    void testGetMaterialsByModule_Success() {
        LectureMaterial mat = new LectureMaterial(null, 200, null, 10, "Title", "f.pdf", "/p/f.pdf", "pdf", "application/pdf", 100L, false);
        mat.setId(6);

        when(moduleRepository.findById(200)).thenReturn(Optional.of(module));
        when(materialRepository.findByModuleId(200)).thenReturn(List.of(mat));

        List<SupplementaryMaterialResponse> list = materialService.getMaterialsByModule(instructor, 200);
        assertEquals(1, list.size());
        assertEquals(6, list.get(0).getId());
    }

    @Test
    void testGetMaterialById_Success() {
        LectureMaterial mat = new LectureMaterial(400, null, null, 10, "Title", "f.pdf", "/p/f.pdf", "pdf", "application/pdf", 100L, false);
        mat.setId(7);

        when(materialRepository.findById(7)).thenReturn(Optional.of(mat));
        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));

        SupplementaryMaterialResponse resp = materialService.getMaterialById(instructor, 7);
        assertNotNull(resp);
        assertEquals(7, resp.getId());
    }

    @Test
    void testDeleteMaterial_Success() {
        LectureMaterial mat = new LectureMaterial(400, null, null, 10, "Title", "f.pdf", "/p/f.pdf", "pdf", "application/pdf", 100L, false);
        mat.setId(8);

        when(materialRepository.findById(8)).thenReturn(Optional.of(mat));
        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        doNothing().when(materialStorageService).deleteMaterialFile("/p/f.pdf");
        doNothing().when(materialRepository).delete(mat);

        assertDoesNotThrow(() -> materialService.deleteMaterial(instructor, 8));
        verify(materialStorageService, times(1)).deleteMaterialFile("/p/f.pdf");
        verify(materialRepository, times(1)).delete(mat);
    }

    @Test
    void testDeleteMaterial_Fails_NotFound() {
        when(materialRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> materialService.deleteMaterial(instructor, 99));
    }

    @Test
    void testResolveUser_PrincipalIsUser() {
        Authentication auth = new UsernamePasswordAuthenticationToken(instructor, null);
        User resolved = materialService.resolveUser(auth);
        assertEquals(instructor, resolved);
    }

    @Test
    void testResolveUser_PrincipalIsString_FindsByEmail() {
        Authentication auth = new UsernamePasswordAuthenticationToken("chen@olp.com", null);
        when(userRepository.findByEmail("chen@olp.com")).thenReturn(Optional.of(instructor));

        User resolved = materialService.resolveUser(auth);
        assertEquals(instructor, resolved);
    }

    @Test
    void testUpdateDownloadableStatus_Success_InstructorOwner() {
        LectureMaterial mat = new LectureMaterial(400, null, null, 10, "Slides", "s.pdf", "/p/s.pdf", "pdf", "application/pdf", 100L, false);
        mat.setId(15);

        when(materialRepository.findById(15)).thenReturn(Optional.of(mat));
        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        when(materialRepository.save(any(LectureMaterial.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupplementaryMaterialResponse resp = materialService.updateDownloadableStatus(instructor, 15, true);

        assertNotNull(resp);
        assertTrue(resp.getIsDownloadable());
        verify(materialRepository, times(1)).save(mat);
    }

    @Test
    void testUpdateDownloadableStatus_Success_CoInstructor() {
        LectureMaterial mat = new LectureMaterial(400, null, null, 10, "Slides", "s.pdf", "/p/s.pdf", "pdf", "application/pdf", 100L, false);
        mat.setId(16);

        when(materialRepository.findById(16)).thenReturn(Optional.of(mat));
        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 11)).thenReturn(true);
        when(materialRepository.save(any(LectureMaterial.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupplementaryMaterialResponse resp = materialService.updateDownloadableStatus(coInstructor, 16, true);

        assertNotNull(resp);
        assertTrue(resp.getIsDownloadable());
        verify(materialRepository, times(1)).save(mat);
    }

    @Test
    void testUpdateDownloadableStatus_Success_Admin() {
        LectureMaterial mat = new LectureMaterial(400, null, null, 10, "Slides", "s.pdf", "/p/s.pdf", "pdf", "application/pdf", 100L, true);
        mat.setId(17);

        when(materialRepository.findById(17)).thenReturn(Optional.of(mat));
        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        when(materialRepository.save(any(LectureMaterial.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupplementaryMaterialResponse resp = materialService.updateDownloadableStatus(admin, 17, false);

        assertNotNull(resp);
        assertFalse(resp.getIsDownloadable());
        verify(materialRepository, times(1)).save(mat);
    }

    @Test
    void testUpdateDownloadableStatus_Fails_UnrelatedInstructor() {
        LectureMaterial mat = new LectureMaterial(400, null, null, 10, "Slides", "s.pdf", "/p/s.pdf", "pdf", "application/pdf", 100L, false);
        mat.setId(18);

        when(materialRepository.findById(18)).thenReturn(Optional.of(mat));
        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 12)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                materialService.updateDownloadableStatus(otherInstructor, 18, true));
        verify(materialRepository, never()).save(any());
    }

    @Test
    void testUpdateDownloadableStatus_Fails_Student() {
        assertThrows(AccessDeniedException.class, () ->
                materialService.updateDownloadableStatus(student, 19, true));
        verify(materialRepository, never()).save(any());
    }

    @Test
    void testUpdateDownloadableStatus_Fails_NotFound() {
        when(materialRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                materialService.updateDownloadableStatus(instructor, 999, true));
        verify(materialRepository, never()).save(any());
    }
}

