package com.olp.service;

import com.olp.dto.CourseDiscussionRequest;
import com.olp.dto.CourseDiscussionResponse;
import com.olp.dto.DiscussionReplyRequest;
import com.olp.dto.DiscussionReplyResponse;
import com.olp.entity.Course;
import com.olp.entity.CourseDiscussion;
import com.olp.entity.DiscussionReply;
import com.olp.entity.User;
import com.olp.repository.CourseDiscussionRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.DiscussionReplyRepository;
import com.olp.repository.EnrollmentRepository;
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
class CourseDiscussionServiceTest {

    @Mock
    private CourseDiscussionRepository discussionRepository;

    @Mock
    private DiscussionReplyRepository replyRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private CourseDiscussionService discussionService;

    private User student;
    private User instructor;
    private Course course;
    private CourseDiscussion discussion;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1);
        student.setName("Alice");
        student.setRole("student");

        instructor = new User();
        instructor.setId(2);
        instructor.setName("Dr. Bob");
        instructor.setRole("instructor");

        course = new Course();
        course.setId(10);
        course.setTitle("Data Structures");
        course.setInstructor(instructor);

        discussion = new CourseDiscussion(course, student, "Question about BST", "How does delete work?", "QUESTIONS", null);
        discussion.setId(100);
        discussion.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testCreateDiscussion_EnrolledStudent_Success() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudentIdAndCourseId(1, 10)).thenReturn(true);
        when(discussionRepository.save(any(CourseDiscussion.class))).thenAnswer(i -> {
            CourseDiscussion d = i.getArgument(0);
            d.setId(101);
            return d;
        });

        CourseDiscussionRequest req = new CourseDiscussionRequest("Graph DFS", "What is visited set?", "GENERAL", null);
        CourseDiscussionResponse res = discussionService.createDiscussion(student, 10, req);

        assertNotNull(res);
        assertEquals("Graph DFS", res.getTitle());
        verify(discussionRepository).save(any(CourseDiscussion.class));
    }

    @Test
    void testCreateDiscussion_NotEnrolled_ThrowsAccessDenied() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudentIdAndCourseId(1, 10)).thenReturn(false);

        CourseDiscussionRequest req = new CourseDiscussionRequest("Graph DFS", "What is visited set?", "GENERAL", null);

        assertThrows(AccessDeniedException.class, () ->
                discussionService.createDiscussion(student, 10, req)
        );
    }

    @Test
    void testAddReply_Success() {
        when(discussionRepository.findById(100)).thenReturn(Optional.of(discussion));
        when(replyRepository.save(any(DiscussionReply.class))).thenAnswer(i -> {
            DiscussionReply r = i.getArgument(0);
            r.setId(500);
            return r;
        });

        DiscussionReplyRequest req = new DiscussionReplyRequest("You need to find inorder predecessor or successor.");
        DiscussionReplyResponse res = discussionService.replyToDiscussion(instructor, 100, req);

        assertNotNull(res);
        assertEquals("You need to find inorder predecessor or successor.", res.getContent());
        assertTrue(res.getIsInstructorReply());
    }

    @Test
    void testUpvoteDiscussion_Success() {
        when(discussionRepository.findById(100)).thenReturn(Optional.of(discussion));
        when(discussionRepository.save(any(CourseDiscussion.class))).thenAnswer(i -> i.getArgument(0));
        when(replyRepository.countByDiscussionId(100)).thenReturn(2);

        CourseDiscussionResponse response = discussionService.upvoteDiscussion(student, 100);
        assertNotNull(response);
        assertEquals(1, response.getUpvotes());
        assertEquals(2, response.getReplyCount());
    }
}
