package com.olp.service;

import com.olp.dto.ScheduleContentReleaseRequest;
import com.olp.dto.ScheduledReleaseResponse;
import com.olp.entity.*;
import com.olp.event.ContentReleasedEvent;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

/**
 * Service managing scheduled content release lifecycle and background publication jobs (FR-I18, Step 20).
 * Stores release date-time in UTC and surfaces instructor-localized display times with timezone labels (DR-ED08).
 * Dispatches ContentReleasedEvent upon release to feed learner notification streams (FR-I41).
 */
@Service
@Transactional
public class ScheduledContentReleaseService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledContentReleaseService.class);

    private final LectureMaterialRepository materialRepository;
    private final LessonRepository lessonRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public ScheduledContentReleaseService(
            LectureMaterialRepository materialRepository,
            LessonRepository lessonRepository,
            CourseInstructorRepository courseInstructorRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher) {
        this.materialRepository = materialRepository;
        this.lessonRepository = lessonRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Resolves the authenticated User from the security context authentication.
     */
    public User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AccessDeniedException("Full authentication is required");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    /**
     * Scheduled background job that scans for and publishes due content items once and only once (FR-I18).
     * Runs every 60 seconds.
     */
    @Scheduled(fixedRate = 60000)
    public int publishDueContentItems() {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        int publishedCount = 0;

        // 1. Publish due materials
        List<LectureMaterial> dueMaterials = materialRepository.findDueUnreleasedMaterials(nowUtc);
        for (LectureMaterial material : dueMaterials) {
            material.setIsReleased(true);
            materialRepository.save(material);

            Integer courseId = resolveCourseIdFromMaterial(material);
            eventPublisher.publishEvent(new ContentReleasedEvent(
                    this, material.getId(), "MATERIAL", material.getTitle(), courseId, material.getLessonId(), nowUtc));
            publishedCount++;
            logger.info("Published due material ID {} '{}' in course ID {}", material.getId(), material.getTitle(), courseId);
        }

        // 2. Publish due lessons
        List<Lesson> dueLessons = lessonRepository.findDueUnreleasedLessons(nowUtc);
        for (Lesson lesson : dueLessons) {
            lesson.setIsReleased(true);
            lessonRepository.save(lesson);

            Integer courseId = (lesson.getSection() != null && lesson.getSection().getModule() != null && lesson.getSection().getModule().getCourse() != null)
                    ? lesson.getSection().getModule().getCourse().getId()
                    : null;
            eventPublisher.publishEvent(new ContentReleasedEvent(
                    this, lesson.getId(), "LESSON", lesson.getTitle(), courseId, lesson.getId(), nowUtc));
            publishedCount++;
            logger.info("Published due lesson ID {} '{}' in course ID {}", lesson.getId(), lesson.getTitle(), courseId);
        }

        if (publishedCount > 0) {
            logger.info("Scheduled content release job successfully published {} items at UTC {}", publishedCount, nowUtc);
        }
        return publishedCount;
    }

    /**
     * Programmatic trigger for release scan (for unit testing and manual sync).
     */
    public int triggerContentReleaseScan() {
        return publishDueContentItems();
    }

    /**
     * Schedules release date-time for a supplementary learning material (FR-I18).
     */
    public ScheduledReleaseResponse scheduleMaterialRelease(
            User instructor, Integer materialId, ScheduleContentReleaseRequest request) {

        validateInstructorRole(instructor);
        if (materialId == null) {
            throw new IllegalArgumentException("Material ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Schedule request cannot be null");
        }

        LectureMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + materialId));

        if (material.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Cannot schedule release for deleted material with id: " + materialId);
        }

        Course course = resolveCourseFromMaterial(material);
        assertInstructorAccess(course, instructor);

        LocalDateTime releaseAtUtc = request.resolveUtcReleaseAt();
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        boolean isReleased = !releaseAtUtc.isAfter(nowUtc);

        material.setReleaseAt(releaseAtUtc);
        material.setIsReleased(isReleased);
        material.setTimeZone(request.getTimeZone());

        LectureMaterial saved = materialRepository.save(material);

        if (isReleased) {
            eventPublisher.publishEvent(new ContentReleasedEvent(
                    this, saved.getId(), "MATERIAL", saved.getTitle(), course.getId(), saved.getLessonId(), nowUtc));
        }

        logger.info("Configured release for material ID {} to UTC {} (zone: {}, isReleased: {})",
                materialId, releaseAtUtc, request.getTimeZone(), isReleased);

        return new ScheduledReleaseResponse(
                saved.getId(), "MATERIAL", saved.getTitle(), course.getId(),
                saved.getLessonId(), saved.getReleaseAt(), saved.getIsReleased(), saved.getTimeZone());
    }

    /**
     * Schedules release date-time for a lesson (FR-I18).
     */
    public ScheduledReleaseResponse scheduleLessonRelease(
            User instructor, Integer lessonId, ScheduleContentReleaseRequest request) {

        validateInstructorRole(instructor);
        if (lessonId == null) {
            throw new IllegalArgumentException("Lesson ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Schedule request cannot be null");
        }

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + lessonId));

        Course course = extractCourseFromLesson(lesson);
        assertInstructorAccess(course, instructor);

        LocalDateTime releaseAtUtc = request.resolveUtcReleaseAt();
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        boolean isReleased = !releaseAtUtc.isAfter(nowUtc);

        lesson.setReleaseAt(releaseAtUtc);
        lesson.setIsReleased(isReleased);
        lesson.setTimeZone(request.getTimeZone());

        Lesson saved = lessonRepository.save(lesson);

        if (isReleased) {
            eventPublisher.publishEvent(new ContentReleasedEvent(
                    this, saved.getId(), "LESSON", saved.getTitle(), course.getId(), saved.getId(), nowUtc));
        }

        logger.info("Configured release for lesson ID {} to UTC {} (zone: {}, isReleased: {})",
                lessonId, releaseAtUtc, request.getTimeZone(), isReleased);

        return new ScheduledReleaseResponse(
                saved.getId(), "LESSON", saved.getTitle(), course.getId(),
                saved.getId(), saved.getReleaseAt(), saved.getIsReleased(), saved.getTimeZone());
    }

    /**
     * Get release schedule for a material.
     */
    @Transactional(readOnly = true)
    public ScheduledReleaseResponse getMaterialReleaseSchedule(User instructor, Integer materialId) {
        validateInstructorRole(instructor);
        LectureMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + materialId));

        Course course = resolveCourseFromMaterial(material);
        assertInstructorAccess(course, instructor);

        return new ScheduledReleaseResponse(
                material.getId(), "MATERIAL", material.getTitle(), course.getId(),
                material.getLessonId(), material.getReleaseAt(), material.getIsReleased(), material.getTimeZone());
    }

    /**
     * Get release schedule for a lesson.
     */
    @Transactional(readOnly = true)
    public ScheduledReleaseResponse getLessonReleaseSchedule(User instructor, Integer lessonId) {
        validateInstructorRole(instructor);
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + lessonId));

        Course course = extractCourseFromLesson(lesson);
        assertInstructorAccess(course, instructor);

        return new ScheduledReleaseResponse(
                lesson.getId(), "LESSON", lesson.getTitle(), course.getId(),
                lesson.getId(), lesson.getReleaseAt(), lesson.getIsReleased(), lesson.getTimeZone());
    }

    private Integer resolveCourseIdFromMaterial(LectureMaterial material) {
        try {
            Course c = resolveCourseFromMaterial(material);
            return c != null ? c.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Course resolveCourseFromMaterial(LectureMaterial material) {
        if (material.getLessonId() != null) {
            Lesson lesson = lessonRepository.findById(material.getLessonId()).orElse(null);
            if (lesson != null) {
                return extractCourseFromLesson(lesson);
            }
        }
        return null;
    }

    private Course extractCourseFromLesson(Lesson lesson) {
        if (lesson.getSection() != null &&
                lesson.getSection().getModule() != null &&
                lesson.getSection().getModule().getCourse() != null) {
            return lesson.getSection().getModule().getCourse();
        }
        throw new ResourceNotFoundException("Lesson hierarchy is missing course association");
    }

    private void validateInstructorRole(User user) {
        if (user == null) {
            throw new AccessDeniedException("Full authentication is required");
        }
        if (!"instructor".equalsIgnoreCase(user.getRole()) && !"admin".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Only instructors or administrators can manage content release schedules");
        }
    }

    private void assertInstructorAccess(Course course, User instructor) {
        if (course == null) {
            throw new ResourceNotFoundException("Associated course not found");
        }
        if (instructor == null) {
            throw new AccessDeniedException("Full authentication is required");
        }
        if ("admin".equalsIgnoreCase(instructor.getRole())) {
            return;
        }
        if (course.getInstructor() != null && Objects.equals(course.getInstructor().getId(), instructor.getId())) {
            return;
        }
        if (courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), instructor.getId())) {
            return;
        }
        throw new AccessDeniedException("You are not authorized to manage content for course: " + course.getTitle());
    }
}

