package com.olp.service;

import com.olp.dto.CourseAnnouncementRequest;
import com.olp.dto.CourseAnnouncementResponse;
import com.olp.entity.Course;
import com.olp.entity.CourseAnnouncement;
import com.olp.entity.CourseStatus;
import com.olp.entity.User;
import com.olp.repository.CourseAnnouncementRepository;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseAnnouncementServiceTest {

    @Mock
    private CourseAnnouncementRepository announcementRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @InjectMocks
    private CourseAnnouncementService announcementService;

    private User student;
    private User instructor;
    private Course course;
    private CourseAnnouncement announcement;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(10);
        student.setRole("student");
        student.setName("Alice");

        instructor = new User();
        instructor.setId(20);
        instructor.setRole("instructor");
        instructor.setName("Dr. Bob");

        course = new Course();
        course.setId(100);
        course.setTitle("Java 101");
        course.setStatus(CourseStatus.DRAFT);
        course.setInstructor(instructor);

        announcement = new CourseAnnouncement(course, instructor, "Exam Next Week", "Please be prepared.", true);
        announcement.setId(1);
        announcement.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testGetAnnouncementsForStudent_Success() {
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudentIdAndCourseId(10, 100)).thenReturn(true);
        when(announcementRepository.findByCourseIdOrderByPinnedAndDate(100)).thenReturn(List.of(announcement));

        List<CourseAnnouncementResponse> list = announcementService.getCourseAnnouncements(student, 100);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Exam Next Week", list.get(0).getTitle());
        assertTrue(list.get(0).getIsPinned());
    }

    @Test
    void testGetAnnouncementsForStudent_NotEnrolled_ThrowsAccessDenied() {
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudentIdAndCourseId(10, 100)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                announcementService.getCourseAnnouncements(student, 100)
        );
    }

    @Test
    void testCreateAnnouncement_Instructor_Success() {
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(announcementRepository.save(any(CourseAnnouncement.class))).thenAnswer(i -> {
            CourseAnnouncement a = i.getArgument(0);
            a.setId(2);
            return a;
        });

        CourseAnnouncementRequest request = new CourseAnnouncementRequest("Lab 1 Released", "Check the portal", false);
        CourseAnnouncementResponse response = announcementService.createAnnouncement(instructor, 100, request);

        assertNotNull(response);
        assertEquals("Lab 1 Released", response.getTitle());
        verify(announcementRepository).save(any(CourseAnnouncement.class));
    }

    @Test
    void testCreateAnnouncement_StudentRole_ThrowsAccessDenied() {
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));

        CourseAnnouncementRequest request = new CourseAnnouncementRequest("Unauthorized", "Content", false);

        assertThrows(AccessDeniedException.class, () ->
                announcementService.createAnnouncement(student, 100, request)
        );
    }
}
