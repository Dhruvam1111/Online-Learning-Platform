package com.olp.service;

import com.olp.dto.AnnouncementRequest;
import com.olp.dto.AnnouncementResponse;
import com.olp.dto.NotificationPayload;
import com.olp.entity.Course;
import com.olp.entity.CourseAnnouncement;
import com.olp.entity.User;
import com.olp.repository.CourseAnnouncementRepository;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.EnrollmentRepository;
import com.olp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstructorAnnouncementServiceTest {

    @Mock
    private CourseAnnouncementRepository announcementRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private InstructorAnnouncementService announcementService;

    private User instructor;
    private Course course;
    private User student1;
    private User student2;

    @BeforeEach
    void setUp() {
        instructor = new User();
        instructor.setId(10);
        instructor.setName("Prof. Ada Lovelace");
        instructor.setEmail("ada@focotech.edu");

        course = new Course();
        course.setId(101);
        course.setTitle("Distributed Systems");
        course.setInstructor(instructor);

        student1 = new User();
        student1.setId(201);
        student1.setName("Alice Student");
        student1.setEmail("alice@student.edu");

        student2 = new User();
        student2.setId(202);
        student2.setName("Bob Student");
        student2.setEmail("bob@student.edu");
    }

    @Test
    void createAnnouncement_broadcastsNotificationsAndSavesHistory() {
        when(courseRepository.findById(101)).thenReturn(Optional.of(course));
        when(userRepository.findById(10)).thenReturn(Optional.of(instructor));
        when(enrollmentRepository.findEnrolledStudentsByCourseId(101)).thenReturn(List.of(student1, student2));

        when(announcementRepository.save(any(CourseAnnouncement.class))).thenAnswer(inv -> {
            CourseAnnouncement saved = inv.getArgument(0);
            saved.setId(500);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        AnnouncementRequest request = new AnnouncementRequest(
                "Midterm Exam Schedule",
                "The midterm exam will open this Friday at 10:00 AM IST."
        );

        AnnouncementResponse response = announcementService.createAnnouncement(10, 101, request);

        assertNotNull(response);
        assertEquals(500, response.getId());
        assertEquals("Midterm Exam Schedule", response.getTitle());
        assertEquals(2, response.getNotifiedStudentCount());

        ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(notificationService).notifyUsers(
                eq(List.of(student1, student2)),
                eq(InstructorAnnouncementService.NOTIFICATION_TYPE_COURSE_ANNOUNCEMENT),
                payloadCaptor.capture()
        );
        assertTrue(payloadCaptor.getValue().getTitle().contains("Midterm Exam Schedule"));
    }

    @Test
    void createAnnouncement_throwsAccessDeniedForNonInstructor() {
        when(courseRepository.findById(101)).thenReturn(Optional.of(course));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(101, 99)).thenReturn(false);

        AnnouncementRequest request = new AnnouncementRequest("Title", "Body");

        assertThrows(AccessDeniedException.class,
                () -> announcementService.createAnnouncement(99, 101, request));
        verify(announcementRepository, never()).save(any());
    }
}

