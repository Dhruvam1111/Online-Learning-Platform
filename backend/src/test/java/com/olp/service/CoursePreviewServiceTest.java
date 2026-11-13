package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoursePreviewServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseModuleRepository courseModuleRepository;
    @Mock
    private CourseSectionRepository courseSectionRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LectureRepository lectureRepository;
    @Mock
    private CourseInstructorRepository courseInstructorRepository;
    @Mock
    private InstructorProfileRepository instructorProfileRepository;
    @Mock
    private CoursePrerequisiteRepository coursePrerequisiteRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private CoursePricingService coursePricingService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CoursePreviewService previewService;

    private User ownerInstructor;
    private User coInstructor;
    private User otherInstructor;
    private User pendingInstructor;
    private User studentUser;
    private User adminUser;
    private Course draftCourse;

    @BeforeEach
    void setUp() {
        ownerInstructor = new User("Dr. Smith", "smith@olp.com", "hash", "instructor", "active");
        ownerInstructor.setId(1);

        coInstructor = new User("Prof. Alan", "alan@olp.com", "hash", "instructor", "active");
        coInstructor.setId(2);

        otherInstructor = new User("Dr. Jane", "jane@olp.com", "hash", "instructor", "active");
        otherInstructor.setId(3);

        pendingInstructor = new User("Pending User", "pending@olp.com", "hash", "instructor", "pending");
        pendingInstructor.setId(4);

        studentUser = new User("Student Bob", "bob@olp.com", "hash", "student", "active");
        studentUser.setId(5);

        adminUser = new User("Admin John", "admin@olp.com", "hash", "admin", "active");
        adminUser.setId(99);

        draftCourse = new Course(ownerInstructor, "Intro to Quantum Computing", "Comprehensive draft guide",
                CourseStatus.DRAFT);
        draftCourse.setId(10);
        draftCourse.setCategory("Computer Science");
        draftCourse.setPrice(new BigDecimal("1200.00"));
        draftCourse.setDiscountType(DiscountType.PERCENTAGE);
        draftCourse.setDiscountValue(new BigDecimal("10.00"));
        draftCourse.setLearningOutcomesList(List.of("Understand qubits", "Build quantum circuits"));
    }

    // =========================================================================
    // 1. DRAFT COURSE PREVIEW & CONTENT STRUCTURE
    // =========================================================================

    @Test
    @DisplayName("Preview draft course: renders description, modules, sections, lessons, pricing and quizzes with read-only banner")
    void testPreviewDraftCourse_RendersCompleteDetailsAndReadOnlyBanner() {
        // Prepare modules & hierarchy
        CourseModule module1 = new CourseModule(draftCourse, "Module 1: Basics", "Basics overview", 1);
        module1.setId(101);

        CourseSection section1 = new CourseSection(module1, "Section 1.1: Math Foundations", "Linear algebra", 1);
        section1.setId(201);

        Lesson lesson1 = new Lesson(section1, "Lesson 1: Complex Vectors", LessonType.VIDEO, 1);
        lesson1.setId(301);
        lesson1.setDurationMinutes(15);
        lesson1.setContentUrl("https://video.olp.com/v1");

        when(courseRepository.findById(10)).thenReturn(Optional.of(draftCourse));
        when(courseModuleRepository.findByCourseIdOrderByOrderIndexAsc(10)).thenReturn(List.of(module1));
        when(courseSectionRepository.findByModuleIdOrderByOrderIndexAsc(101)).thenReturn(List.of(section1));
        when(lessonRepository.findBySectionIdOrderByOrderIndexAsc(201)).thenReturn(List.of(lesson1));

        // Lectures & Quizzes
        Lecture lecture1 = new Lecture(draftCourse, "Intro Lecture", "https://youtube.com/watch?v=123", "123", 1);
        lecture1.setId(401);
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(10)).thenReturn(List.of(lecture1));

        Quiz quiz1 = new Quiz();
        quiz1.setId(501);
        quiz1.setCourse(draftCourse);
        quiz1.setTitle("Quantum Quiz 1");
        quiz1.setDescription("Testing basic concepts");
        quiz1.setTotalMarks(new BigDecimal("20.00"));
        quiz1.setStatus(QuizStatus.DRAFT);
        when(quizRepository.findByCourseIdOrderByCreatedAtDesc(10)).thenReturn(List.of(quiz1));

        // Pricing service
        CoursePricingResponse pricingResp = new CoursePricingResponse();
        pricingResp.setCourseId(10);
        pricingResp.setBasePrice(new BigDecimal("1200.00"));
        pricingResp.setDiscountAmount(new BigDecimal("120.00"));
        pricingResp.setDiscountedPrice(new BigDecimal("1080.00"));
        pricingResp.setTaxAmount(new BigDecimal("194.40"));
        pricingResp.setFinalPayableAmount(new BigDecimal("1274.40"));
        pricingResp.setFree(false);
        when(coursePricingService.calculatePricing(draftCourse)).thenReturn(pricingResp);

        CoursePreviewResponse response = previewService.previewCourseAsStudent(ownerInstructor, 10);

        assertNotNull(response);
        // Preview flags
        assertTrue(response.isPreview());
        assertTrue(response.getPreview());
        assertTrue(response.isReadOnly());
        assertFalse(response.isCanEnroll());
        assertFalse(response.isEnrolled());
        assertFalse(response.isProgressTrackingEnabled());
        assertNotNull(response.getBannerMessage());
        assertTrue(response.getBannerMessage().toLowerCase().contains("previewing"));

        // Course details
        assertEquals(10, response.getId());
        assertEquals("Intro to Quantum Computing", response.getTitle());
        assertEquals("Comprehensive draft guide", response.getDescription());
        assertEquals("Computer Science", response.getCategory());
        assertEquals("DRAFT", response.getStatus());
        assertEquals(1, response.getInstructorId());
        assertEquals("Dr. Smith", response.getInstructorName());

        // Structure counts and items
        assertEquals(1, response.getTotalModules());
        assertEquals(1, response.getTotalSections());
        assertEquals(1, response.getTotalLessons());
        assertEquals(1, response.getTotalLectures());
        assertEquals(1, response.getModules().size());
        assertEquals("Module 1: Basics", response.getModules().get(0).getTitle());
        assertEquals(1, response.getModules().get(0).getSections().size());
        assertEquals("Section 1.1: Math Foundations", response.getModules().get(0).getSections().get(0).getTitle());
        assertEquals(1, response.getModules().get(0).getSections().get(0).getLessons().size());
        assertEquals("Lesson 1: Complex Vectors",
                response.getModules().get(0).getSections().get(0).getLessons().get(0).getTitle());

        // Lectures & Quizzes
        assertEquals(1, response.getLectures().size());
        assertEquals("Intro Lecture", response.getLectures().get(0).getTitle());
        assertEquals(1, response.getQuizzes().size());
        assertEquals("Quantum Quiz 1", response.getQuizzes().get(0).getTitle());

        // Pricing
        assertNotNull(response.getPricing());
        assertEquals(new BigDecimal("1200.00"), response.getPrice());
        assertEquals(new BigDecimal("1274.40"), response.getFinalPayableAmount());
        assertFalse(response.isFree());
    }

    @Test
    @DisplayName("Preview published course: preview mode still enforces read-only banner and disables enrollment")
    void testPreviewPublishedCourse_EnforcesReadOnly() {
        draftCourse.setStatus(CourseStatus.PUBLISHED);
        when(courseRepository.findById(10)).thenReturn(Optional.of(draftCourse));
        when(courseModuleRepository.findByCourseIdOrderByOrderIndexAsc(10)).thenReturn(Collections.emptyList());
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(10)).thenReturn(Collections.emptyList());

        CoursePreviewResponse response = previewService.previewCourseAsStudent(ownerInstructor, 10);

        assertNotNull(response);
        assertEquals("PUBLISHED", response.getStatus());
        assertTrue(response.isPreview());
        assertTrue(response.isReadOnly());
        assertFalse(response.isCanEnroll());
        assertFalse(response.isEnrolled());
        assertFalse(response.isProgressTrackingEnabled());
    }

    // =========================================================================
    // 2. AUTHORIZATION & ACCESS CONTROL
    // =========================================================================

    @Test
    @DisplayName("Authorized Co-Instructor can access course preview")
    void testPreview_AuthorizedCoInstructor_Success() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(draftCourse));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 2)).thenReturn(true);

        CoursePreviewResponse response = previewService.previewCourseAsStudent(coInstructor, 10);

        assertNotNull(response);
        assertEquals(10, response.getId());
    }

    @Test
    @DisplayName("Admin can preview any course")
    void testPreview_Admin_Success() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(draftCourse));

        CoursePreviewResponse response = previewService.previewCourseAsStudent(adminUser, 10);

        assertNotNull(response);
        assertEquals(10, response.getId());
    }

    @Test
    @DisplayName("Rejects unauthenticated user with InvalidCredentialsException")
    void testPreview_Unauthenticated_ThrowsInvalidCredentials() {
        assertThrows(InvalidCredentialsException.class, () -> previewService.previewCourseAsStudent(null, 10));
    }

    @Test
    @DisplayName("Rejects student user with AccessDeniedException")
    void testPreview_StudentUser_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class, () -> previewService.previewCourseAsStudent(studentUser, 10));
    }

    @Test
    @DisplayName("Rejects pending instructor with AccessDeniedException")
    void testPreview_PendingInstructor_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class, () -> previewService.previewCourseAsStudent(pendingInstructor, 10));
    }

    @Test
    @DisplayName("Rejects instructor not associated with the course with AccessDeniedException")
    void testPreview_UnassociatedInstructor_ThrowsAccessDenied() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(draftCourse));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(10, 3)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> previewService.previewCourseAsStudent(otherInstructor, 10));
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when course does not exist")
    void testPreview_CourseNotFound_ThrowsResourceNotFoundException() {
        when(courseRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> previewService.previewCourseAsStudent(ownerInstructor, 999));
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when courseId is null")
    void testPreview_NullCourseId_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> previewService.previewCourseAsStudent(ownerInstructor, null));
    }

    // =========================================================================
    // 3. INCOMPLETE COURSE HANDLING & MISSING CONTENT WARNINGS
    // =========================================================================

    @Test
    @DisplayName("Incomplete course: null description, category, modules, and price produces helpful warnings without errors")
    void testPreviewIncompleteCourse_ProducesWarningsWithoutFailing() {
        Course emptyCourse = new Course(ownerInstructor, "Empty Draft", null, CourseStatus.DRAFT);
        emptyCourse.setId(25);
        emptyCourse.setCategory(null);
        emptyCourse.setPrice(null);

        when(courseRepository.findById(25)).thenReturn(Optional.of(emptyCourse));
        when(courseModuleRepository.findByCourseIdOrderByOrderIndexAsc(25)).thenReturn(Collections.emptyList());
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(25)).thenReturn(Collections.emptyList());

        CoursePreviewResponse response = previewService.previewCourseAsStudent(ownerInstructor, 25);

        assertNotNull(response);
        assertEquals(25, response.getId());
        assertEquals("Empty Draft", response.getTitle());
        assertNull(response.getDescription());
        assertEquals(0, response.getTotalModules());
        assertEquals(0, response.getTotalLessons());
        assertTrue(response.isFree());
        assertEquals(new BigDecimal("0.00"), response.getPrice());

        // Check missing content warnings
        List<String> warnings = response.getMissingContentWarnings();
        assertNotNull(warnings);
        assertFalse(warnings.isEmpty());
        assertTrue(warnings.stream().anyMatch(w -> w.toLowerCase().contains("description")));
        assertTrue(warnings.stream().anyMatch(w -> w.toLowerCase().contains("category")));
        assertTrue(warnings.stream().anyMatch(w -> w.toLowerCase().contains("no modules")));
        assertTrue(warnings.stream().anyMatch(w -> w.toLowerCase().contains("price")));
    }

    @Test
    @DisplayName("Incomplete course: module without sections or section without lessons produces granular warnings")
    void testPreviewIncompleteCourse_ModuleWithoutSectionsOrLessons_ProducesGranularWarnings() {
        CourseModule emptyModule = new CourseModule(draftCourse, "Setup", "Env setup", 1);
        emptyModule.setId(102);

        CourseModule moduleWithEmptySection = new CourseModule(draftCourse, "Deep Dive", "Advanced topics", 2);
        moduleWithEmptySection.setId(103);
        CourseSection emptySection = new CourseSection(moduleWithEmptySection, "Advanced Math", "Math", 1);
        emptySection.setId(203);

        when(courseRepository.findById(10)).thenReturn(Optional.of(draftCourse));
        when(courseModuleRepository.findByCourseIdOrderByOrderIndexAsc(10))
                .thenReturn(List.of(emptyModule, moduleWithEmptySection));
        when(courseSectionRepository.findByModuleIdOrderByOrderIndexAsc(102)).thenReturn(Collections.emptyList());
        when(courseSectionRepository.findByModuleIdOrderByOrderIndexAsc(103)).thenReturn(List.of(emptySection));
        when(lessonRepository.findBySectionIdOrderByOrderIndexAsc(203)).thenReturn(Collections.emptyList());

        CoursePreviewResponse response = previewService.previewCourseAsStudent(ownerInstructor, 10);

        assertNotNull(response);
        List<String> warnings = response.getMissingContentWarnings();
        assertNotNull(warnings);
        assertTrue(warnings.stream().anyMatch(w -> w.contains("Module 'Setup' has no sections")));
        assertTrue(warnings.stream()
                .anyMatch(w -> w.contains("Section 'Advanced Math' in module 'Deep Dive' has no lessons")));
    }

    // =========================================================================
    // 4. READ-ONLY ENFORCEMENT & IMMUTABILITY VERIFICATION
    // =========================================================================

    @Test
    @DisplayName("Strict read-only: preview never alters course data or creates enrollment/progress records")
    void testPreview_NeverCreatesEnrollmentsOrMutatesDatabase() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(draftCourse));

        CoursePreviewResponse response = previewService.previewCourseAsStudent(ownerInstructor, 10);

        assertNotNull(response);
        // Verify repository save / delete were NEVER called on courses or any entity
        verify(courseRepository, never()).save(any());
        verify(courseRepository, never()).delete(any());
        verifyNoInteractions(userRepository);
    }

    // =========================================================================
    // 5. RESOLVE USER AUTHENTICATION HELPER
    // =========================================================================

    @Test
    @DisplayName("resolveUser: resolves directly from User principal or via UserRepository")
    void testResolveUser_PrincipalVariations() {
        Authentication authDirect = new UsernamePasswordAuthenticationToken(ownerInstructor, "credentials");
        User resolved = previewService.resolveUser(authDirect);
        assertSame(ownerInstructor, resolved);

        Authentication authEmail = new UsernamePasswordAuthenticationToken("alan@olp.com", "credentials");
        when(userRepository.findByEmail("alan@olp.com")).thenReturn(Optional.of(coInstructor));
        User resolvedFromEmail = previewService.resolveUser(authEmail);
        assertSame(coInstructor, resolvedFromEmail);

        assertThrows(InvalidCredentialsException.class, () -> previewService.resolveUser(null));
    }
}
