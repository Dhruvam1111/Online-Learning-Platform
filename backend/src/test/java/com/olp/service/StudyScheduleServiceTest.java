package com.olp.service;

import com.olp.dto.StudyScheduleGenerateRequest;
import com.olp.dto.StudyScheduleRequest;
import com.olp.dto.StudyScheduleResponse;
import com.olp.entity.Assignment;
import com.olp.entity.Course;
import com.olp.entity.Enrollment;
import com.olp.entity.Exam;
import com.olp.entity.Lecture;
import com.olp.entity.StudySchedule;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.AssignmentRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.EnrollmentRepository;
import com.olp.repository.ExamRepository;
import com.olp.repository.LectureProgressRepository;
import com.olp.repository.LectureRepository;
import com.olp.repository.StudyScheduleRepository;
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
class StudyScheduleServiceTest {

    @Mock
    private StudyScheduleRepository scheduleRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private LectureRepository lectureRepository;

    @Mock
    private LectureProgressRepository lectureProgressRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

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

    @Test
    void testGetAndCreateScheduleItem_ValidatesAuthBoundsMissingCourseAndWindow() {
        assertThrows(InvalidCredentialsException.class,
                () -> scheduleService.getStudentSchedule(null, null, null));

        LocalDateTime s = LocalDateTime.now();
        LocalDateTime e = s.plusDays(3);
        when(scheduleRepository.findByStudentIdAndWindow(10, s, e)).thenReturn(List.of(item));
        assertEquals(1, scheduleService.getStudentSchedule(student, s, e).size());

        when(scheduleRepository.findByStudentIdOrderByStart(10)).thenReturn(List.of(item));
        assertEquals(1, scheduleService.getStudentSchedule(student, null, null).size());

        StudyScheduleRequest badTitle = new StudyScheduleRequest();
        badTitle.setTitle("   ");
        assertThrows(IllegalArgumentException.class, () -> scheduleService.createCustomItem(student, badTitle));

        StudyScheduleRequest endBeforeStart = new StudyScheduleRequest();
        endBeforeStart.setTitle("Review");
        endBeforeStart.setScheduledStart(s.plusHours(3));
        endBeforeStart.setScheduledEnd(s);
        assertThrows(IllegalArgumentException.class, () -> scheduleService.createCustomItem(student, endBeforeStart));

        StudyScheduleRequest missingCourse = new StudyScheduleRequest();
        missingCourse.setTitle("Review");
        missingCourse.setCourseId(999);
        missingCourse.setScheduledStart(s);
        missingCourse.setScheduledEnd(e);
        when(courseRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> scheduleService.createCustomItem(student, missingCourse));

        // Unchecking completion clears completedAt
        item.setIsCompleted(true);
        item.setCompletedAt(LocalDateTime.now());
        when(scheduleRepository.findById(1)).thenReturn(Optional.of(item));
        when(scheduleRepository.save(any(StudySchedule.class))).thenAnswer(i -> i.getArgument(0));
        StudyScheduleResponse unchecked = scheduleService.markItemCompleted(student, 1, false);
        assertFalse(unchecked.getIsCompleted());
        assertNull(unchecked.getCompletedAt());
    }

    @Test
    void testGeneratePersonalizedSchedule_CreatesItemsAndSkipsCompletedAndDuplicates() {
        assertThrows(InvalidCredentialsException.class,
                () -> scheduleService.generatePersonalizedSchedule(null, new StudyScheduleGenerateRequest()));

        Enrollment enrollment = new Enrollment(student, course);
        when(enrollmentRepository.findByStudentId(10)).thenReturn(List.of(enrollment));

        Exam upcomingExam = new Exam();
        upcomingExam.setId(50);
        upcomingExam.setTitle("Final Exam");
        upcomingExam.setStartWindow(LocalDateTime.now().plusDays(5));
        when(examRepository.findByCourseId(20)).thenReturn(List.of(upcomingExam));
        when(scheduleRepository.findExistingSchedules(10, 20, "EXAM", 50)).thenReturn(List.of());

        Assignment upcomingAssignment = new Assignment();
        upcomingAssignment.setId(60);
        upcomingAssignment.setTitle("Project Part 1");
        upcomingAssignment.setDeadline(LocalDateTime.now().plusHours(12));
        when(assignmentRepository.findByCourseId(20)).thenReturn(List.of(upcomingAssignment));
        when(scheduleRepository.findExistingSchedules(10, 20, "ASSIGNMENT", 60)).thenReturn(List.of(item));

        Lecture incompleteLecture = new Lecture(course, "Dynamic Programming", "https://youtu.be/abc", 1);
        incompleteLecture.setId(70);
        Lecture completedLecture = new Lecture(course, "Intro", "https://youtu.be/xyz", 0);
        completedLecture.setId(71);
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(20))
                .thenReturn(List.of(completedLecture, incompleteLecture));
        when(lectureProgressRepository.existsByStudentIdAndLectureId(10, 71)).thenReturn(true);
        when(lectureProgressRepository.existsByStudentIdAndLectureId(10, 70)).thenReturn(false);
        when(scheduleRepository.findExistingSchedules(10, 20, "LECTURE", 70)).thenReturn(List.of());

        when(scheduleRepository.save(any(StudySchedule.class))).thenAnswer(i -> i.getArgument(0));

        List<StudyScheduleResponse> generated = scheduleService.generatePersonalizedSchedule(student, null);
        // Expected: 1 EXAM + 0 ASSIGNMENT (duplicate skipped) + 1 incomplete LECTURE = 2 items
        assertEquals(2, generated.size());
        assertEquals("EXAM", generated.get(0).getItemType());
        assertEquals("LECTURE", generated.get(1).getItemType());
        assertEquals("Asia/Kolkata", generated.get(0).getTimeZone());
    }
}
