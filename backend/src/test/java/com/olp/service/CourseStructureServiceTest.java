package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseStructureServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseModuleRepository courseModuleRepository;

    @Mock
    private CourseSectionRepository courseSectionRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CourseStructureService service;

    private User owner;
    private User coInstructor;
    private User stranger;
    private User pendingInstructor;
    private User student;
    private Course course;

    @BeforeEach
    void setUp() {
        owner = new User("Dr. Owner", "owner@olp.com", "hash", "instructor", "active");
        owner.setId(1);

        coInstructor = new User("Dr. Co", "co@olp.com", "hash", "instructor", "active");
        coInstructor.setId(2);

        stranger = new User("Dr. Stranger", "stranger@olp.com", "hash", "instructor", "active");
        stranger.setId(3);

        pendingInstructor = new User("Dr. Pending", "pending@olp.com", "hash", "instructor", "pending");
        pendingInstructor.setId(4);

        student = new User("Sam Student", "student@olp.com", "hash", "student", "active");
        student.setId(5);

        course = new Course(owner, "Full Stack Web Development", "Description", CourseStatus.DRAFT);
        course.setId(10);
    }

    private void stubCourseAccess() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
    }

    // =========================================================================
    // AUTHORIZATION TESTS
    // =========================================================================

    @Test
    void testVerifyCourseAssociation_Owner_Success() {
        stubCourseAccess();
        Course result = service.verifyCourseAssociation(10, owner);
        assertSame(course, result);
    }

    @Test
    void testVerifyCourseAssociation_CoInstructor_Success() {
        stubCourseAccess();
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 2)).thenReturn(true);

        Course result = service.verifyCourseAssociation(10, coInstructor);
        assertSame(course, result);
    }

    @Test
    void testVerifyCourseAssociation_Stranger_ThrowsAccessDenied() {
        stubCourseAccess();
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 3)).thenReturn(false);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> service.verifyCourseAssociation(10, stranger));
        assertTrue(ex.getMessage().contains("not an authorized instructor"));
    }

    @Test
    void testVerifyCourseAssociation_PendingInstructor_ThrowsAccessDenied() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> service.verifyCourseAssociation(10, pendingInstructor));
        assertTrue(ex.getMessage().contains("awaiting admin approval"));
    }

    @Test
    void testVerifyCourseAssociation_Student_ThrowsAccessDenied() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> service.verifyCourseAssociation(10, student));
        assertTrue(ex.getMessage().contains("only instructors or admins"));
    }

    @Test
    void testVerifyCourseAssociation_CourseNotFound_ThrowsResourceNotFound() {
        when(courseRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.verifyCourseAssociation(999, owner));
    }

    // =========================================================================
    // MODULE TESTS
    // =========================================================================

    @Test
    void testCreateModule_Success() {
        stubCourseAccess();
        when(courseModuleRepository.findMaxOrderIndexByCourseId(10)).thenReturn(0);
        when(courseModuleRepository.save(any(CourseModule.class))).thenAnswer(inv -> {
            CourseModule m = inv.getArgument(0);
            m.setId(101);
            return m;
        });

        CourseModuleRequest request = new CourseModuleRequest("HTML Basics", "Learn tags", null);
        CourseModuleResponse response = service.createModule(owner, 10, request);

        assertNotNull(response);
        assertEquals(101, response.getId());
        assertEquals("HTML Basics", response.getTitle());
        assertEquals("Learn tags", response.getDescription());
        assertEquals(1, response.getOrderIndex());
    }

    @Test
    void testCreateModule_BlankTitle_Throws() {
        stubCourseAccess();
        CourseModuleRequest request = new CourseModuleRequest("   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createModule(owner, 10, request));
        assertTrue(ex.getMessage().contains("Module title cannot be blank"));
    }

    @Test
    void testGetModule_Success() {
        stubCourseAccess();
        CourseModule module = new CourseModule(course, "Module 1", "Desc", 1);
        module.setId(101);
        when(courseModuleRepository.findByIdAndCourseId(101, 10)).thenReturn(Optional.of(module));

        CourseModuleResponse response = service.getModule(owner, 10, 101);

        assertNotNull(response);
        assertEquals(101, response.getId());
        assertEquals("Module 1", response.getTitle());
    }

    @Test
    void testUpdateModule_Success() {
        stubCourseAccess();
        CourseModule module = new CourseModule(course, "Old Title", "Old Desc", 1);
        module.setId(101);
        when(courseModuleRepository.findByIdAndCourseId(101, 10)).thenReturn(Optional.of(module));
        when(courseModuleRepository.save(any(CourseModule.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseModuleRequest updateReq = new CourseModuleRequest("New Title", "New Desc", 2);
        CourseModuleResponse response = service.updateModule(owner, 10, 101, updateReq);

        assertEquals("New Title", response.getTitle());
        assertEquals("New Desc", response.getDescription());
        assertEquals(2, response.getOrderIndex());
    }

    @Test
    void testDeleteModule_Success() {
        stubCourseAccess();
        CourseModule module = new CourseModule(course, "Module 1", "Desc", 1);
        module.setId(101);
        when(courseModuleRepository.findByIdAndCourseId(101, 10)).thenReturn(Optional.of(module));

        ApiResponse response = service.deleteModule(owner, 10, 101);

        assertTrue(response.isSuccess());
        verify(courseModuleRepository, times(1)).delete(module);
    }

    @Test
    void testReorderModules_Success() {
        stubCourseAccess();
        CourseModule m1 = new CourseModule(course, "M1", null, 1);
        m1.setId(101);
        CourseModule m2 = new CourseModule(course, "M2", null, 2);
        m2.setId(102);

        when(courseModuleRepository.findByIdAndCourseId(102, 10)).thenReturn(Optional.of(m2));
        when(courseModuleRepository.findByIdAndCourseId(101, 10)).thenReturn(Optional.of(m1));
        when(courseModuleRepository.findByCourseIdOrderByOrderIndexAsc(10)).thenReturn(List.of(m2, m1));

        ReorderStructureRequest req = new ReorderStructureRequest(List.of(102, 101));
        List<CourseModuleResponse> result = service.reorderModules(owner, 10, req);

        assertEquals(2, result.size());
        assertEquals(1, m2.getOrderIndex());
        assertEquals(2, m1.getOrderIndex());
        verify(courseModuleRepository, times(2)).save(any(CourseModule.class));
    }

    @Test
    void testReorderModules_DuplicateIds_Throws() {
        stubCourseAccess();
        ReorderStructureRequest req = new ReorderStructureRequest(List.of(101, 101));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.reorderModules(owner, 10, req));
        assertTrue(ex.getMessage().contains("Duplicate module ID"));
    }

    // =========================================================================
    // SECTION TESTS
    // =========================================================================

    @Test
    void testCreateSection_Success() {
        stubCourseAccess();
        CourseModule module = new CourseModule(course, "Module 1", null, 1);
        module.setId(101);
        when(courseModuleRepository.findByIdAndCourseId(101, 10)).thenReturn(Optional.of(module));
        when(courseSectionRepository.findMaxOrderIndexByModuleId(101)).thenReturn(0);
        when(courseSectionRepository.save(any(CourseSection.class))).thenAnswer(inv -> {
            CourseSection s = inv.getArgument(0);
            s.setId(201);
            return s;
        });

        CourseSectionRequest req = new CourseSectionRequest("Section 1", "Desc", null);
        CourseSectionResponse response = service.createSection(owner, 10, 101, req);

        assertNotNull(response);
        assertEquals(201, response.getId());
        assertEquals("Section 1", response.getTitle());
        assertEquals(1, response.getOrderIndex());
    }

    @Test
    void testCreateSection_WrongModuleForCourse_ThrowsResourceNotFound() {
        stubCourseAccess();
        when(courseModuleRepository.findByIdAndCourseId(999, 10)).thenReturn(Optional.empty());

        CourseSectionRequest req = new CourseSectionRequest("Section 1");
        assertThrows(ResourceNotFoundException.class,
                () -> service.createSection(owner, 10, 999, req));
    }

    @Test
    void testUpdateSection_Success() {
        stubCourseAccess();
        CourseModule module = new CourseModule(course, "Module 1", null, 1);
        module.setId(101);
        CourseSection section = new CourseSection(module, "Old Section", null, 1);
        section.setId(201);

        when(courseModuleRepository.findByIdAndCourseId(101, 10)).thenReturn(Optional.of(module));
        when(courseSectionRepository.findByIdAndModuleId(201, 101)).thenReturn(Optional.of(section));
        when(courseSectionRepository.save(any(CourseSection.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseSectionRequest req = new CourseSectionRequest("Updated Section", "Updated Desc", 3);
        CourseSectionResponse response = service.updateSection(owner, 10, 101, 201, req);

        assertEquals("Updated Section", response.getTitle());
        assertEquals("Updated Desc", response.getDescription());
        assertEquals(3, response.getOrderIndex());
    }

    @Test
    void testDeleteSection_Success() {
        stubCourseAccess();
        CourseModule module = new CourseModule(course, "Module 1", null, 1);
        module.setId(101);
        CourseSection section = new CourseSection(module, "Section 1", null, 1);
        section.setId(201);

        when(courseModuleRepository.findByIdAndCourseId(101, 10)).thenReturn(Optional.of(module));
        when(courseSectionRepository.findByIdAndModuleId(201, 101)).thenReturn(Optional.of(section));

        ApiResponse response = service.deleteSection(owner, 10, 101, 201);

        assertTrue(response.isSuccess());
        verify(courseSectionRepository, times(1)).delete(section);
    }

    // =========================================================================
    // LESSON TESTS & 4 LESSON TYPES (FR-I06)
    // =========================================================================

    @Test
    void testCreateLesson_AllFourTypesSupported() {
        stubCourseAccess();
        CourseModule module = new CourseModule(course, "Module 1", null, 1);
        module.setId(101);
        CourseSection section = new CourseSection(module, "Section 1", null, 1);
        section.setId(201);

        when(courseModuleRepository.findByIdAndCourseId(101, 10)).thenReturn(Optional.of(module));
        when(courseSectionRepository.findByIdAndModuleId(201, 101)).thenReturn(Optional.of(section));
        when(lessonRepository.findMaxOrderIndexBySectionId(201)).thenReturn(0);
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> {
            Lesson l = inv.getArgument(0);
            l.setId(301);
            return l;
        });

        // 1. VIDEO
        LessonRequest videoReq = new LessonRequest("Video Lesson", "VIDEO", "Overview", "https://youtube.com/v1", 10,
                null, "{}");
        LessonResponse videoRes = service.createLesson(owner, 10, 101, 201, videoReq);
        assertEquals("VIDEO", videoRes.getLessonType());
        assertEquals("https://youtube.com/v1", videoRes.getContentUrl());

        // 2. MATERIAL
        LessonRequest materialReq = new LessonRequest("Reading Material", "MATERIAL", "# Markdown Guide", null, 15,
                null, null);
        LessonResponse matRes = service.createLesson(owner, 10, 101, 201, materialReq);
        assertEquals("MATERIAL", matRes.getLessonType());

        // 3. QUIZ
        LessonRequest quizReq = new LessonRequest("Quiz Check", "QUIZ", "Quiz instructions", null, 5, null,
                "{\"quizId\": 10}");
        LessonResponse quizRes = service.createLesson(owner, 10, 101, 201, quizReq);
        assertEquals("QUIZ", quizRes.getLessonType());

        // 4. ASSIGNMENT
        LessonRequest assignReq = new LessonRequest("Final Assignment", "ASSIGNMENT", "Build portfolio", null, 60, null,
                null);
        LessonResponse assignRes = service.createLesson(owner, 10, 101, 201, assignReq);
        assertEquals("ASSIGNMENT", assignRes.getLessonType());
    }

    @Test
    void testCreateLesson_InvalidType_Throws() {
        stubCourseAccess();
        CourseModule module = new CourseModule(course, "Module 1", null, 1);
        module.setId(101);
        CourseSection section = new CourseSection(module, "Section 1", null, 1);
        section.setId(201);

        when(courseModuleRepository.findByIdAndCourseId(101, 10)).thenReturn(Optional.of(module));
        when(courseSectionRepository.findByIdAndModuleId(201, 101)).thenReturn(Optional.of(section));

        LessonRequest req = new LessonRequest("Lesson", "INVALID_TYPE");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createLesson(owner, 10, 101, 201, req));
        assertTrue(ex.getMessage().contains("Invalid lesson type"));
    }

    @Test
    void testLesson_AncestorChainIntegrity_MismatchThrowsResourceNotFound() {
        stubCourseAccess();
        CourseModule module = new CourseModule(course, "Module 1", null, 1);
        module.setId(101);

        // Section does NOT belong to module 101
        when(courseModuleRepository.findByIdAndCourseId(101, 10)).thenReturn(Optional.of(module));
        when(courseSectionRepository.findByIdAndModuleId(202, 101)).thenReturn(Optional.empty());

        LessonRequest req = new LessonRequest("Lesson 1", "VIDEO");
        assertThrows(ResourceNotFoundException.class,
                () -> service.createLesson(owner, 10, 101, 202, req));
    }

    @Test
    void testUpdateLesson_Success() {
        stubCourseAccess();
        CourseModule module = new CourseModule(course, "Module 1", null, 1);
        module.setId(101);
        CourseSection section = new CourseSection(module, "Section 1", null, 1);
        section.setId(201);
        Lesson lesson = new Lesson(section, "Old Lesson", LessonType.VIDEO, 1);
        lesson.setId(301);

        when(courseModuleRepository.findByIdAndCourseId(101, 10)).thenReturn(Optional.of(module));
        when(courseSectionRepository.findByIdAndModuleId(201, 101)).thenReturn(Optional.of(section));
        when(lessonRepository.findByIdAndSectionId(301, 201)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));

        LessonRequest req = new LessonRequest("New Lesson Title", "MATERIAL", "Updated Content", "https://doc.com", 25,
                2, "{\"scorm\": true}");
        LessonResponse response = service.updateLesson(owner, 10, 101, 201, 301, req);

        assertEquals("New Lesson Title", response.getTitle());
        assertEquals("MATERIAL", response.getLessonType());
        assertEquals("Updated Content", response.getContent());
        assertEquals("https://doc.com", response.getContentUrl());
        assertEquals(25, response.getDurationMinutes());
        assertEquals(2, response.getOrderIndex());
        assertEquals("{\"scorm\": true}", response.getMetadata());
    }

    @Test
    void testDeleteLesson_Success() {
        stubCourseAccess();
        CourseModule module = new CourseModule(course, "Module 1", null, 1);
        module.setId(101);
        CourseSection section = new CourseSection(module, "Section 1", null, 1);
        section.setId(201);
        Lesson lesson = new Lesson(section, "Lesson 1", LessonType.VIDEO, 1);
        lesson.setId(301);

        when(courseModuleRepository.findByIdAndCourseId(101, 10)).thenReturn(Optional.of(module));
        when(courseSectionRepository.findByIdAndModuleId(201, 101)).thenReturn(Optional.of(section));
        when(lessonRepository.findByIdAndSectionId(301, 201)).thenReturn(Optional.of(lesson));

        ApiResponse response = service.deleteLesson(owner, 10, 101, 201, 301);

        assertTrue(response.isSuccess());
        verify(lessonRepository, times(1)).delete(lesson);
    }

    @Test
    void testReorderLessons_Success() {
        stubCourseAccess();
        CourseModule module = new CourseModule(course, "Module 1", null, 1);
        module.setId(101);
        CourseSection section = new CourseSection(module, "Section 1", null, 1);
        section.setId(201);
        Lesson l1 = new Lesson(section, "L1", LessonType.VIDEO, 1);
        l1.setId(301);
        Lesson l2 = new Lesson(section, "L2", LessonType.MATERIAL, 2);
        l2.setId(302);

        when(courseModuleRepository.findByIdAndCourseId(101, 10)).thenReturn(Optional.of(module));
        when(courseSectionRepository.findByIdAndModuleId(201, 101)).thenReturn(Optional.of(section));
        when(lessonRepository.findByIdAndSectionId(302, 201)).thenReturn(Optional.of(l2));
        when(lessonRepository.findByIdAndSectionId(301, 201)).thenReturn(Optional.of(l1));
        when(lessonRepository.findBySectionIdOrderByOrderIndexAsc(201)).thenReturn(List.of(l2, l1));

        ReorderStructureRequest req = new ReorderStructureRequest(List.of(302, 301));
        List<LessonResponse> result = service.reorderLessons(owner, 10, 101, 201, req);

        assertEquals(2, result.size());
        assertEquals(1, l2.getOrderIndex());
        assertEquals(2, l1.getOrderIndex());
        verify(lessonRepository, times(2)).save(any(Lesson.class));
    }

    // =========================================================================
    // FULL STRUCTURE TREE TEST
    // =========================================================================

    @Test
    void testGetCourseStructure_ReturnsCompleteHierarchy() {
        stubCourseAccess();

        CourseModule m1 = new CourseModule(course, "Module 1", "Desc 1", 1);
        m1.setId(101);
        CourseSection s1 = new CourseSection(m1, "Section 1.1", "Desc 1.1", 1);
        s1.setId(201);
        Lesson l1 = new Lesson(s1, "Lesson 1.1.1", LessonType.VIDEO, 1);
        l1.setId(301);

        s1.setLessons(List.of(l1));
        m1.setSections(List.of(s1));

        when(courseModuleRepository.findByCourseIdOrderByOrderIndexAsc(10)).thenReturn(List.of(m1));

        CourseStructureResponse response = service.getCourseStructure(owner, 10);

        assertNotNull(response);
        assertEquals(10, response.getCourseId());
        assertEquals("Full Stack Web Development", response.getCourseTitle());
        assertEquals(1, response.getTotalModules());
        assertEquals(1, response.getTotalSections());
        assertEquals(1, response.getTotalLessons());
        assertEquals(1, response.getModules().size());
        assertEquals("Module 1", response.getModules().get(0).getTitle());
        assertEquals(1, response.getModules().get(0).getSections().size());
        assertEquals("Section 1.1", response.getModules().get(0).getSections().get(0).getTitle());
        assertEquals(1, response.getModules().get(0).getSections().get(0).getLessons().size());
        assertEquals("Lesson 1.1.1", response.getModules().get(0).getSections().get(0).getLessons().get(0).getTitle());
    }
}
