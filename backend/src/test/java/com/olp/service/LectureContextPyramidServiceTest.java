package com.olp.service;

import com.olp.entity.*;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LectureContextPyramidServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LectureNotesDocumentRepository notesDocumentRepository;

    @Mock
    private CoursePrerequisiteRepository prerequisiteRepository;

    @InjectMocks
    private LectureContextPyramidService pyramidService;

    private Lesson lesson;
    private CourseSection section;
    private CourseModule module;
    private Course course;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(10);
        course.setTitle("Distributed Systems");
        course.setCategory("Computer Science");
        course.setDescription("Advanced consensus and replication.");

        module = new CourseModule();
        module.setId(20);
        module.setTitle("Consensus Protocols");
        module.setCourse(course);

        section = new CourseSection();
        section.setId(30);
        section.setTitle("Raft Leader Election");
        section.setModule(module);

        lesson = new Lesson();
        lesson.setId(40);
        lesson.setTitle("Election Safety & Heartbeats");
        lesson.setSection(section);
        lesson.setDurationMinutes(50);
        lesson.setContent("00:04:15 - Welcome to Raft leader election.\n00:15:20 - Remember quorum Q = floor(N/2)+1.");
    }

    @Test
    void testAssembleContextPyramid_Success() {
        when(lessonRepository.findById(40)).thenReturn(Optional.of(lesson));
        when(lessonRepository.findByCourseId(10)).thenReturn(List.of(lesson));
        when(prerequisiteRepository.findByCourseIdOrderByIdAsc(10)).thenReturn(List.of());

        LectureNotesDocument doc = new LectureNotesDocument();
        doc.setId(100);
        doc.setExtractedText("# Raft Notes\n- Randomized timeouts 150-300ms");
        when(notesDocumentRepository.findById(100)).thenReturn(Optional.of(doc));

        Map<String, Object> pyramid = pyramidService.assembleContextPyramid(
                40, 100, null, "Focus on split brain", 3, true
        );

        assertNotNull(pyramid);
        assertTrue(pyramid.containsKey("course"));
        assertTrue(pyramid.containsKey("trajectory"));
        assertTrue(pyramid.containsKey("transcript_text"));
        assertTrue(pyramid.containsKey("extracted_notes_text"));

        Map<String, Object> courseCtx = (Map<String, Object>) pyramid.get("course");
        assertEquals("Distributed Systems", courseCtx.get("title"));

        Map<String, Object> trajectory = (Map<String, Object>) pyramid.get("trajectory");
        assertEquals("Election Safety & Heartbeats", trajectory.get("lesson_title"));
        assertEquals(1, trajectory.get("lesson_index"));

        assertEquals("# Raft Notes\n- Randomized timeouts 150-300ms", pyramid.get("extracted_notes_text"));
        assertTrue(((String) pyramid.get("transcript_text")).contains("00:04:15"));
    }
}
