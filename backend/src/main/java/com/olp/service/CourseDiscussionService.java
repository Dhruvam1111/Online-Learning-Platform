package com.olp.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.olp.dto.DiscussionModerationLogResponse;
import com.olp.dto.DiscussionModerationRequest;
import com.olp.dto.DiscussionQuestionRequest;
import com.olp.dto.DiscussionReplyRequest;
import com.olp.dto.DiscussionReplyResponse;
import com.olp.dto.DiscussionThreadResponse;
import com.olp.dto.NotificationPayload;
import com.olp.entity.Course;
import com.olp.entity.CourseDiscussion;
import com.olp.entity.CourseDiscussionReply;
import com.olp.entity.DiscussionModerationLog;
import com.olp.entity.Lecture;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseDiscussionReplyRepository;
import com.olp.repository.CourseDiscussionRepository;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.DiscussionModerationLogRepository;
import com.olp.repository.EnrollmentRepository;
import com.olp.repository.LectureRepository;
import com.olp.repository.UserRepository;

@Service
public class CourseDiscussionService {

    private final CourseDiscussionRepository discussionRepository;
    private final CourseDiscussionReplyRepository replyRepository;
    private final DiscussionModerationLogRepository moderationLogRepository;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final LectureRepository lectureRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public CourseDiscussionService(
            CourseDiscussionRepository discussionRepository,
            CourseDiscussionReplyRepository replyRepository,
            DiscussionModerationLogRepository moderationLogRepository,
            CourseRepository courseRepository,
            CourseInstructorRepository courseInstructorRepository,
            LectureRepository lectureRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.discussionRepository = discussionRepository;
        this.replyRepository = replyRepository;
        this.moderationLogRepository = moderationLogRepository;
        this.courseRepository = courseRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.lectureRepository = lectureRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // =========================================================================
    // Step 29 / FR-I40 & FR-S63/S64: Questions, Replies, Pin & Mark Answered
    // =========================================================================

    @Transactional
    public DiscussionThreadResponse createQuestion(
            Integer courseId,
            DiscussionQuestionRequest request,
            User currentUser) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID is required");
        }
        if (request == null || request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Question title is required");
        }
        if (request.getBody() == null || request.getBody().trim().isEmpty()) {
            throw new IllegalArgumentException("Question body is required");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + courseId));

        validateCourseViewerOrInstructor(course, currentUser);

        CourseDiscussion discussion = new CourseDiscussion();
        discussion.setCourse(course);
        discussion.setAuthor(currentUser);
        discussion.setTitle(request.getTitle().trim());
        discussion.setBody(request.getBody().trim());

        if (request.getLectureId() != null) {
            Lecture lecture = lectureRepository.findById(request.getLectureId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lecture not found with ID: " + request.getLectureId()));
            discussion.setLecture(lecture);
        }

        CourseDiscussion saved = discussionRepository.save(discussion);

        User instructor = course.getInstructor();
        if (instructor != null && !instructor.getId().equals(currentUser.getId())) {
            NotificationPayload payload = new NotificationPayload(
                    "New Student Question in " + course.getTitle(),
                    currentUser.getName() + " asked: " + saved.getTitle(),
                    "/courses/" + course.getId() + "/discussions/" + saved.getId()
            )
                    .withMetadata("courseId", course.getId())
                    .withMetadata("discussionId", saved.getId());
            notificationService.notify(instructor, "DISCUSSION_QUESTION", payload);
        }

        return DiscussionThreadResponse.fromEntity(saved, Collections.emptyList());
    }

    @Transactional(readOnly = true)
    public List<DiscussionThreadResponse> getCourseDiscussionsForInstructor(
            Integer courseId,
            Boolean unansweredOnly,
            Integer lectureId,
            User currentUser) {
        Course course = validateInstructorCourseAccess(courseId, currentUser);

        boolean filterUnanswered = Boolean.TRUE.equals(unansweredOnly);
        List<CourseDiscussion> discussions;

        if (lectureId != null) {
            discussions = filterUnanswered
                    ? discussionRepository.findUnansweredByCourseIdAndLectureIdForInstructor(course.getId(), lectureId)
                    : discussionRepository.findByCourseIdAndLectureIdForInstructor(course.getId(), lectureId);
        } else {
            discussions = filterUnanswered
                    ? discussionRepository.findUnansweredByCourseIdForInstructor(course.getId())
                    : discussionRepository.findAllByCourseIdForInstructor(course.getId());
        }

        return attachRepliesForInstructor(discussions);
    }

    @Transactional(readOnly = true)
    public List<DiscussionThreadResponse> getCourseDiscussionsForViewer(
            Integer courseId,
            Boolean unansweredOnly,
            User currentUser) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID is required");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + courseId));

        validateCourseViewerOrInstructor(course, currentUser);

        boolean filterUnanswered = Boolean.TRUE.equals(unansweredOnly);
        List<CourseDiscussion> discussions = filterUnanswered
                ? discussionRepository.findVisibleUnansweredByCourseId(courseId)
                : discussionRepository.findVisibleByCourseId(courseId);

        return attachVisibleReplies(discussions);
    }

    @Transactional(readOnly = true)
    public DiscussionThreadResponse getDiscussionThread(Integer discussionId, User currentUser) {
        CourseDiscussion discussion = discussionRepository.findByIdWithDetails(discussionId)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion not found with ID: " + discussionId));

        validateCourseViewerOrInstructor(discussion.getCourse(), currentUser);

        boolean isInstructorOrAdmin = isCourseInstructorOrAdmin(discussion.getCourse(), currentUser);
        if (!isInstructorOrAdmin && (discussion.isHidden() || discussion.isDeleted())) {
            throw new ResourceNotFoundException("Discussion not found with ID: " + discussionId);
        }

        List<CourseDiscussionReply> replies = isInstructorOrAdmin
                ? replyRepository.findAllByDiscussionIdForInstructor(discussionId)
                : replyRepository.findVisibleByDiscussionId(discussionId);

        List<DiscussionReplyResponse> replyResponses = replies.stream()
                .map(DiscussionReplyResponse::fromEntity)
                .collect(Collectors.toList());

        return DiscussionThreadResponse.fromEntity(discussion, replyResponses);
    }

    @Transactional
    public DiscussionReplyResponse addReply(
            Integer discussionId,
            DiscussionReplyRequest request,
            User currentUser) {
        if (discussionId == null) {
            throw new IllegalArgumentException("Discussion ID is required");
        }
        if (request == null || request.getBody() == null || request.getBody().trim().isEmpty()) {
            throw new IllegalArgumentException("Reply body is required");
        }

        CourseDiscussion discussion = discussionRepository.findByIdWithDetails(discussionId)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion not found with ID: " + discussionId));

        Course course = discussion.getCourse();
        validateCourseViewerOrInstructor(course, currentUser);

        if (discussion.isDeleted()) {
            throw new IllegalArgumentException("Cannot reply to a deleted discussion");
        }

        boolean instructorReply = isCourseInstructorOrAdmin(course, currentUser);

        CourseDiscussionReply reply = new CourseDiscussionReply();
        reply.setDiscussion(discussion);
        reply.setAuthor(currentUser);
        reply.setBody(request.getBody().trim());
        reply.setInstructorReply(instructorReply);
        reply.setOfficialAnswer(instructorReply && request.isOfficialAnswer());

        CourseDiscussionReply savedReply = replyRepository.save(reply);

        if (instructorReply) {
            if (!discussion.isAnswered()) {
                discussion.setAnswered(true);
                discussionRepository.save(discussion);
            }
            User questionAuthor = discussion.getAuthor();
            if (questionAuthor != null && !questionAuthor.getId().equals(currentUser.getId())) {
                NotificationPayload payload = new NotificationPayload(
                        "Instructor Replied to Your Question in " + course.getTitle(),
                        currentUser.getName() + " replied to \"" + discussion.getTitle() + "\": " + savedReply.getBody(),
                        "/courses/" + course.getId() + "/discussions/" + discussion.getId()
                )
                        .withMetadata("courseId", course.getId())
                        .withMetadata("discussionId", discussion.getId())
                        .withMetadata("replyId", savedReply.getId());
                notificationService.notify(questionAuthor, "DISCUSSION_REPLY", payload);
            }
        }

        return DiscussionReplyResponse.fromEntity(savedReply);
    }

    @Transactional
    public DiscussionThreadResponse toggleAnswered(Integer discussionId, Boolean answered, User currentUser) {
        CourseDiscussion discussion = discussionRepository.findByIdWithDetails(discussionId)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion not found with ID: " + discussionId));

        validateInstructorCourseAccess(discussion.getCourse().getId(), currentUser);

        boolean nextValue = answered != null ? answered : !discussion.isAnswered();
        discussion.setAnswered(nextValue);
        CourseDiscussion saved = discussionRepository.save(discussion);

        List<DiscussionReplyResponse> replies = replyRepository.findAllByDiscussionIdForInstructor(discussionId)
                .stream()
                .map(DiscussionReplyResponse::fromEntity)
                .collect(Collectors.toList());
        return DiscussionThreadResponse.fromEntity(saved, replies);
    }

    @Transactional
    public DiscussionThreadResponse togglePinned(Integer discussionId, Boolean pinned, User currentUser) {
        CourseDiscussion discussion = discussionRepository.findByIdWithDetails(discussionId)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion not found with ID: " + discussionId));

        validateInstructorCourseAccess(discussion.getCourse().getId(), currentUser);

        boolean nextValue = pinned != null ? pinned : !discussion.isPinned();
        discussion.setPinned(nextValue);
        CourseDiscussion saved = discussionRepository.save(discussion);

        List<DiscussionReplyResponse> replies = replyRepository.findAllByDiscussionIdForInstructor(discussionId)
                .stream()
                .map(DiscussionReplyResponse::fromEntity)
                .collect(Collectors.toList());
        return DiscussionThreadResponse.fromEntity(saved, replies);
    }

    @Transactional
    public DiscussionReplyResponse toggleOfficialAnswer(Integer replyId, Boolean officialAnswer, User currentUser) {
        CourseDiscussionReply reply = replyRepository.findByIdWithDetails(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found with ID: " + replyId));

        CourseDiscussion discussion = reply.getDiscussion();
        validateInstructorCourseAccess(discussion.getCourse().getId(), currentUser);

        boolean nextValue = officialAnswer != null ? officialAnswer : !reply.isOfficialAnswer();
        reply.setOfficialAnswer(nextValue);
        CourseDiscussionReply saved = replyRepository.save(reply);

        if (nextValue && !discussion.isAnswered()) {
            discussion.setAnswered(true);
            discussionRepository.save(discussion);
        }

        return DiscussionReplyResponse.fromEntity(saved);
    }

    // =========================================================================
    // Step 30 / FR-I42 & FR-A11: Discussion Moderation, Logging & Escalation
    // =========================================================================

    @Transactional
    public DiscussionThreadResponse moderateDiscussion(
            Integer discussionId,
            DiscussionModerationRequest request,
            User currentUser) {
        if (discussionId == null) {
            throw new IllegalArgumentException("Discussion ID is required");
        }
        validateModerationRequest(request);

        CourseDiscussion discussion = discussionRepository.findByIdWithDetails(discussionId)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion not found with ID: " + discussionId));

        Course course = validateInstructorCourseAccess(discussion.getCourse().getId(), currentUser);

        String action = request.getAction().trim().toUpperCase();
        String reason = request.getReason().trim();
        String previousContent = discussion.getTitle() + "\n\n" + discussion.getBody();
        String newContent = previousContent;
        LocalDateTime now = LocalDateTime.now();

        switch (action) {
            case "HIDE":
                discussion.setHidden(true);
                discussion.setModerationStatus("HIDDEN");
                break;
            case "UNHIDE", "RESTORE":
                discussion.setHidden(false);
                discussion.setDeleted(false);
                discussion.setModerationStatus("ACTIVE");
                break;
            case "DELETE":
                discussion.setDeleted(true);
                discussion.setHidden(true);
                discussion.setModerationStatus("DELETED");
                break;
            case "EDIT":
                if (request.getUpdatedTitle() != null && !request.getUpdatedTitle().trim().isEmpty()) {
                    discussion.setTitle(request.getUpdatedTitle().trim());
                }
                if (request.getUpdatedBody() == null || request.getUpdatedBody().trim().isEmpty()) {
                    throw new IllegalArgumentException("Updated body is required when editing a discussion post");
                }
                discussion.setBody(request.getUpdatedBody().trim());
                discussion.setModerationStatus("EDITED");
                newContent = discussion.getTitle() + "\n\n" + discussion.getBody();
                break;
            case "ESCALATE":
                discussion.setEscalatedToAdmin(true);
                discussion.setEscalationReason(reason);
                discussion.setEscalatedAt(now);
                discussion.setModerationStatus("ESCALATED");
                break;
            default:
                throw new IllegalArgumentException("Unsupported moderation action: " + action
                        + ". Allowed actions: HIDE, UNHIDE, RESTORE, EDIT, DELETE, ESCALATE");
        }

        if (request.isEscalateToAdmin() && !discussion.isEscalatedToAdmin()) {
            discussion.setEscalatedToAdmin(true);
            discussion.setEscalationReason(reason);
            discussion.setEscalatedAt(now);
        }

        discussion.setModerationReason(reason);
        discussion.setModeratedBy(currentUser);
        discussion.setModeratedAt(now);

        CourseDiscussion saved = discussionRepository.save(discussion);

        boolean escalated = "ESCALATE".equals(action) || request.isEscalateToAdmin();

        DiscussionModerationLog log = new DiscussionModerationLog();
        log.setCourse(course);
        log.setDiscussion(saved);
        log.setTargetType("DISCUSSION");
        log.setTargetAuthor(saved.getAuthor());
        log.setModerator(currentUser);
        log.setAction(action);
        log.setReason(reason);
        log.setPreviousContent(previousContent);
        log.setNewContent(newContent);
        log.setEscalatedToAdmin(escalated);
        moderationLogRepository.save(log);

        notifyAuthorOfModeration(saved.getAuthor(), course, "discussion post", action, reason, escalated);
        if (escalated) {
            notifyAdminsOfEscalation(course, currentUser, saved.getAuthor(), "DISCUSSION", saved.getId(), reason);
        }

        List<DiscussionReplyResponse> replies = replyRepository.findAllByDiscussionIdForInstructor(discussionId)
                .stream()
                .map(DiscussionReplyResponse::fromEntity)
                .collect(Collectors.toList());
        return DiscussionThreadResponse.fromEntity(saved, replies);
    }

    @Transactional
    public DiscussionReplyResponse moderateReply(
            Integer replyId,
            DiscussionModerationRequest request,
            User currentUser) {
        if (replyId == null) {
            throw new IllegalArgumentException("Reply ID is required");
        }
        validateModerationRequest(request);

        CourseDiscussionReply reply = replyRepository.findByIdWithDetails(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found with ID: " + replyId));

        CourseDiscussion discussion = reply.getDiscussion();
        Course course = validateInstructorCourseAccess(discussion.getCourse().getId(), currentUser);

        String action = request.getAction().trim().toUpperCase();
        String reason = request.getReason().trim();
        String previousContent = reply.getBody();
        String newContent = previousContent;
        LocalDateTime now = LocalDateTime.now();

        switch (action) {
            case "HIDE":
                reply.setHidden(true);
                reply.setModerationStatus("HIDDEN");
                break;
            case "UNHIDE", "RESTORE":
                reply.setHidden(false);
                reply.setDeleted(false);
                reply.setModerationStatus("ACTIVE");
                break;
            case "DELETE":
                reply.setDeleted(true);
                reply.setHidden(true);
                reply.setModerationStatus("DELETED");
                break;
            case "EDIT":
                if (request.getUpdatedBody() == null || request.getUpdatedBody().trim().isEmpty()) {
                    throw new IllegalArgumentException("Updated body is required when editing a reply");
                }
                reply.setBody(request.getUpdatedBody().trim());
                reply.setModerationStatus("EDITED");
                newContent = reply.getBody();
                break;
            case "ESCALATE":
                reply.setEscalatedToAdmin(true);
                reply.setEscalationReason(reason);
                reply.setEscalatedAt(now);
                reply.setModerationStatus("ESCALATED");
                break;
            default:
                throw new IllegalArgumentException("Unsupported moderation action: " + action
                        + ". Allowed actions: HIDE, UNHIDE, RESTORE, EDIT, DELETE, ESCALATE");
        }

        if (request.isEscalateToAdmin() && !reply.isEscalatedToAdmin()) {
            reply.setEscalatedToAdmin(true);
            reply.setEscalationReason(reason);
            reply.setEscalatedAt(now);
        }

        reply.setModerationReason(reason);
        reply.setModeratedBy(currentUser);
        reply.setModeratedAt(now);

        CourseDiscussionReply saved = replyRepository.save(reply);

        boolean escalated = "ESCALATE".equals(action) || request.isEscalateToAdmin();

        DiscussionModerationLog log = new DiscussionModerationLog();
        log.setCourse(course);
        log.setDiscussion(discussion);
        log.setReply(saved);
        log.setTargetType("REPLY");
        log.setTargetAuthor(saved.getAuthor());
        log.setModerator(currentUser);
        log.setAction(action);
        log.setReason(reason);
        log.setPreviousContent(previousContent);
        log.setNewContent(newContent);
        log.setEscalatedToAdmin(escalated);
        moderationLogRepository.save(log);

        notifyAuthorOfModeration(saved.getAuthor(), course, "discussion reply", action, reason, escalated);
        if (escalated) {
            notifyAdminsOfEscalation(course, currentUser, saved.getAuthor(), "REPLY", saved.getId(), reason);
        }

        return DiscussionReplyResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<DiscussionModerationLogResponse> getCourseModerationLogs(Integer courseId, User currentUser) {
        validateInstructorCourseAccess(courseId, currentUser);
        return moderationLogRepository.findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(DiscussionModerationLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DiscussionModerationLogResponse> getAdminEscalatedLogs(User currentUser) {
        if (currentUser == null || !"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            throw new AccessDeniedException("Only administrators can view escalated discussion cases");
        }
        return moderationLogRepository.findEscalatedLogsOrderByCreatedAtDesc()
                .stream()
                .map(DiscussionModerationLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private void validateModerationRequest(DiscussionModerationRequest request) {
        if (request == null || request.getAction() == null || request.getAction().trim().isEmpty()) {
            throw new IllegalArgumentException("Moderation action is required");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Moderation reason is required");
        }
    }

    private void notifyAuthorOfModeration(
            User author,
            Course course,
            String postLabel,
            String action,
            String reason,
            boolean escalated) {
        if (author == null || author.getId() == null) {
            return;
        }
        String title = "Moderation Notice in " + course.getTitle();
        String message = "Your " + postLabel + " in \"" + course.getTitle() + "\" underwent moderation ("
                + action + "). Reason: " + reason
                + (escalated ? " This case has also been escalated to platform administrators." : "");
        NotificationPayload payload = new NotificationPayload(
                title,
                message,
                "/courses/" + course.getId() + "/discussions"
        )
                .withMetadata("courseId", course.getId())
                .withMetadata("action", action)
                .withMetadata("escalatedToAdmin", escalated);
        notificationService.notify(author, "DISCUSSION_MODERATION", payload);
    }

    private void notifyAdminsOfEscalation(
            Course course,
            User moderator,
            User targetAuthor,
            String targetType,
            Integer targetId,
            String reason) {
        List<User> admins = userRepository.findAllByRoleIgnoreCase("ADMIN");
        if (admins == null || admins.isEmpty()) {
            return;
        }
        String title = "Discussion Escalated to Admin (" + course.getTitle() + ")";
        String authorName = targetAuthor != null ? targetAuthor.getName() : "Unknown user";
        String message = "Instructor " + moderator.getName() + " escalated a " + targetType
                + " (#" + targetId + ") by " + authorName + " in \"" + course.getTitle()
                + "\". Reason: " + reason;
        NotificationPayload payload = new NotificationPayload(
                title,
                message,
                "/admin/moderation/discussions"
        )
                .withMetadata("courseId", course.getId())
                .withMetadata("targetType", targetType)
                .withMetadata("targetId", targetId);
        notificationService.notifyUsers(admins, "ADMIN_DISCUSSION_ESCALATION", payload);
    }

    private List<DiscussionThreadResponse> attachRepliesForInstructor(List<CourseDiscussion> discussions) {
        if (discussions.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> ids = discussions.stream().map(CourseDiscussion::getId).collect(Collectors.toList());
        Map<Integer, List<DiscussionReplyResponse>> repliesByDiscussion = replyRepository
                .findAllByDiscussionIdsForInstructor(ids)
                .stream()
                .map(DiscussionReplyResponse::fromEntity)
                .collect(Collectors.groupingBy(DiscussionReplyResponse::getDiscussionId));

        List<DiscussionThreadResponse> result = new ArrayList<>(discussions.size());
        for (CourseDiscussion d : discussions) {
            List<DiscussionReplyResponse> threadReplies = repliesByDiscussion.getOrDefault(d.getId(), Collections.emptyList());
            result.add(DiscussionThreadResponse.fromEntity(d, threadReplies));
        }
        return result;
    }

    private List<DiscussionThreadResponse> attachVisibleReplies(List<CourseDiscussion> discussions) {
        if (discussions.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> ids = discussions.stream().map(CourseDiscussion::getId).collect(Collectors.toList());
        Map<Integer, List<DiscussionReplyResponse>> repliesByDiscussion = replyRepository
                .findVisibleByDiscussionIds(ids)
                .stream()
                .map(DiscussionReplyResponse::fromEntity)
                .collect(Collectors.groupingBy(DiscussionReplyResponse::getDiscussionId));

        List<DiscussionThreadResponse> result = new ArrayList<>(discussions.size());
        for (CourseDiscussion d : discussions) {
            List<DiscussionReplyResponse> threadReplies = repliesByDiscussion.getOrDefault(d.getId(), Collections.emptyList());
            result.add(DiscussionThreadResponse.fromEntity(d, threadReplies));
        }
        return result;
    }

    private Course validateInstructorCourseAccess(Integer courseId, User currentUser) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID is required");
        }
        if (currentUser == null) {
            throw new AccessDeniedException("Unauthorized");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + courseId));

        if (!isCourseInstructorOrAdmin(course, currentUser)) {
            throw new AccessDeniedException("Access denied: you can only manage discussions in your own courses");
        }
        return course;
    }

    private void validateCourseViewerOrInstructor(Course course, User currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Unauthorized");
        }
        if (isCourseInstructorOrAdmin(course, currentUser)) {
            return;
        }
        boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseId(currentUser.getId(), course.getId());
        if (!enrolled) {
            throw new AccessDeniedException("Access denied: you must be enrolled in this course to participate in discussions");
        }
    }

    private boolean isCourseInstructorOrAdmin(Course course, User user) {
        if (user == null) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        boolean isPrimaryOwner = course.getInstructor() != null
                && course.getInstructor().getId().equals(user.getId());
        if (isPrimaryOwner) {
            return true;
        }
        return courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), user.getId());
    }
}
