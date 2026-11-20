package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service managing course discussions, student Q&A, and instructor replies (FR-S63, FR-S64).
 */
@Service
public class CourseDiscussionService {

    private final CourseDiscussionRepository discussionRepository;
    private final DiscussionReplyRepository replyRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LectureRepository lectureRepository;
    private final CourseInstructorRepository courseInstructorRepository;

    @Autowired(required = false)
    private StudentNotificationService studentNotificationService;

    public CourseDiscussionService(CourseDiscussionRepository discussionRepository,
                                   DiscussionReplyRepository replyRepository,
                                   CourseRepository courseRepository,
                                   EnrollmentRepository enrollmentRepository,
                                   LectureRepository lectureRepository,
                                   CourseInstructorRepository courseInstructorRepository) {
        this.discussionRepository = discussionRepository;
        this.replyRepository = replyRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.lectureRepository = lectureRepository;
        this.courseInstructorRepository = courseInstructorRepository;
    }

    public void setStudentNotificationService(StudentNotificationService studentNotificationService) {
        this.studentNotificationService = studentNotificationService;
    }

    @Transactional(readOnly = true)
    public List<CourseDiscussionResponse> getDiscussions(User user, Integer courseId, String category) {
        if (user == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        if ("student".equalsIgnoreCase(user.getRole())) {
            boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseId(user.getId(), courseId);
            if (!enrolled && course.getStatus() != CourseStatus.PUBLISHED) {
                throw new AccessDeniedException("You must be enrolled in this course to view discussions");
            }
        }

        List<CourseDiscussion> discussions;
        if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category)) {
            discussions = discussionRepository.findByCourseIdAndCategory(courseId, category.toUpperCase().trim());
        } else {
            discussions = discussionRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
        }

        return discussions.stream().map(d -> {
            int replyCount = replyRepository.countByDiscussionId(d.getId());
            return CourseDiscussionResponse.fromEntity(d, replyCount);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDiscussionDetails(User user, Integer discussionId) {
        if (user == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        CourseDiscussion discussion = discussionRepository.findById(discussionId)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion thread not found: " + discussionId));

        List<DiscussionReply> replies = replyRepository.findByDiscussionIdOrderByCreatedAtAsc(discussionId);
        List<DiscussionReplyResponse> replyDtos = replies.stream()
                .map(DiscussionReplyResponse::fromEntity)
                .collect(Collectors.toList());

        CourseDiscussionResponse headerDto = CourseDiscussionResponse.fromEntity(discussion, replies.size());

        Map<String, Object> result = new HashMap<>();
        result.put("discussion", headerDto);
        result.put("replies", replyDtos);
        return result;
    }

    @Transactional
    public CourseDiscussionResponse createDiscussion(User student, Integer courseId, CourseDiscussionRequest request) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (request == null || request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be blank");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        if ("student".equalsIgnoreCase(student.getRole())) {
            boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), courseId);
            if (!enrolled) {
                throw new AccessDeniedException("You must be enrolled in this course to start a discussion or ask a question");
            }
        }

        Lecture lecture = null;
        if (request.getLectureId() != null) {
            lecture = lectureRepository.findById(request.getLectureId()).orElse(null);
        }

        CourseDiscussion discussion = new CourseDiscussion(
                course, student, request.getTitle().trim(), request.getContent().trim(),
                request.getCategory(), lecture
        );
        CourseDiscussion saved = discussionRepository.save(discussion);
        return CourseDiscussionResponse.fromEntity(saved, 0);
    }

    @Transactional
    public DiscussionReplyResponse replyToDiscussion(User user, Integer discussionId, DiscussionReplyRequest request) {
        if (user == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (request == null || request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Reply content cannot be blank");
        }

        CourseDiscussion discussion = discussionRepository.findById(discussionId)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion thread not found: " + discussionId));

        Course course = discussion.getCourse();
        boolean isInstructor = "instructor".equalsIgnoreCase(user.getRole()) ||
                (courseInstructorRepository != null && courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), user.getId())) ||
                user.getId().equals(course.getInstructor().getId());

        if ("student".equalsIgnoreCase(user.getRole())) {
            boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseId(user.getId(), course.getId());
            if (!enrolled) {
                throw new AccessDeniedException("You must be enrolled in this course to reply in discussions");
            }
        }

        DiscussionReply reply = new DiscussionReply(
                discussion, user, request.getContent().trim(), isInstructor, false
        );
        DiscussionReply saved = replyRepository.save(reply);

        if (isInstructor) {
            discussion.setIsAnswered(true);
            discussionRepository.save(discussion);

            // Notify the student who asked the question (FR-S64, FR-S65)
            if (studentNotificationService != null && !user.getId().equals(discussion.getStudent().getId())) {
                studentNotificationService.createNotification(
                        discussion.getStudent(), course, "DISCUSSION_REPLY",
                        "Instructor replied to your question: " + discussion.getTitle(),
                        saved.getContent(), discussion.getId(), "/courses/" + course.getId() + "/discussions/" + discussion.getId()
                );
            }
        }

        return DiscussionReplyResponse.fromEntity(saved);
    }

    @Transactional
    public CourseDiscussionResponse upvoteDiscussion(User user, Integer discussionId) {
        if (user == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        CourseDiscussion discussion = discussionRepository.findById(discussionId)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion thread not found: " + discussionId));

        discussion.setUpvotes(discussion.getUpvotes() + 1);
        CourseDiscussion saved = discussionRepository.save(discussion);
        int replyCount = replyRepository.countByDiscussionId(discussionId);
        return CourseDiscussionResponse.fromEntity(saved, replyCount);
    }
}
