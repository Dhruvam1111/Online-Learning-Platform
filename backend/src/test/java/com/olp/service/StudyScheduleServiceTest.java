package com.olp.service;

import com.olp.dto.StudyScheduleRequest;
import com.olp.dto.StudyScheduleResponse;
import com.olp.entity.Course;
import com.olp.entity.StudySchedule;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseRepository;
import com.olp.repository.StudyScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyScheduleServiceTest {

    @Mock
    private StudyScheduleRepository scheduleRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private StudyScheduleService scheduleService;

    private User student;
    private Course course;
    private StudySchedule item;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(10);

        course = new Course();
        course.setId(20);
        course.setTitle("Algorithms");

        item = new StudySchedule(student, course, "Revise Sorting Algorithms", "Bubble, Merge, Quick sort",
                "LECTURE", 101, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2), "HIGH", "UTC");
        item.setId(1);
    }

    @Test
    void testCreateScheduleItem_Success() {
        when(courseRepository.findById(20)).thenReturn(Optional.of(course));
        when(scheduleRepository.save(any(StudySchedule.class))).thenAnswer(i -> {
            StudySchedule s = i.getArgument(0);
            s.setId(2);
            return s;
        });

        StudyScheduleRequest request = new StudyScheduleRequest();
        request.setCourseId(20);
        request.setTitle("Study Graph BFS");
        request.setTaskDescription("Traversals");
        request.setScheduledStart(LocalDateTime.now().plusDays(2));
        request.setScheduledEnd(LocalDateTime.now().plusDays(2).plusHours(1));
        request.setItemType("LECTURE");
        request.setReferenceId(102);
        request.setPriority("MEDIUM");

        StudyScheduleResponse response = scheduleService.createCustomItem(student, request);

        assertNotNull(response);
        assertEquals("Study Graph BFS", response.getTitle());
        verify(scheduleRepository).save(any(StudySchedule.class));
    }

    @Test
    void testToggleComplete_Success() {
        when(scheduleRepository.findById(1)).thenReturn(Optional.of(item));
        when(scheduleRepository.save(any(StudySchedule.class))).thenAnswer(i -> i.getArgument(0));

        StudyScheduleResponse response = scheduleService.markItemCompleted(student, 1, true);

        assertNotNull(response);
        assertTrue(response.getIsCompleted());
        assertNotNull(response.getCompletedAt());
    }

    @Test
    void testToggleComplete_OtherStudent_ThrowsAccessDenied() {
        User other = new User();
        other.setId(999);

        when(scheduleRepository.findById(1)).thenReturn(Optional.of(item));

        assertThrows(AccessDeniedException.class, () ->
                scheduleService.markItemCompleted(other, 1, true)
        );
    }

    @Test
    void testDeleteScheduleItem_Success() {
        when(scheduleRepository.findById(1)).thenReturn(Optional.of(item));

        scheduleService.deleteItem(student, 1);

        verify(scheduleRepository).delete(item);
    }
}
