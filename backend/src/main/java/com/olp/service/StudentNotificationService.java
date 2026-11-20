package com.olp.service;

import com.olp.dto.StudentNotificationResponse;
import com.olp.entity.Course;
import com.olp.entity.Enrollment;
import com.olp.entity.StudentNotification;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.EnrollmentRepository;
import com.olp.repository.StudentNotificationRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing student notifications for course announcements, exam schedules,
 * assignment deadlines, and grade releases (FR-S65).
 */
@Service
public class StudentNotificationService {

    private final StudentNotificationRepository notificationRepository;
    private final EnrollmentRepository enrollmentRepository;

    public StudentNotificationService(StudentNotificationRepository notificationRepository,
                                      EnrollmentRepository enrollmentRepository) {
        this.notificationRepository = notificationRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public List<StudentNotificationResponse> getStudentNotifications(User student, Boolean unreadOnly) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        List<StudentNotification> list;
        if (Boolean.TRUE.equals(unreadOnly)) {
            list = notificationRepository.findUnreadByStudentId(student.getId());
        } else {
            list = notificationRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());
        }
        return list.stream().map(StudentNotificationResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User student) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        return notificationRepository.countUnreadByStudentId(student.getId());
    }

    @Transactional
    public StudentNotificationResponse markAsRead(User student, Integer notificationId) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        StudentNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!student.getId().equals(notification.getStudent().getId())) {
            throw new AccessDeniedException("You do not have permission to view this notification");
        }

        notification.setIsRead(true);
        StudentNotification saved = notificationRepository.save(notification);
        return StudentNotificationResponse.fromEntity(saved);
    }

    @Transactional
    public int markAllAsRead(User student) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        return notificationRepository.markAllAsRead(student.getId());
    }

    @Transactional
    public StudentNotification createNotification(User student, Course course, String type,
                                                  String title, String message, Integer refId, String refUrl) {
        if (student == null) return null;
        StudentNotification notification = new StudentNotification(student, course, type, title, message, refId, refUrl);
        return notificationRepository.save(notification);
    }

    @Transactional
    public List<StudentNotification> notifyEnrolledStudents(Course course, String type,
                                                           String title, String message,
                                                           Integer refId, String refUrl) {
        if (course == null) return List.of();
        List<Enrollment> enrollments = enrollmentRepository.findByCourseId(course.getId());
        List<StudentNotification> notifications = new ArrayList<>();
        for (Enrollment e : enrollments) {
            if (e.getStudent() != null) {
                StudentNotification n = new StudentNotification(e.getStudent(), course, type, title, message, refId, refUrl);
                notifications.add(n);
            }
        }
        return notificationRepository.saveAll(notifications);
    }
}
