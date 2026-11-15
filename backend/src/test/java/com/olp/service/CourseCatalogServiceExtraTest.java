package com.olp.service;

import com.olp.dto.CourseDetailResponse;
import com.olp.dto.EnrollmentResponse;
import com.olp.dto.LectureResponse;
import com.olp.dto.PublicCourseResponse;
import com.olp.entity.Course;
import com.olp.entity.CourseStatus;
import com.olp.entity.Enrollment;
import com.olp.entity.Lecture;
import com.olp.entity.Review;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Catalog, enrollment and user-resolution behaviour not covered by CourseCatalogServiceTest. */
@ExtendWith(MockitoExtension.class)
class CourseCatalogServiceExtraTest {

    @Mock private CourseRepository courseRepository;
    @Mock private LectureRepository lectureRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private BookmarkRepository bookmarkRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private CourseCatalogService service;

    private User instructor;
    private User student;

    @BeforeEach
    void setUp() {
        instructor = new User("Ivy", "ivy@olp.com", "hash", "instructor", "active");
        instructor.setId(10);
        student = new User("Sam", "sam@olp.com", "hash", "student", "active");
        student.setId(20);
    }

    private Course course(Integer id, String title, String description, CourseStatus status) {
        Course c = new Course(instructor, title, description, status);
        c.setId(id);
        return c;
    }

    private Lecture lecture(Course course, Integer id, String title, int order) {
        Lecture l = new Lecture(course, title, "https://youtu.be/dQw4w9WgXcQ", "dQw4w9WgXcQ", order);
        l.setId(id);
        return l;
    }

    private Review review() {
        return new Review(30, 1, 5, "nice");
    }

    // ===== resolveUserOptional / resolveUser =====

    @Test
    void testResolveUserOptional_NullAuthentication_ReturnsNull() {
        assertNull(service.resolveUserOptional(null));
        verifyNoInteractions(userRepository);
    }

    @Test
    void testResolveUserOptional_UnauthenticatedToken_ReturnsNull() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(student, null);

        assertNull(service.resolveUserOptional(auth));
        verifyNoInteractions(userRepository);
    }

    @Test
    void testResolveUserOptional_AnonymousUserPrincipal_ReturnsNull() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());

        assertNull(service.resolveUserOptional(auth));
        verifyNoInteractions(userRepository);
    }

    @Test
    void testResolveUserOptional_UserPrincipal_ReturnsSameInstance() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(student, null, List.of());

        assertSame(student, service.resolveUserOptional(auth));
        verifyNoInteractions(userRepository);
    }

    @Test
    void testResolveUserOptional_EmailPrincipal_LooksUpUser() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("sam@olp.com", null, List.of());
        when(userRepository.findByEmail("sam@olp.com")).thenReturn(Optional.of(student));

        assertSame(student, service.resolveUserOptional(auth));
    }

    @Test
    void testResolveUserOptional_EmailNotFound_ReturnsNull() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("ghost@olp.com", null, List.of());
        when(userRepository.findByEmail("ghost@olp.com")).thenReturn(Optional.empty());

        assertNull(service.resolveUserOptional(auth));
    }

    @Test
    void testResolveUser_Anonymous_ThrowsInvalidCredentials() {
        InvalidCredentialsException ex =
                assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(null));

        assertTrue(ex.getMessage().contains("not authenticated"));
    }

    @Test
    void testResolveUser_Authenticated_ReturnsUser() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(student, null, List.of());

        assertSame(student, service.resolveUser(auth));
    }

    @Test
    void testResolveUser_EmailPrincipalNotFound_ThrowsInvalidCredentials() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("ghost@olp.com", null, List.of());
        when(userRepository.findByEmail("ghost@olp.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(auth));
    }

    // ===== getPublishedCourses =====

    @Test
    void testGetPublishedCourses_Anonymous_ReturnsOnlyPublishedWithComputedStats() {
        Course java = course(1, "Java Basics", "Learn Java", CourseStatus.PUBLISHED);
        Course python = course(2, "Python", null, CourseStatus.PUBLISHED);
        Course draft = course(3, "Draft course", "wip", CourseStatus.DRAFT);
        when(courseRepository.findAll()).thenReturn(List.of(java, python, draft));
        when(lectureRepository.countByCourseId(1)).thenReturn(12);
        when(reviewRepository.findAverageRatingByCourseId(1)).thenReturn(4.26);
        when(reviewRepository.findAverageRatingByCourseId(2)).thenReturn(null);
        when(reviewRepository.findByCourseIdOrderByCreatedAtDesc(1)).thenReturn(List.of(review(), review()));

        List<PublicCourseResponse> result = service.getPublishedCourses(null, null);

        assertEquals(2, result.size());
        PublicCourseResponse first = result.get(0);
        assertEquals(1, first.getId());
        assertEquals("Java Basics", first.getTitle());
        assertEquals("PUBLISHED", first.getStatus());
        assertEquals(10, first.getInstructorId());
        assertEquals("Ivy", first.getInstructorName());
        assertEquals(12, first.getTotalLectures());
        assertEquals(4.3, first.getAverageRating(), 0.0001);
        assertEquals(2, first.getTotalReviews());
        assertFalse(first.isEnrolled());
        assertFalse(first.isBookmarked());

        PublicCourseResponse second = result.get(1);
        assertEquals(2, second.getId());
        assertEquals(0, second.getTotalLectures());
        assertEquals(4.8, second.getAverageRating(), 0.0001);
        assertEquals(0, second.getTotalReviews());

        verifyNoInteractions(enrollmentRepository, bookmarkRepository);
    }

    @Test
    void testGetPublishedCourses_LoggedInUser_FlagsEnrolledAndBookmarkedPerCourse() {
        Course java = course(1, "Java Basics", "Learn Java", CourseStatus.PUBLISHED);
        Course python = course(2, "Python", "Learn Python", CourseStatus.PUBLISHED);
        when(courseRepository.findAll()).thenReturn(List.of(java, python));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 1)).thenReturn(true);
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 2)).thenReturn(false);
        when(bookmarkRepository.existsByStudentIdAndCourseId(20, 1)).thenReturn(false);
        when(bookmarkRepository.existsByStudentIdAndCourseId(20, 2)).thenReturn(true);

        List<PublicCourseResponse> result = service.getPublishedCourses(null, student);

        assertTrue(result.get(0).isEnrolled());
        assertFalse(result.get(0).isBookmarked());
        assertFalse(result.get(1).isEnrolled());
        assertTrue(result.get(1).isBookmarked());
    }

    @Test
    void testGetPublishedCourses_CourseWithoutInstructor_UsesFallbackName() {
        Course orphan = new Course(null, "Orphan", "no owner", CourseStatus.PUBLISHED);
        orphan.setId(4);
        when(courseRepository.findAll()).thenReturn(List.of(orphan));

        PublicCourseResponse response = service.getPublishedCourses(null, null).get(0);

        assertNull(response.getInstructorId());
        assertEquals("Instructor", response.getInstructorName());
    }

    @Test
    void testGetPublishedCourses_SearchMatchesTitleCaseInsensitivelyAndTrimsInput() {
        Course java = course(1, "Java Basics", "Learn programming", CourseStatus.PUBLISHED);
        Course python = course(2, "Python", "Learn scripting", CourseStatus.PUBLISHED);
        when(courseRepository.findAll()).thenReturn(List.of(java, python));

        List<PublicCourseResponse> result = service.getPublishedCourses("  JAVA ", null);

        assertEquals(1, result.size());
        assertEquals("Java Basics", result.get(0).getTitle());
    }

    @Test
    void testGetPublishedCourses_SearchMatchesDescription() {
        Course java = course(1, "Java Basics", "Learn programming", CourseStatus.PUBLISHED);
        Course sql = course(2, "SQL", "Databases", CourseStatus.PUBLISHED);
        when(courseRepository.findAll()).thenReturn(List.of(java, sql));

        List<PublicCourseResponse> result = service.getPublishedCourses("database", null);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getId());
    }

    @Test
    void testGetPublishedCourses_SearchIsNullSafeForMissingDescription() {
        Course noDescription = course(1, "Go Language", null, CourseStatus.PUBLISHED);
        when(courseRepository.findAll()).thenReturn(List.of(noDescription));

        assertTrue(service.getPublishedCourses("rust", null).isEmpty());
        assertEquals(1, service.getPublishedCourses("go", null).size());
    }

    @Test
    void testGetPublishedCourses_SearchNeverReturnsUnpublishedCourses() {
        Course draftMatch = course(1, "Java Draft", "Java", CourseStatus.DRAFT);
        Course pendingMatch = course(2, "Java Pending", "Java", CourseStatus.PENDING_REVIEW);
        Course rejectedMatch = course(3, "Java Rejected", "Java", CourseStatus.REJECTED);
        when(courseRepository.findAll()).thenReturn(List.of(draftMatch, pendingMatch, rejectedMatch));

        assertTrue(service.getPublishedCourses("java", null).isEmpty());
        verifyNoInteractions(lectureRepository, reviewRepository);
    }

    @Test
    void testGetPublishedCourses_NoMatch_ReturnsEmptyList() {
        when(courseRepository.findAll()).thenReturn(List.of(course(1, "Java", "d", CourseStatus.PUBLISHED)));

        assertTrue(service.getPublishedCourses("zzz-no-match", null).isEmpty());
    }

    @Test
    void testGetPublishedCourses_BlankSearchIsTreatedAsNoSearch() {
        Course java = course(1, "Java", "d", CourseStatus.PUBLISHED);
        Course python = course(2, "Python", "d", CourseStatus.PUBLISHED);
        when(courseRepository.findAll()).thenReturn(List.of(java, python));

        assertEquals(2, service.getPublishedCourses("   ", null).size());
    }

    @Test
    void testGetPublishedCourses_NoCourses_ReturnsEmptyList() {
        when(courseRepository.findAll()).thenReturn(List.of());

        assertTrue(service.getPublishedCourses(null, student).isEmpty());
    }

    // ===== getCourseDetail =====

    @Test
    void testGetCourseDetail_CourseNotFound_Throws() {
        when(courseRepository.findById(99)).thenReturn(Optional.empty());

        ResourceNotFoundException ex =
                assertThrows(ResourceNotFoundException.class, () -> service.getCourseDetail(99, student));

        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void testGetCourseDetail_EnrolledStudent_SeesUnmaskedLecturesAndFlags() {
        Course java = course(1, "Java Basics", "Learn Java", CourseStatus.PUBLISHED);
        when(courseRepository.findById(1)).thenReturn(Optional.of(java));
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(1)).thenReturn(List.of(lecture(java, 100, "Intro", 1)));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 1)).thenReturn(true);
        when(bookmarkRepository.existsByStudentIdAndCourseId(20, 1)).thenReturn(true);
        when(reviewRepository.findAverageRatingByCourseId(1)).thenReturn(4.0);
        when(reviewRepository.findByCourseIdOrderByCreatedAtDesc(1))
                .thenReturn(List.of(review(), review(), review()));

        CourseDetailResponse detail = service.getCourseDetail(1, student);

        assertEquals(1, detail.getId());
        assertEquals("Java Basics", detail.getTitle());
        assertEquals("PUBLISHED", detail.getStatus());
        assertEquals(10, detail.getInstructorId());
        assertEquals("Ivy", detail.getInstructorName());
        assertEquals(1, detail.getTotalLectures());
        assertEquals(4.0, detail.getAverageRating(), 0.0001);
        assertEquals(3, detail.getTotalReviews());
        assertTrue(detail.isEnrolled());
        assertTrue(detail.isBookmarked());
        LectureResponse lecture = detail.getLectures().get(0);
        assertEquals("dQw4w9WgXcQ", lecture.getVideoId());
        assertFalse(lecture.isLocked());
    }

    @Test
    void testGetCourseDetail_NoRatingsYet_UsesDefaultRating() {
        Course java = course(1, "Java Basics", "Learn Java", CourseStatus.PUBLISHED);
        when(courseRepository.findById(1)).thenReturn(Optional.of(java));
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(1)).thenReturn(List.of());
        when(reviewRepository.findAverageRatingByCourseId(1)).thenReturn(null);

        CourseDetailResponse detail = service.getCourseDetail(1, null);

        assertEquals(4.8, detail.getAverageRating(), 0.0001);
        assertEquals(0, detail.getTotalReviews());
        assertEquals(0, detail.getTotalLectures());
        assertFalse(detail.isEnrolled());
        assertFalse(detail.isBookmarked());
        verifyNoInteractions(enrollmentRepository, bookmarkRepository);
    }

    @Test
    void testGetCourseDetail_RatingIsRoundedToOneDecimal() {
        Course java = course(1, "Java Basics", "Learn Java", CourseStatus.PUBLISHED);
        when(courseRepository.findById(1)).thenReturn(Optional.of(java));
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(1)).thenReturn(List.of());
        when(reviewRepository.findAverageRatingByCourseId(1)).thenReturn(3.66);

        assertEquals(3.7, service.getCourseDetail(1, null).getAverageRating(), 0.0001);
    }

    @Test
    void testGetCourseDetail_CourseWithoutInstructor_UsesFallbackName() {
        Course orphan = new Course(null, "Orphan", "no owner", CourseStatus.PUBLISHED);
        orphan.setId(1);
        when(courseRepository.findById(1)).thenReturn(Optional.of(orphan));
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(1)).thenReturn(List.of());

        CourseDetailResponse detail = service.getCourseDetail(1, null);

        assertNull(detail.getInstructorId());
        assertEquals("Instructor", detail.getInstructorName());
    }

    // ===== enrollCourse =====

    @Test
    void testEnrollCourse_Instructor_ThrowsAccessDenied() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> service.enrollCourse(instructor, 1));

        assertTrue(ex.getMessage().contains("Only students"));
        verifyNoInteractions(courseRepository, enrollmentRepository);
    }

    @Test
    void testEnrollCourse_CourseNotFound_Throws() {
        when(courseRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.enrollCourse(student, 99));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void testEnrollCourse_AlreadyEnrolled_ReturnsMessageWithoutSaving() {
        Course java = course(1, "Java Basics", "d", CourseStatus.PUBLISHED);
        when(courseRepository.findById(1)).thenReturn(Optional.of(java));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 1)).thenReturn(true);

        EnrollmentResponse response = service.enrollCourse(student, 1);

        assertTrue(response.success());
        assertTrue(response.enrolled());
        assertEquals(1, response.courseId());
        assertTrue(response.message().contains("Already enrolled"));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void testEnrollCourse_Success_SavesEnrollmentForStudentAndCourse() {
        Course java = course(1, "Java Basics", "d", CourseStatus.PUBLISHED);
        when(courseRepository.findById(1)).thenReturn(Optional.of(java));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 1)).thenReturn(false);

        EnrollmentResponse response = service.enrollCourse(student, 1);

        ArgumentCaptor<Enrollment> saved = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository, times(1)).save(saved.capture());
        assertSame(student, saved.getValue().getStudent());
        assertSame(java, saved.getValue().getCourse());
        assertTrue(response.success());
        assertTrue(response.enrolled());
        assertEquals("Successfully enrolled in Java Basics", response.message());
    }

    @Test
    void testEnrollCourse_RoleCheckIsCaseInsensitive() {
        student.setRole("STUDENT");
        Course java = course(1, "Java Basics", "d", CourseStatus.PUBLISHED);
        when(courseRepository.findById(1)).thenReturn(Optional.of(java));

        assertTrue(service.enrollCourse(student, 1).success());
    }

    @Test
    void testEnrollCourse_RejectsUnpublishedCourse() {
        Course draft = course(1, "Unreleased", "d", CourseStatus.DRAFT);
        when(courseRepository.findById(1)).thenReturn(Optional.of(draft));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> service.enrollCourse(student, 1));
        assertTrue(ex.getMessage().contains("Cannot enroll in a course that is not published"));
        verify(enrollmentRepository, never()).save(any());
    }

    // ===== getCourseLectures =====

    @Test
    void testGetCourseLectures_CourseNotFound_Throws() {
        when(courseRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getCourseLectures(99, student));
        verifyNoInteractions(lectureRepository);
    }

    @Test
    void testGetCourseLectures_OtherInstructorNotEnrolled_GetsMaskedLectures() {
        Course java = course(1, "Java Basics", "d", CourseStatus.PUBLISHED);
        User otherInstructor = new User("Omar", "omar@olp.com", "hash", "instructor", "active");
        otherInstructor.setId(11);
        when(courseRepository.findById(1)).thenReturn(Optional.of(java));
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(1)).thenReturn(List.of(lecture(java, 100, "Intro", 1)));
        when(enrollmentRepository.existsByStudentIdAndCourseId(11, 1)).thenReturn(false);

        LectureResponse result = service.getCourseLectures(1, otherInstructor).get(0);

        assertTrue(result.isLocked());
        assertNull(result.getVideoId());
        assertNull(result.getVideoUrl());
    }

    @Test
    void testGetCourseLectures_CourseWithoutInstructor_FallsThroughToEnrollmentCheck() {
        Course orphan = new Course(null, "Orphan", "d", CourseStatus.PUBLISHED);
        orphan.setId(1);
        when(courseRepository.findById(1)).thenReturn(Optional.of(orphan));
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(1)).thenReturn(List.of(lecture(orphan, 100, "Intro", 1)));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 1)).thenReturn(false);

        assertTrue(service.getCourseLectures(1, student).get(0).isLocked());
    }

    @Test
    void testGetCourseLectures_EnrolledStudent_KeepsRepositoryOrderAndUnmasksAll() {
        Course java = course(1, "Java Basics", "d", CourseStatus.PUBLISHED);
        when(courseRepository.findById(1)).thenReturn(Optional.of(java));
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(1))
                .thenReturn(List.of(lecture(java, 100, "Intro", 1), lecture(java, 101, "Streams", 2)));
        when(enrollmentRepository.existsByStudentIdAndCourseId(20, 1)).thenReturn(true);

        List<LectureResponse> result = service.getCourseLectures(1, student);

        assertEquals(2, result.size());
        assertEquals("Intro", result.get(0).getTitle());
        assertEquals("Streams", result.get(1).getTitle());
        assertFalse(result.get(0).isLocked());
        assertFalse(result.get(1).isLocked());
    }

    @Test
    void testGetCourseLectures_NoLectures_ReturnsEmptyList() {
        Course java = course(1, "Java Basics", "d", CourseStatus.PUBLISHED);
        when(courseRepository.findById(1)).thenReturn(Optional.of(java));
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(1)).thenReturn(List.of());

        assertTrue(service.getCourseLectures(1, null).isEmpty());
    }
}
