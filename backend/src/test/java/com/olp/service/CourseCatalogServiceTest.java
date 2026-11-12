package com.olp.service;

import com.olp.dto.CourseDetailResponse;
import com.olp.dto.CourseInstructorResponse;
import com.olp.dto.LectureResponse;
import com.olp.dto.PublicCourseResponse;
import com.olp.entity.Course;
import com.olp.entity.CourseInstructor;
import com.olp.entity.CourseInstructorRole;
import com.olp.entity.CourseStatus;
import com.olp.entity.InstructorProfile;
import com.olp.entity.Lecture;
import com.olp.entity.User;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseCatalogServiceTest {

    @Mock private CourseRepository courseRepository;
    @Mock private LectureRepository lectureRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private BookmarkRepository bookmarkRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private CourseInstructorRepository courseInstructorRepository;
    @Mock private InstructorProfileRepository instructorProfileRepository;

    @InjectMocks
    private CourseCatalogService service;

    private Course course;
    private User instructor;

    @BeforeEach
    void setUp() {
        instructor = new User("Ivy", "ivy@olp.com", "hash", "instructor", "active");
        instructor.setId(10);
        course = new Course(instructor, "Java", "desc", CourseStatus.PUBLISHED);
        course.setId(1);
        Lecture lecture = new Lecture(course, "Intro", "https://youtu.be/dQw4w9WgXcQ", "dQw4w9WgXcQ", 1);
        lecture.setId(100);
        lenient().when(courseRepository.findById(1)).thenReturn(Optional.of(course));
        lenient().when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(1)).thenReturn(List.of(lecture));
    }

    private void assertMasked(LectureResponse l) {
        assertEquals("Intro", l.getTitle());
        assertEquals(1, l.getOrderIndex());
        assertNull(l.getVideoUrl());
        assertNull(l.getYoutubeUrl());
        assertNull(l.getVideoId());
        assertNull(l.getEmbedUrl());
        assertNull(l.getThumbnailUrl());
        assertTrue(l.isLocked());
    }

    @Test
    void anonymousVisitorGetsMaskedLectures() {
        assertMasked(service.getCourseLectures(1, null).get(0));
    }

    @Test
    void nonEnrolledStudentGetsMaskedLectures() {
        User student = new User("Sam", "sam@olp.com", "hash", "student", "active");
        student.setId(20);
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 1)).thenReturn(false);
        assertMasked(service.getCourseLectures(1, student).get(0));
    }

    @Test
    void enrolledStudentGetsVideoData() {
        User student = new User("Sam", "sam@olp.com", "hash", "student", "active");
        student.setId(20);
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 1)).thenReturn(true);
        LectureResponse l = service.getCourseLectures(1, student).get(0);
        assertEquals("dQw4w9WgXcQ", l.getVideoId());
        assertNotNull(l.getVideoUrl());
        assertFalse(l.isLocked());
    }

    @Test
    void owningInstructorGetsVideoData() {
        LectureResponse l = service.getCourseLectures(1, instructor).get(0);
        assertEquals("dQw4w9WgXcQ", l.getVideoId());
        assertFalse(l.isLocked());
    }

    @Test
    void adminCanViewContent() {
        User admin = new User("Admin", "admin@olp.com", "hash", "admin", "active");
        admin.setId(99);
        LectureResponse l = service.getCourseLectures(1, admin).get(0);
        assertEquals("dQw4w9WgXcQ", l.getVideoId());
        assertNotNull(l.getVideoUrl());
        assertFalse(l.isLocked());
    }

    @Test
    void courseDetailMasksLecturesForAnonymous() {
        assertMasked(service.getCourseDetail(1, null).getLectures().get(0));
    }

    // ===== FR-I07: Draft visibility & access control =====

    @Test
    void draftCourse_HiddenFromPublicCatalog() {
        Course draftCourse = new Course(instructor, "Secret Draft", "WIP", CourseStatus.DRAFT);
        draftCourse.setId(2);
        Course pendingCourse = new Course(instructor, "Pending Review", "WIP", CourseStatus.PENDING_REVIEW);
        pendingCourse.setId(3);
        Course archivedCourse = new Course(instructor, "Archived", "Old", CourseStatus.ARCHIVED);
        archivedCourse.setId(4);

        when(courseRepository.findAll()).thenReturn(List.of(course, draftCourse, pendingCourse, archivedCourse));

        List<PublicCourseResponse> catalog = service.getPublishedCourses(null, null);

        assertEquals(1, catalog.size());
        assertEquals(1, catalog.get(0).getId());
    }

    @Test
    void draftCourse_StudentDeniedAccessToDetailAndLecturesAndEnroll() {
        course.setStatus(CourseStatus.DRAFT);
        User student = new User("Sam", "sam@olp.com", "hash", "student", "active");
        student.setId(20);

        assertThrows(AccessDeniedException.class, () -> service.getCourseDetail(1, student));
        assertThrows(AccessDeniedException.class, () -> service.getCourseLectures(1, student));
        assertThrows(AccessDeniedException.class, () -> service.enrollCourse(student, 1));
    }

    @Test
    void draftCourse_AnonymousDeniedAccess() {
        course.setStatus(CourseStatus.DRAFT);

        assertThrows(AccessDeniedException.class, () -> service.getCourseDetail(1, null));
        assertThrows(AccessDeniedException.class, () -> service.getCourseLectures(1, null));
    }

    @Test
    void draftCourse_UnassociatedInstructorDeniedAccess() {
        course.setStatus(CourseStatus.DRAFT);
        User stranger = new User("Other", "other@olp.com", "hash", "instructor", "active");
        stranger.setId(99);
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(1, 99)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.getCourseDetail(1, stranger));
        assertThrows(AccessDeniedException.class, () -> service.getCourseLectures(1, stranger));
    }

    @Test
    void draftCourse_VisibleToAssociatedCoInstructorAndOwnerAndAdmin() {
        course.setStatus(CourseStatus.DRAFT);
        User coInstructor = new User("Co", "co@olp.com", "hash", "instructor", "active");
        coInstructor.setId(30);
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(1, 30)).thenReturn(true);

        User admin = new User("Admin", "admin@olp.com", "hash", "admin", "active");
        admin.setId(99);

        CourseDetailResponse ownerDetail = service.getCourseDetail(1, instructor);
        CourseDetailResponse coDetail = service.getCourseDetail(1, coInstructor);
        CourseDetailResponse adminDetail = service.getCourseDetail(1, admin);

        assertEquals("DRAFT", ownerDetail.getStatus());
        assertEquals("DRAFT", coDetail.getStatus());
        assertEquals("DRAFT", adminDetail.getStatus());
        assertFalse(service.getCourseLectures(1, coInstructor).get(0).isLocked());
    }

    // ===== FR-I59: Multiple Instructors on Public / Course Detail Endpoints =====

    @Test
    void publishedCourse_InstructorsVisibleToPublicAndDetailIncludesTeam() {
        User coInstructor = new User("Dr. Ada", "ada@olp.com", "hash", "instructor", "active");
        coInstructor.setId(30);

        when(courseInstructorRepository.findByCourseIdWithInstructor(1)).thenReturn(List.of(
                new CourseInstructor(course, coInstructor, CourseInstructorRole.CO_INSTRUCTOR),
                new CourseInstructor(course, instructor, CourseInstructorRole.OWNER)
        ));
        when(instructorProfileRepository.findByInstructorIdIn(anyList())).thenReturn(List.of(
                new InstructorProfile(instructor, "Ph.D. CS", "Java", "Course creator"),
                new InstructorProfile(coInstructor, "M.Sc. SE", "Testing", "Co-instructor")
        ));

        List<CourseInstructorResponse> publicList = service.getCourseInstructors(1, null);
        CourseDetailResponse detail = service.getCourseDetail(1, null);

        assertEquals(2, publicList.size());
        assertEquals(CourseInstructorRole.OWNER, publicList.get(0).getRole());
        assertEquals(10, publicList.get(0).getInstructorId());
        assertEquals("Ph.D. CS", publicList.get(0).getQualifications());

        assertEquals(CourseInstructorRole.CO_INSTRUCTOR, publicList.get(1).getRole());
        assertEquals(30, publicList.get(1).getInstructorId());
        assertEquals("Testing", publicList.get(1).getExpertiseAreas());

        assertEquals(2, detail.getInstructors().size());
    }

    @Test
    void draftCourse_InstructorsDeniedToStudentAndAnonymous() {
        course.setStatus(CourseStatus.DRAFT);
        User student = new User("Sam", "sam@olp.com", "hash", "student", "active");
        student.setId(20);

        assertThrows(AccessDeniedException.class, () -> service.getCourseInstructors(1, null));
        assertThrows(AccessDeniedException.class, () -> service.getCourseInstructors(1, student));
    }
}
