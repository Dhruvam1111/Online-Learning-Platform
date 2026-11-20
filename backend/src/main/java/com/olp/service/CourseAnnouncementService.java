package com.olp.service;

import com.olp.dto.CourseAnnouncementRequest;
import com.olp.dto.CourseAnnouncementResponse;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing course announcements and broadcasting updates to enrolled learners (FR-S62, FR-S65).
 */
@Service
public class CourseAnnouncementService {

    private final CourseAnnouncementRepository announcementRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseInstructorRepository courseInstructorRepository;

    @Autowired(required = false)
    private StudentNotificationService studentNotificationService;

    public CourseAnnouncementService(CourseAnnouncementRepository announcementRepository,
                                     CourseRepository courseRepository,
                                     EnrollmentRepository enrollmentRepository,
                                     CourseInstructorRepository courseInstructorRepository) {
        this.announcementRepository = announcementRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseInstructorRepository = courseInstructorRepository;
    }

    public void setStudentNotificationService(StudentNotificationService studentNotificationService) {
        this.studentNotificationService = studentNotificationService;
    }

    @Transactional(readOnly = true)
    public List<CourseAnnouncementResponse> getCourseAnnouncements(User user, Integer courseId) {
        if (user == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        // Students must be enrolled or course published
        if ("student".equalsIgnoreCase(user.getRole())) {
            boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseId(user.getId(), courseId);
            if (!enrolled && course.getStatus() != CourseStatus.PUBLISHED) {
                throw new AccessDeniedException("You must be enrolled in this course to view announcements");
            }
        }

        List<CourseAnnouncement> announcements = announcementRepository.findByCourseIdOrderByPinnedAndDate(courseId);
        return announcements.stream().map(CourseAnnouncementResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CourseAnnouncementResponse getAnnouncementById(User user, Integer announcementId) {
        if (user == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        CourseAnnouncement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + announcementId));

        if ("student".equalsIgnoreCase(user.getRole())) {
            boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseId(user.getId(), announcement.getCourse().getId());
            if (!enrolled && announcement.getCourse().getStatus() != CourseStatus.PUBLISHED) {
                throw new AccessDeniedException("You do not have access to this announcement");
            }
        }

        return CourseAnnouncementResponse.fromEntity(announcement);
    }

    @Transactional
    public CourseAnnouncementResponse createAnnouncement(User instructor, Integer courseId, CourseAnnouncementRequest request) {
        if (instructor == null) {
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

        boolean isAdmin = "admin".equalsIgnoreCase(instructor.getRole());
        boolean isAssociated = courseInstructorRepository != null &&
                courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, instructor.getId());

        if (!isAdmin && !isAssociated && !instructor.getId().equals(course.getInstructor().getId())) {
            throw new AccessDeniedException("You are not authorized to post announcements for this course");
        }

        CourseAnnouncement announcement = new CourseAnnouncement(
                course, instructor, request.getTitle().trim(), request.getContent().trim(), request.getIsPinned()
        );
        CourseAnnouncement saved = announcementRepository.save(announcement);

        // Notify enrolled students (FR-S65)
        if (studentNotificationService != null) {
            studentNotificationService.notifyEnrolledStudents(
                    course, "ANNOUNCEMENT", "New Announcement: " + saved.getTitle(),
                    saved.getContent(), saved.getId(), "/courses/" + courseId + "/announcements"
            );
        }

        return CourseAnnouncementResponse.fromEntity(saved);
    }
}
