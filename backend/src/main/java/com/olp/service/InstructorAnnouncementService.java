package com.olp.service;

import com.olp.dto.AnnouncementRequest;
import com.olp.dto.AnnouncementResponse;
import com.olp.dto.NotificationPayload;
import com.olp.entity.Course;
import com.olp.entity.CourseAnnouncement;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseAnnouncementRepository;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.EnrollmentRepository;
import com.olp.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * FR-I39 — Course Announcements Service.
 * Allows instructors and co-instructors to broadcast announcements to all enrolled students
 * and view/manage the announcement history for a course.
 */
@Service
public class InstructorAnnouncementService {

    public static final String NOTIFICATION_TYPE_COURSE_ANNOUNCEMENT = "COURSE_ANNOUNCEMENT";

    private final CourseAnnouncementRepository announcementRepository;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public InstructorAnnouncementService(
            CourseAnnouncementRepository announcementRepository,
            CourseRepository courseRepository,
            CourseInstructorRepository courseInstructorRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.announcementRepository = announcementRepository;
        this.courseRepository = courseRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public AnnouncementResponse createAnnouncement(Integer instructorId, Integer courseId, AnnouncementRequest request) {
        Course course = verifyCourseAccess(instructorId, courseId);
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new ResourceNotFoundException("Instructor not found with ID: " + instructorId));

        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Announcement title is required");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("Announcement content is required");
        }

        CourseAnnouncement announcement = new CourseAnnouncement(
                course,
                instructor,
                request.getTitle().trim(),
                request.getContent().trim()
        );
        CourseAnnouncement saved = announcementRepository.save(announcement);

        List<User> enrolledStudents = enrollmentRepository.findEnrolledStudentsByCourseId(courseId);
        NotificationPayload payload = new NotificationPayload(
                "New Announcement in " + course.getTitle() + ": " + saved.getTitle(),
                saved.getContent(),
                "/courses/" + courseId
        )
                .withMetadata("courseId", courseId)
                .withMetadata("courseTitle", course.getTitle())
                .withMetadata("announcementId", saved.getId())
                .withMetadata("instructorName", instructor.getName());

        notificationService.notifyUsers(enrolledStudents, NOTIFICATION_TYPE_COURSE_ANNOUNCEMENT, payload);

        return AnnouncementResponse.fromEntity(saved, enrolledStudents.size());
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getCourseAnnouncementsForInstructor(Integer instructorId, Integer courseId) {
        verifyCourseAccess(instructorId, courseId);
        return announcementRepository.findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(AnnouncementResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getCourseAnnouncementsPublic(Integer courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Course not found with ID: " + courseId);
        }
        return announcementRepository.findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(AnnouncementResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public AnnouncementResponse updateAnnouncement(
            Integer instructorId,
            Integer courseId,
            Integer announcementId,
            AnnouncementRequest request) {
        verifyCourseAccess(instructorId, courseId);
        CourseAnnouncement announcement = announcementRepository.findByIdAndCourseId(announcementId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with ID: " + announcementId));

        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Announcement title is required");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("Announcement content is required");
        }

        announcement.setTitle(request.getTitle().trim());
        announcement.setContent(request.getContent().trim());
        CourseAnnouncement updated = announcementRepository.save(announcement);
        return AnnouncementResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteAnnouncement(Integer instructorId, Integer courseId, Integer announcementId) {
        verifyCourseAccess(instructorId, courseId);
        CourseAnnouncement announcement = announcementRepository.findByIdAndCourseId(announcementId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with ID: " + announcementId));
        announcementRepository.delete(announcement);
    }

    private Course verifyCourseAccess(Integer userId, Integer courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + courseId));

        boolean isOwner = course.getInstructor() != null && course.getInstructor().getId().equals(userId);
        boolean isCoInstructor = courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, userId);

        if (!isOwner && !isCoInstructor) {
            throw new AccessDeniedException("You do not have permission to manage announcements for this course");
        }
        return course;
    }
}
