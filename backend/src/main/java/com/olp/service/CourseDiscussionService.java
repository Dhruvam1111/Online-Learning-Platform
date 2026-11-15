package com.olp.service;

import com.olp.dto.DiscussionQuestionRequest;
import com.olp.dto.DiscussionReplyRequest;
import com.olp.dto.DiscussionReplyResponse;
import com.olp.dto.DiscussionThreadResponse;
import com.olp.dto.NotificationPayload;
import com.olp.entity.Course;
import com.olp.entity.CourseDiscussion;
import com.olp.entity.CourseDiscussionReply;
import com.olp.entity.Lecture;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseDiscussionReplyRepository;
import com.olp.repository.CourseDiscussionRepository;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.EnrollmentRepository;
import com.olp.repository.LectureRepository;
import com.olp.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Step 29 / FR-I40 & FR-S63/S64: Course Q&A Discussions Service.
 * Supports student question posting, instructor/student reply threads, unanswered filtering,
 * pinning, marking answered, and notifying students when an instructor replies.
 */
@Service
public class CourseDiscussionService {

    public static final String NOTIFICATION_TYPE_DISCUSSION_REPLY = "DISCUSSION_REPLY";
    public static final String NOTIFICATION_TYPE_DISCUSSION_QUESTION = "DISCUSSION_QUESTION";

    private final CourseDiscussionRepository discussionRepository;
    private final CourseDiscussionReplyRepository replyRepository;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public CourseDiscussionService(
            CourseDiscussionRepository discussionRepository,
            CourseDiscussionReplyRepository replyRepository,
            CourseRepository courseRepository,
            CourseInstructorRepository courseInstructorRepository,
            EnrollmentRepository enrollmentRepository,
            LectureRepository lectureRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.discussionRepository = discussionRepository;
        this.replyRepository = replyRepository;
        this.courseRepository = courseRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.lectureRepository = lectureRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * FR-I40: List student questions for a course (all or unanswered only) for the instructor.
     */
    @Transactional(readOnly = true)
    public List<DiscussionThreadResponse> getInstructorCourseDiscussions(
            Integer instructorId,
            Integer courseId,
            boolean unansweredOnly) {
        verifyInstructorCourseAccess(instructorId, courseId);
        List<CourseDiscussion> discussions = unansweredOnly
                ? discussionRepository.findUnansweredByCourseIdOrdered(courseId)
                : discussionRepository.findByCourseIdOrdered(courseId);
        return attachReplies(discussions);
    }

    /**
     * FR-S63/S64: List course discussions for students/public viewers (optionally filtered by unanswered or lectureId).
     */
    @Transactional(readOnly = true)
    public List<DiscussionThreadResponse> getCourseDiscussions(
            Integer courseId,
            boolean unansweredOnly,
            Integer lectureId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Course not found with ID: " + courseId);
        }
        List<CourseDiscussion> discussions = unansweredOnly
                ? discussionRepository.findUnansweredByCourseIdOrdered(courseId)
                : discussionRepository.findByCourseIdOrdered(courseId);

        if (lectureId != null) {
            discussions = discussions.stream()
                    .filter(d -> d.getLecture() != null && lectureId.equals(d.getLecture().getId()))
                    .collect(Collectors.toList());
        }
        return attachReplies(discussions);
    }

    /**
     * FR-S63: Student (or instructor) posts a new question in the course Q&A forum.
     */
    @Transactional
    public DiscussionThreadResponse createQuestion(
            Integer userId,
            Integer courseId,
            DiscussionQuestionRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + courseId));
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        boolean isInstructor = isCourseInstructorOrAdmin(course, author);
        boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(userId, courseId);
        if (!isInstructor && !isEnrolled) {
            throw new AccessDeniedException("Only enrolled students or course instructors can post questions");
        }

        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Question title is required");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("Question content is required");
        }

        Lecture lecture = null;
        if (request.getLectureId() != null) {
            lecture = lectureRepository.findById(request.getLectureId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lecture not found with ID: " + request.getLectureId()));
        }

        CourseDiscussion discussion = new CourseDiscussion(
                course,
                lecture,
                author,
                request.getTitle().trim(),
                request.getContent().trim()
        );
        CourseDiscussion saved = discussionRepository.save(discussion);

        if (course.getInstructor() != null && !course.getInstructor().getId().equals(author.getId())) {
            NotificationPayload payload = new NotificationPayload(
                    "New Q&A Question in " + course.getTitle() + ": " + saved.getTitle(),
                    author.getName() + " asked: " + saved.getContent(),
                    "/courses/" + courseId
            )
                    .withMetadata("courseId", courseId)
                    .withMetadata("discussionId", saved.getId())
                    .withMetadata("studentName", author.getName());
            notificationService.notify(course.getInstructor(), NOTIFICATION_TYPE_DISCUSSION_QUESTION, payload);
        }

        return DiscussionThreadResponse.fromEntity(saved, Collections.emptyList());
    }

    /**
     * FR-I40: Instructor replies to a student question, optionally marks thread answered, and notifies the student.
     */
    @Transactional
    public DiscussionThreadResponse replyAsInstructor(
            Integer instructorId,
            Integer courseId,
            Integer discussionId,
            DiscussionReplyRequest request) {
        Course course = verifyInstructorCourseAccess(instructorId, courseId);
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new ResourceNotFoundException("Instructor not found with ID: " + instructorId));
        CourseDiscussion discussion = discussionRepository.findByIdAndCourseId(discussionId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion question not found with ID: " + discussionId));

        if (request == null || request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("Reply content is required");
        }

        boolean official = request.getOfficialAnswer() == null || request.getOfficialAnswer();
        CourseDiscussionReply reply = new CourseDiscussionReply(
                discussion,
                instructor,
                "instructor",
                request.getContent().trim(),
                official
        );
        replyRepository.save(reply);

        boolean shouldMarkAnswered = request.getMarkAsAnswered() == null || request.getMarkAsAnswered();
        if (shouldMarkAnswered && !discussion.isAnswered()) {
            discussion.setAnswered(true);
            discussionRepository.save(discussion);
        }

        User questionAuthor = discussion.getAuthor();
        if (questionAuthor != null && questionAuthor.getId() != null && !questionAuthor.getId().equals(instructorId)) {
            NotificationPayload payload = new NotificationPayload(
                    "Instructor Replied to Your Question in " + course.getTitle(),
                    instructor.getName() + " replied to \"" + discussion.getTitle() + "\": " + reply.getContent(),
                    "/courses/" + courseId
            )
                    .withMetadata("courseId", courseId)
                    .withMetadata("courseTitle", course.getTitle())
                    .withMetadata("discussionId", discussion.getId())
                    .withMetadata("instructorName", instructor.getName());
            notificationService.notify(questionAuthor, NOTIFICATION_TYPE_DISCUSSION_REPLY, payload);
        }

        List<DiscussionReplyResponse> replies = replyRepository.findByDiscussionIdOrdered(discussionId)
                .stream()
                .map(DiscussionReplyResponse::fromEntity)
                .collect(Collectors.toList());
        return DiscussionThreadResponse.fromEntity(discussion, replies);
    }

    /**
     * FR-S64: Student or instructor posts a reply in an existing thread.
     */
    @Transactional
    public DiscussionThreadResponse addReply(
            Integer userId,
            Integer courseId,
            Integer discussionId,
            DiscussionReplyRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + courseId));
        User replier = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        CourseDiscussion discussion = discussionRepository.findByIdAndCourseId(discussionId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion question not found with ID: " + discussionId));

        boolean isInstructor = isCourseInstructorOrAdmin(course, replier);
        if (isInstructor) {
            return replyAsInstructor(userId, courseId, discussionId, request);
        }

        boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(userId, courseId);
        if (!isEnrolled) {
            throw new AccessDeniedException("Only enrolled students or course instructors can reply to discussions");
        }

        if (request == null || request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("Reply content is required");
        }

        CourseDiscussionReply reply = new CourseDiscussionReply(
                discussion,
                replier,
                "student",
                request.getContent().trim(),
                false
        );
        replyRepository.save(reply);

        List<DiscussionReplyResponse> replies = replyRepository.findByDiscussionIdOrdered(discussionId)
                .stream()
                .map(DiscussionReplyResponse::fromEntity)
                .collect(Collectors.toList());
        return DiscussionThreadResponse.fromEntity(discussion, replies);
    }

    /**
     * FR-I40: Mark a discussion question as answered or unanswered.
     */
    @Transactional
    public DiscussionThreadResponse setAnsweredStatus(
            Integer instructorId,
            Integer courseId,
            Integer discussionId,
            Boolean answered) {
        verifyInstructorCourseAccess(instructorId, courseId);
        CourseDiscussion discussion = discussionRepository.findByIdAndCourseId(discussionId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion question not found with ID: " + discussionId));

        boolean nextAnswered = answered != null ? answered : !discussion.isAnswered();
        discussion.setAnswered(nextAnswered);
        CourseDiscussion saved = discussionRepository.save(discussion);

        List<DiscussionReplyResponse> replies = replyRepository.findByDiscussionIdOrdered(discussionId)
                .stream()
                .map(DiscussionReplyResponse::fromEntity)
                .collect(Collectors.toList());
        return DiscussionThreadResponse.fromEntity(saved, replies);
    }

    /**
     * FR-I40: Pin or unpin a discussion question at the top of the course Q&A forum.
     */
    @Transactional
    public DiscussionThreadResponse setPinnedStatus(
            Integer instructorId,
            Integer courseId,
            Integer discussionId,
            Boolean pinned) {
        verifyInstructorCourseAccess(instructorId, courseId);
        CourseDiscussion discussion = discussionRepository.findByIdAndCourseId(discussionId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion question not found with ID: " + discussionId));

        boolean nextPinned = pinned != null ? pinned : !discussion.isPinned();
        discussion.setPinned(nextPinned);
        CourseDiscussion saved = discussionRepository.save(discussion);

        List<DiscussionReplyResponse> replies = replyRepository.findByDiscussionIdOrdered(discussionId)
                .stream()
                .map(DiscussionReplyResponse::fromEntity)
                .collect(Collectors.toList());
        return DiscussionThreadResponse.fromEntity(saved, replies);
    }

    /**
     * FR-I40: Delete a discussion thread from the course Q&A forum.
     */
    @Transactional
    public void deleteDiscussion(Integer instructorId, Integer courseId, Integer discussionId) {
        verifyInstructorCourseAccess(instructorId, courseId);
        CourseDiscussion discussion = discussionRepository.findByIdAndCourseId(discussionId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion question not found with ID: " + discussionId));
        discussionRepository.delete(discussion);
    }

    private List<DiscussionThreadResponse> attachReplies(List<CourseDiscussion> discussions) {
        if (discussions == null || discussions.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> ids = discussions.stream().map(CourseDiscussion::getId).collect(Collectors.toList());
        List<CourseDiscussionReply> allReplies = replyRepository.findByDiscussionIdsOrdered(ids);

        Map<Integer, List<DiscussionReplyResponse>> repliesByDiscussion = new HashMap<>();
        for (CourseDiscussionReply reply : allReplies) {
            Integer discId = reply.getDiscussion().getId();
            repliesByDiscussion.computeIfAbsent(discId, k -> new ArrayList<>())
                    .add(DiscussionReplyResponse.fromEntity(reply));
        }

        return discussions.stream()
                .map(d -> DiscussionThreadResponse.fromEntity(
                        d,
                        repliesByDiscussion.getOrDefault(d.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    private Course verifyInstructorCourseAccess(Integer userId, Integer courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + courseId));

        boolean isOwner = course.getInstructor() != null && course.getInstructor().getId().equals(userId);
        boolean isCoInstructor = courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, userId);

        if (!isOwner && !isCoInstructor) {
            throw new AccessDeniedException("You do not have permission to manage Q&A discussions for this course");
        }
        return course;
    }

    private boolean isCourseInstructorOrAdmin(Course course, User user) {
        if (user == null || user.getId() == null) {
            return false;
        }
        if ("admin".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        boolean isOwner = course.getInstructor() != null && course.getInstructor().getId().equals(user.getId());
        boolean isCoInstructor = courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), user.getId());
        return isOwner || isCoInstructor;
    }
}

