package com.olp.service;

import com.olp.dto.LectureResponse;
import com.olp.entity.Course;
import com.olp.entity.Lecture;
import com.olp.entity.User;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private CourseCatalogService service;

    private Course course;
    private User instructor;

    @BeforeEach
    void setUp() {
        instructor = new User("Ivy", "ivy@olp.com", "hash", "instructor", "active");
        instructor.setId(10);
        course = new Course(instructor, "Java", "desc");
        course.setId(1);
        Lecture lecture = new Lecture(course, "Intro", "https://youtu.be/dQw4w9WgXcQ", "dQw4w9WgXcQ", 1);
        lecture.setId(100);
        when(courseRepository.findById(1)).thenReturn(Optional.of(course));
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(1)).thenReturn(List.of(lecture));
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
}
