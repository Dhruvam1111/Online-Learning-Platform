package com.olp.service;

import com.olp.dto.ContentDeclarationRequest;
import com.olp.dto.ContentDeclarationResponse;
import com.olp.dto.TakedownReplaceRequest;
import com.olp.dto.TakedownResponseRequest;
import com.olp.entity.*;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.ContentDeclarationRepository;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.LessonRepository;
import com.olp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentDeclarationServiceTest {

    @Mock
    private ContentDeclarationRepository declarationRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private CourseInstructorRepository courseInstructorRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ContentDeclarationService declarationService;

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
        instructor = new User("Jane Instructor", "jane@olp.com", "hash", "instructor");
        instructor.setId(10);

        coInstructor = new User("Co Instructor", "co@olp.com", "hash", "instructor");
        coInstructor.setId(11);

        otherInstructor = new User("Other Instructor", "other@olp.com", "hash", "instructor");
        otherInstructor.setId(12);

        admin = new User("Site Admin", "admin@olp.com", "hash", "admin");
        admin.setId(1);

        student = new User("Sam Student", "student@olp.com", "hash", "student");
        student.setId(20);

        course = new Course();
        course.setId(100);
        course.setTitle("Data Structures & Algorithms");
        course.setInstructor(instructor);

        module = new CourseModule();
        module.setId(200);
        module.setCourse(course);

        section = new CourseSection();
        section.setId(300);
        section.setModule(module);

        lesson = new Lesson();
        lesson.setId(400);
        lesson.setTitle("Binary Search Trees");
        lesson.setSection(section);
    }

    @Test
    void testDeclareContent_Success_CourseLevel() {
        ContentDeclarationRequest request = new ContentDeclarationRequest();
        request.setCourseId(100);
        request.setLicenseType(LicenseType.ORIGINAL_CREATOR);
        request.setDeclarationText("I confirm that I am the sole author and owner of all uploaded lecture materials.");

        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(declarationRepository.save(any(ContentDeclaration.class))).thenAnswer(invocation -> {
            ContentDeclaration cd = invocation.getArgument(0);
            cd.setId(1);
            return cd;
        });

        ContentDeclarationResponse response = declarationService.declareContent(instructor, request);

        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals(100, response.getCourseId());
        assertNull(response.getLessonId());
        assertEquals(LicenseType.ORIGINAL_CREATOR, response.getLicenseType());
        assertEquals(TakedownStatus.NONE, response.getTakedownStatus());
        verify(declarationRepository, times(1)).save(any(ContentDeclaration.class));
    }

    @Test
    void testDeclareContent_Success_LessonLevelWithAttribution() {
        ContentDeclarationRequest request = new ContentDeclarationRequest();
        request.setCourseId(100);
        request.setLessonId(400);
        request.setLicenseType(LicenseType.CREATIVE_COMMONS);
        request.setSourceAttribution("https://creativecommons.org/licenses/by/4.0/");
        request.setDeclarationText("Creative commons CC-BY attribution applied.");

        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        when(declarationRepository.save(any(ContentDeclaration.class))).thenAnswer(invocation -> {
            ContentDeclaration cd = invocation.getArgument(0);
            cd.setId(2);
            return cd;
        });

        ContentDeclarationResponse response = declarationService.declareContent(instructor, request);

        assertNotNull(response);
        assertEquals(2, response.getId());
        assertEquals(100, response.getCourseId());
        assertEquals(400, response.getLessonId());
        assertEquals("Binary Search Trees", response.getLessonTitle());
        assertEquals(LicenseType.CREATIVE_COMMONS, response.getLicenseType());
        assertEquals("https://creativecommons.org/licenses/by/4.0/", response.getSourceAttribution());
    }

    @Test
    void testDeclareContent_Success_CoInstructor() {
        ContentDeclarationRequest request = new ContentDeclarationRequest();
        request.setCourseId(100);
        request.setLicenseType(LicenseType.EDUCATIONAL_FAIR_USE);
        request.setDeclarationText("Co-instructor declaration under Fair Use guidelines.");

        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 11)).thenReturn(true);
        when(declarationRepository.save(any(ContentDeclaration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContentDeclarationResponse response = declarationService.declareContent(coInstructor, request);
        assertNotNull(response);
    }

    @Test
    void testDeclareContent_Success_Admin() {
        ContentDeclarationRequest request = new ContentDeclarationRequest();
        request.setCourseId(100);
        request.setLicenseType(LicenseType.PUBLIC_DOMAIN);
        request.setDeclarationText("Admin verified public domain content.");

        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(declarationRepository.save(any(ContentDeclaration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContentDeclarationResponse response = declarationService.declareContent(admin, request);
        assertNotNull(response);
    }

    @Test
    void testDeclareContent_Fails_Unauthenticated() {
        ContentDeclarationRequest request = new ContentDeclarationRequest();
        request.setCourseId(100);

        assertThrows(AccessDeniedException.class, () -> declarationService.declareContent(null, request));
    }

    @Test
    void testDeclareContent_Fails_StudentRole() {
        ContentDeclarationRequest request = new ContentDeclarationRequest();
        request.setCourseId(100);

        assertThrows(AccessDeniedException.class, () -> declarationService.declareContent(student, request));
    }

    @Test
    void testDeclareContent_Fails_UnauthorizedInstructor() {
        ContentDeclarationRequest request = new ContentDeclarationRequest();
        request.setCourseId(100);

        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 12)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> declarationService.declareContent(otherInstructor, request));
    }

    @Test
    void testDeclareContent_Fails_CourseNotFound() {
        ContentDeclarationRequest request = new ContentDeclarationRequest();
        request.setCourseId(999);

        when(courseRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> declarationService.declareContent(instructor, request));
    }

    @Test
    void testDeclareContent_Fails_LessonNotFound() {
        ContentDeclarationRequest request = new ContentDeclarationRequest();
        request.setCourseId(100);
        request.setLessonId(999);

        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(lessonRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> declarationService.declareContent(instructor, request));
    }

    @Test
    void testDeclareContent_Fails_LessonCourseMismatch() {
        Course otherCourse = new Course();
        otherCourse.setId(999);
        CourseModule otherModule = new CourseModule();
        otherModule.setCourse(otherCourse);
        CourseSection otherSection = new CourseSection();
        otherSection.setModule(otherModule);
        Lesson mismatchLesson = new Lesson();
        mismatchLesson.setId(500);
        mismatchLesson.setSection(otherSection);

        ContentDeclarationRequest request = new ContentDeclarationRequest();
        request.setCourseId(100);
        request.setLessonId(500);

        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(lessonRepository.findById(500)).thenReturn(Optional.of(mismatchLesson));

        assertThrows(IllegalArgumentException.class, () -> declarationService.declareContent(instructor, request));
    }

    @Test
    void testGetDeclarationsByCourse_Success() {
        ContentDeclaration cd = new ContentDeclaration(course, null, instructor, "Text", LicenseType.ORIGINAL_CREATOR,
                null);
        cd.setId(1);

        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(declarationRepository.findByCourseIdOrderByDeclaredAtDesc(100)).thenReturn(List.of(cd));

        List<ContentDeclarationResponse> list = declarationService.getDeclarationsByCourse(100, instructor);
        assertEquals(1, list.size());
        assertEquals(1, list.get(0).getId());
    }

    @Test
    void testGetDeclarationByLesson_Success_DirectLesson() {
        ContentDeclaration cd = new ContentDeclaration(course, lesson, instructor, "Text", LicenseType.ORIGINAL_CREATOR,
                null);
        cd.setId(10);

        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(declarationRepository.findFirstByLessonIdOrderByDeclaredAtDesc(400)).thenReturn(Optional.of(cd));

        ContentDeclarationResponse response = declarationService.getDeclarationByLesson(400, instructor);
        assertNotNull(response);
        assertEquals(10, response.getId());
    }

    @Test
    void testGetDeclarationByLesson_Success_FallbackCourse() {
        ContentDeclaration cd = new ContentDeclaration(course, null, instructor, "Text", LicenseType.ORIGINAL_CREATOR,
                null);
        cd.setId(11);

        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(declarationRepository.findFirstByLessonIdOrderByDeclaredAtDesc(400)).thenReturn(Optional.empty());
        when(declarationRepository.findFirstByCourseIdAndLessonIsNullOrderByDeclaredAtDesc(100))
                .thenReturn(Optional.of(cd));

        ContentDeclarationResponse response = declarationService.getDeclarationByLesson(400, instructor);
        assertNotNull(response);
        assertEquals(11, response.getId());
    }

    @Test
    void testGetDeclarationByLesson_NotFound() {
        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(declarationRepository.findFirstByLessonIdOrderByDeclaredAtDesc(400)).thenReturn(Optional.empty());
        when(declarationRepository.findFirstByCourseIdAndLessonIsNullOrderByDeclaredAtDesc(100))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> declarationService.getDeclarationByLesson(400, instructor));
    }

    @Test
    void testGetDeclarationById_Success() {
        ContentDeclaration cd = new ContentDeclaration(course, null, instructor, "Text", LicenseType.ORIGINAL_CREATOR,
                null);
        cd.setId(1);

        when(declarationRepository.findById(1)).thenReturn(Optional.of(cd));

        ContentDeclarationResponse response = declarationService.getDeclarationById(1, instructor);
        assertNotNull(response);
        assertEquals(1, response.getId());
    }

    @Test
    void testRespondToTakedown_Success() {
        ContentDeclaration cd = new ContentDeclaration(course, null, instructor, "Text", LicenseType.ORIGINAL_CREATOR,
                null);
        cd.setId(1);
        cd.setTakedownStatus(TakedownStatus.FLAGGED);

        TakedownResponseRequest req = new TakedownResponseRequest();
        req.setResponse("We hold explicit license from the publisher.");

        when(declarationRepository.findById(1)).thenReturn(Optional.of(cd));
        when(declarationRepository.save(any(ContentDeclaration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContentDeclarationResponse response = declarationService.respondToTakedown(1, instructor, req);
        assertEquals(TakedownStatus.RESPONDED, response.getTakedownStatus());
        assertEquals("We hold explicit license from the publisher.", response.getInstructorResponse());
        assertNotNull(response.getInstructorRespondedAt());
    }

    @Test
    void testRespondToTakedown_Fails_IfNotFlagged() {
        ContentDeclaration cd = new ContentDeclaration(course, null, instructor, "Text", LicenseType.ORIGINAL_CREATOR,
                null);
        cd.setId(1);
        cd.setTakedownStatus(TakedownStatus.NONE);

        TakedownResponseRequest req = new TakedownResponseRequest();
        req.setResponse("Response");

        when(declarationRepository.findById(1)).thenReturn(Optional.of(cd));

        assertThrows(IllegalStateException.class, () -> declarationService.respondToTakedown(1, instructor, req));
    }

    @Test
    void testRemoveContent_Success() {
        ContentDeclaration cd = new ContentDeclaration(course, null, instructor, "Text", LicenseType.ORIGINAL_CREATOR,
                null);
        cd.setId(1);
        cd.setTakedownStatus(TakedownStatus.FLAGGED);

        when(declarationRepository.findById(1)).thenReturn(Optional.of(cd));
        when(declarationRepository.save(any(ContentDeclaration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContentDeclarationResponse response = declarationService.removeContent(1, instructor);
        assertEquals(TakedownStatus.REMOVED, response.getTakedownStatus());
        assertNotNull(response.getResolvedAt());
    }

    @Test
    void testRemoveContent_Fails_IfInvalidState() {
        ContentDeclaration cd = new ContentDeclaration(course, null, instructor, "Text", LicenseType.ORIGINAL_CREATOR,
                null);
        cd.setId(1);
        cd.setTakedownStatus(TakedownStatus.NONE);

        when(declarationRepository.findById(1)).thenReturn(Optional.of(cd));

        assertThrows(IllegalStateException.class, () -> declarationService.removeContent(1, instructor));
    }

    @Test
    void testReplaceContent_Success() {
        ContentDeclaration cd = new ContentDeclaration(course, null, instructor, "Text", LicenseType.ORIGINAL_CREATOR,
                null);
        cd.setId(1);
        cd.setTakedownStatus(TakedownStatus.RESPONDED);

        TakedownReplaceRequest req = new TakedownReplaceRequest();
        req.setSourceAttribution("https://new-license.org/terms");

        when(declarationRepository.findById(1)).thenReturn(Optional.of(cd));
        when(declarationRepository.save(any(ContentDeclaration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContentDeclarationResponse response = declarationService.replaceContent(1, instructor, req);
        assertEquals(TakedownStatus.REPLACED, response.getTakedownStatus());
        assertEquals("https://new-license.org/terms", response.getSourceAttribution());
        assertNotNull(response.getResolvedAt());
    }

    @Test
    void testCheckDeclarationStatus_True() {
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(declarationRepository.existsByLessonIdAndTakedownStatusNot(400, TakedownStatus.REMOVED)).thenReturn(true);

        Map<String, Object> map = declarationService.checkDeclarationStatus(100, 400, instructor);
        assertTrue((Boolean) map.get("declared"));
        assertEquals(100, map.get("courseId"));
        assertEquals(400, map.get("lessonId"));
    }

    @Test
    void testAssertDeclared_Success() {
        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        when(declarationRepository.existsByLessonIdAndTakedownStatusNot(400, TakedownStatus.REMOVED)).thenReturn(true);

        assertDoesNotThrow(() -> declarationService.assertDeclared(400));
    }

    @Test
    void testAssertDeclared_Throws_WhenNotDeclared() {
        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        when(declarationRepository.existsByLessonIdAndTakedownStatusNot(400, TakedownStatus.REMOVED)).thenReturn(false);
        when(declarationRepository.existsByCourseIdAndLessonIsNullAndTakedownStatusNot(100, TakedownStatus.REMOVED))
                .thenReturn(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> declarationService.assertDeclared(400));
        assertTrue(ex.getMessage().contains("Content upload blocked"));
    }

    @Test
    void testAssertDeclared_Throws_WhenLessonNull() {
        assertThrows(IllegalArgumentException.class, () -> declarationService.assertDeclared(null));
    }

    @Test
    void testResolveUser_PrincipalIsUser() {
        Authentication auth = new UsernamePasswordAuthenticationToken(instructor, null);
        User resolved = declarationService.resolveUser(auth);
        assertEquals(instructor, resolved);
    }

    @Test
    void testResolveUser_PrincipalIsString_FindsByEmail() {
        Authentication auth = new UsernamePasswordAuthenticationToken("jane@olp.com", null);
        when(userRepository.findByEmail("jane@olp.com")).thenReturn(Optional.of(instructor));

        User resolved = declarationService.resolveUser(auth);
        assertEquals(instructor, resolved);
    }

    @Test
    void testResolveUser_NullAuth_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class, () -> declarationService.resolveUser(null));
    }
}
