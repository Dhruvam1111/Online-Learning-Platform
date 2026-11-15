package com.olp.service;

import com.olp.dto.ScheduleContentReleaseRequest;
import com.olp.dto.ScheduledReleaseResponse;
import com.olp.entity.*;
import com.olp.event.ContentReleasedEvent;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledContentReleaseServiceTest {

    @Mock
    private LectureMaterialRepository materialRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ScheduledContentReleaseService releaseService;

    private User instructor;
    private User student;
    private Course course;
    private CourseModule module;
    private CourseSection section;
    private Lesson lesson;
    private LectureMaterial material;

    @BeforeEach
    void setUp() {
        instructor = new User("Dr. Chen", "chen@olp.com", "hash", "instructor");
        instructor.setId(10);

        student = new User("Alice", "alice@olp.com", "hash", "student");
        student.setId(20);

        course = new Course();
        course.setId(100);
        course.setTitle("Distributed Systems");
        course.setInstructor(instructor);

        module = new CourseModule();
        module.setId(200);
        module.setCourse(course);
        module.setTitle("Module 1");

        section = new CourseSection();
        section.setId(300);
        section.setModule(module);
        section.setTitle("Section 1");

        lesson = new Lesson();
        lesson.setId(400);
        lesson.setSection(section);
        lesson.setTitle("Raft Consensus");
        lesson.setIsReleased(true);

        material = new LectureMaterial(400, null, null, 10, "Raft Whitepaper", "raft.pdf",
                "uploads/raft.pdf", "pdf", "application/pdf", 1024L, true);
        material.setId(500);
        material.setIsReleased(true);
    }

    @Test
    @DisplayName("Schedule material release in future stores UTC and sets isReleased false (DR-ED08)")
    void testScheduleMaterialReleaseFuture() {
        // Arrange
        String futureDateTime = "2028-12-01 10:00:00";
        String timeZone = "Asia/Kolkata";
        ScheduleContentReleaseRequest request = new ScheduleContentReleaseRequest(futureDateTime, timeZone);

        when(materialRepository.findById(500)).thenReturn(Optional.of(material));
        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        when(materialRepository.save(any(LectureMaterial.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ScheduledReleaseResponse response = releaseService.scheduleMaterialRelease(instructor, 500, request);

        // Assert
        assertNotNull(response);
        assertEquals(500, response.getId());
        assertEquals("MATERIAL", response.getContentType());
        assertFalse(response.getIsReleased());
        assertEquals("IST", response.getTimeZoneLabel());
        assertTrue(response.getReleaseAtDisplay().contains("IST"));
        assertTrue(response.getReleaseAtDisplay().contains("2028-12-01 10:00:00"));

        // Verify stored in UTC: 10:00 IST is 04:30 UTC
        assertEquals(LocalDateTime.of(2028, 12, 1, 4, 30, 0), material.getReleaseAt());
        assertFalse(material.getIsReleased());

        // Event should NOT be published immediately for future release
        verify(eventPublisher, never()).publishEvent(any(ContentReleasedEvent.class));
    }

    @Test
    @DisplayName("Schedule material release in past marks isReleased true and dispatches event")
    void testScheduleMaterialReleasePast() {
        // Arrange: 1 day in the past
        ZonedDateTime pastZdt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).minusDays(1);
        String pastStr = pastZdt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        ScheduleContentReleaseRequest request = new ScheduleContentReleaseRequest(pastStr, "Asia/Kolkata");

        when(materialRepository.findById(500)).thenReturn(Optional.of(material));
        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        when(materialRepository.save(any(LectureMaterial.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ScheduledReleaseResponse response = releaseService.scheduleMaterialRelease(instructor, 500, request);

        // Assert
        assertNotNull(response);
        assertTrue(response.getIsReleased());
        assertTrue(material.getIsReleased());

        // Verify ContentReleasedEvent was dispatched
        verify(eventPublisher, times(1)).publishEvent(any(ContentReleasedEvent.class));
    }

    @Test
    @DisplayName("Non-instructor cannot schedule content release")
    void testScheduleMaterialReleaseNonInstructor() {
        ScheduleContentReleaseRequest request = new ScheduleContentReleaseRequest("2028-01-01 10:00:00", "Asia/Kolkata");
        assertThrows(AccessDeniedException.class, () ->
                releaseService.scheduleMaterialRelease(student, 500, request));
    }

    @Test
    @DisplayName("Unauthorized instructor cannot schedule content release for another's course")
    void testScheduleMaterialReleaseUnauthorizedInstructor() {
        User otherInstructor = new User("Dr. Other", "other@olp.com", "hash", "instructor");
        otherInstructor.setId(999);

        when(materialRepository.findById(500)).thenReturn(Optional.of(material));
        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(100, 999)).thenReturn(false);

        ScheduleContentReleaseRequest request = new ScheduleContentReleaseRequest("2028-01-01 10:00:00", "Asia/Kolkata");
        assertThrows(AccessDeniedException.class, () ->
                releaseService.scheduleMaterialRelease(otherInstructor, 500, request));
    }

    @Test
    @DisplayName("Schedule lesson release in future updates lesson release schedule")
    void testScheduleLessonReleaseFuture() {
        String futureDateTime = "2028-06-15T09:00:00";
        ScheduleContentReleaseRequest request = new ScheduleContentReleaseRequest(futureDateTime, "UTC");

        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScheduledReleaseResponse response = releaseService.scheduleLessonRelease(instructor, 400, request);

        assertNotNull(response);
        assertEquals(400, response.getId());
        assertEquals("LESSON", response.getContentType());
        assertFalse(response.getIsReleased());
        assertEquals(LocalDateTime.of(2028, 6, 15, 9, 0, 0), lesson.getReleaseAt());
        assertFalse(lesson.getIsReleased());
        verify(eventPublisher, never()).publishEvent(any(ContentReleasedEvent.class));
    }

    @Test
    @DisplayName("Background job publishes due content items once and only once")
    void testPublishDueContentItemsOnceAndOnlyOnce() {
        // Arrange
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        LectureMaterial dueMaterial = new LectureMaterial(400, null, null, 1, "Due Material", "due.pdf",
                "uploads/due.pdf", "pdf", "application/pdf", 512L, true);
        dueMaterial.setId(601);
        dueMaterial.setIsReleased(false);
        dueMaterial.setReleaseAt(nowUtc.minusMinutes(5));

        Lesson dueLesson = new Lesson();
        dueLesson.setId(701);
        dueLesson.setSection(section);
        dueLesson.setTitle("Due Lesson");
        dueLesson.setIsReleased(false);
        dueLesson.setReleaseAt(nowUtc.minusMinutes(2));

        when(materialRepository.findDueUnreleasedMaterials(any(LocalDateTime.class)))
                .thenReturn(List.of(dueMaterial))
                .thenReturn(Collections.emptyList()); // Second run returns empty

        when(lessonRepository.findDueUnreleasedLessons(any(LocalDateTime.class)))
                .thenReturn(List.of(dueLesson))
                .thenReturn(Collections.emptyList()); // Second run returns empty

        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));

        // Act - First scan execution
        int firstScanCount = releaseService.publishDueContentItems();

        // Assert - First run published exactly 2 items
        assertEquals(2, firstScanCount);
        assertTrue(dueMaterial.getIsReleased());
        assertTrue(dueLesson.getIsReleased());
        verify(materialRepository, times(1)).save(dueMaterial);
        verify(lessonRepository, times(1)).save(dueLesson);

        ArgumentCaptor<ContentReleasedEvent> eventCaptor = ArgumentCaptor.forClass(ContentReleasedEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        List<ContentReleasedEvent> events = eventCaptor.getAllValues();
        assertEquals(2, events.size());
        assertEquals("MATERIAL", events.get(0).getContentType());
        assertEquals("LESSON", events.get(1).getContentType());

        // Act - Second scan execution (idempotency check)
        int secondScanCount = releaseService.publishDueContentItems();

        // Assert - Second run published 0 additional items
        assertEquals(0, secondScanCount);
        verify(eventPublisher, times(2)).publishEvent(any(ContentReleasedEvent.class));
    }

    @Test
    @DisplayName("Get material release schedule formats display string in instructor timezone")
    void testGetMaterialReleaseSchedule() {
        material.setReleaseAt(LocalDateTime.of(2028, 1, 1, 4, 30, 0)); // 04:30 UTC = 10:00 IST
        material.setTimeZone("Asia/Kolkata");
        material.setIsReleased(false);

        when(materialRepository.findById(500)).thenReturn(Optional.of(material));
        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));

        ScheduledReleaseResponse response = releaseService.getMaterialReleaseSchedule(instructor, 500);

        assertNotNull(response);
        assertEquals(500, response.getId());
        assertEquals("IST", response.getTimeZoneLabel());
        assertEquals("2028-01-01 10:00:00 IST", response.getReleaseAtDisplay());
    }

    @Test
    @DisplayName("Get lesson release schedule formats display string in UTC")
    void testGetLessonReleaseSchedule() {
        lesson.setReleaseAt(LocalDateTime.of(2028, 5, 20, 14, 0, 0));
        lesson.setTimeZone("UTC");
        lesson.setIsReleased(false);

        when(lessonRepository.findById(400)).thenReturn(Optional.of(lesson));

        ScheduledReleaseResponse response = releaseService.getLessonReleaseSchedule(instructor, 400);

        assertNotNull(response);
        assertEquals(400, response.getId());
        assertEquals("UTC", response.getTimeZoneLabel());
        assertEquals("2028-05-20 14:00:00 UTC", response.getReleaseAtDisplay());
    }
}

