package com.olp.service;

import com.olp.dto.CourseResponse;
import com.olp.dto.CreateCourseRequest;
import com.olp.dto.CreateLectureRequest;
import com.olp.dto.LectureResponse;
import com.olp.entity.Course;
import com.olp.entity.CourseStatus;
import com.olp.entity.Lecture;
import com.olp.entity.User;
import com.olp.repository.CourseRepository;
import com.olp.repository.LectureRepository;
import com.olp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstructorAuthorizationTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LectureRepository lectureRepository;

    @InjectMocks
    private InstructorCourseService instructorCourseService;

    @InjectMocks
    private InstructorLectureService instructorLectureService;

    private User pendingInstructor;
    private User activeInstructor;
    private Course ownedCourse;

    @BeforeEach
    void setUp() {
        pendingInstructor = new User("Dr. Pending", "pending@olp.com", "hash", "instructor", "pending");
        pendingInstructor.setId(10);

        activeInstructor = new User("Dr. Active", "active@olp.com", "hash", "instructor", "active");
        activeInstructor.setId(20);

        ownedCourse = new Course(activeInstructor, "Java Masterclass", "Description", CourseStatus.DRAFT);
        ownedCourse.setId(100);
    }

    @Test
    void testCreateCourse_PendingInstructor_ThrowsAccessDeniedException() {
        CreateCourseRequest request = new CreateCourseRequest("New Course", "Description");

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                instructorCourseService.createCourse(pendingInstructor, request)
        );

        assertTrue(ex.getMessage().contains("awaiting admin approval"));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void testCreateCourse_ActiveInstructor_Success() {
        CreateCourseRequest request = new CreateCourseRequest("New Course", "Description");
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course c = invocation.getArgument(0);
            c.setId(101);
            return c;
        });

        CourseResponse response = instructorCourseService.createCourse(activeInstructor, request);

        assertNotNull(response);
        assertEquals("New Course", response.getTitle());
        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    void testCreateLecture_PendingInstructor_ThrowsAccessDeniedException() {
        CreateLectureRequest request = new CreateLectureRequest(
                "Lecture 1",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                1
        );

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                instructorLectureService.createLecture(pendingInstructor, 100, request)
        );

        assertTrue(ex.getMessage().contains("awaiting admin approval"));
        verify(lectureRepository, never()).save(any());
    }

    @Test
    void testCreateLecture_ActiveInstructor_Success() {
        CreateLectureRequest request = new CreateLectureRequest(
                "Lecture 1",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                1
        );

        when(courseRepository.findById(100)).thenReturn(Optional.of(ownedCourse));
        when(lectureRepository.countByCourseId(100)).thenReturn(0);
        when(lectureRepository.save(any(Lecture.class))).thenAnswer(invocation -> {
            Lecture l = invocation.getArgument(0);
            l.setId(500);
            return l;
        });

        LectureResponse response = instructorLectureService.createLecture(activeInstructor, 100, request);

        assertNotNull(response);
        assertEquals("Lecture 1", response.getTitle());
        assertEquals("dQw4w9WgXcQ", response.getVideoId());
        verify(lectureRepository, times(1)).save(any(Lecture.class));
    }
}

