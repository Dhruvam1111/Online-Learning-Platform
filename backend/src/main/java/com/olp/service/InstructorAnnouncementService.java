package com.olp.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.olp.dto.AnnouncementRequest;
import com.olp.dto.AnnouncementResponse;
import com.olp.dto.NotificationPayload;
import com.olp.entity.Course;
import com.olp.entity.CourseAnnouncement;
import com.olp.entity.User;
import com.olp.repository.CourseAnnouncementRepository;
import com.olp.repository.CourseRepository;
import com.olp.repository.EnrollmentRepository;

@Service
public class InstructorAnnouncementService {

    private final CourseAnnouncementRepository announcementRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;

    public InstructorAnnouncementService(
            CourseAnnouncementRepository announcementRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            NotificationService notificationService) {
        this.announcementRepository = announcementRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public AnnouncementResponse createAnnouncement(Integer courseId, AnnouncementRequest request, User currentUser) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID is required");
        }
        if (request == null || request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Announcement title is required");
        }
        if (request.getBody() == null || request.getBody().trim().isEmpty()) {
            throw new IllegalArgumentException("Announcement body is required");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        validateInstructorOwnership(course, currentUser);

        CourseAnnouncement announcement = new CourseAnnouncement();
        announcement.setCourse(course);
        announcement.setInstructor(currentUser);
        announcement.setTitle(request.getTitle().trim());
        announcement.setBody(request.getBody().trim());

        CourseAnnouncement saved = announcementRepository.save(announcement);

        String notificationTitle = "New Announcement in " + course.getTitle() + ": " + saved.getTitle();
        String link = "/courses/" + course.getId() + "/announcements";
        NotificationPayload payload = new NotificationPayload(
                notificationTitle,
                saved.getBody(),
                link)
                .withMetadata("courseId", course.getId())
                .withMetadata("courseTitle", course.getTitle())
                .withMetadata("announcementId", saved.getId());

        List<User> enrolledStudents = enrollmentRepository.findEnrolledStudentsByCourseId(course.getId());
        notificationService.notifyUsers(enrolledStudents, "COURSE_ANNOUNCEMENT", payload);

        return AnnouncementResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getCourseAnnouncementsForInstructor(Integer courseId, User currentUser) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID is required");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        validateInstructorOwnership(course, currentUser);

        return announcementRepository.findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(AnnouncementResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getInstructorAnnouncements(User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Unauthorized");
        }
        return announcementRepository.findByInstructorIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(AnnouncementResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getCourseAnnouncementsForViewer(Integer courseId, User currentUser) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID is required");
        }
        if (currentUser == null) {
            throw new IllegalArgumentException("Unauthorized");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());
        boolean isOwner = course.getInstructor() != null
                && course.getInstructor().getId().equals(currentUser.getId());
        boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(currentUser.getId(), courseId);

        if (!isAdmin && !isOwner && !isEnrolled) {
            throw new SecurityException("Access denied: you must be enrolled in or own this course to view announcements");
        }

        return announcementRepository.findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(AnnouncementResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAnnouncement(Integer announcementId, User currentUser) {
        if (announcementId == null) {
            throw new IllegalArgumentException("Announcement ID is required");
        }
        CourseAnnouncement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new IllegalArgumentException("Announcement not found"));

        validateInstructorOwnership(announcement.getCourse(), currentUser);

        announcementRepository.delete(announcement);
    }

    private void validateInstructorOwnership(Course course, User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Unauthorized");
        }
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());
        boolean isOwner = course.getInstructor() != null
                && course.getInstructor().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new SecurityException("Access denied: only the course instructor can manage announcements");
        }
    }
}
