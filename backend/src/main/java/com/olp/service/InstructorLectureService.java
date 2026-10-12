package com.olp.service;

import com.olp.dto.CreateLectureRequest;
import com.olp.dto.LectureResponse;
import com.olp.dto.ReorderLecturesRequest;
import com.olp.dto.UpdateLectureRequest;
import com.olp.entity.Course;
import com.olp.entity.Lecture;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseRepository;
import com.olp.repository.LectureRepository;
import com.olp.repository.UserRepository;
import com.olp.util.YoutubeUtil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class InstructorLectureService {

    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;

    public InstructorLectureService(CourseRepository courseRepository,
                                    LectureRepository lectureRepository,
                                    UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.lectureRepository = lectureRepository;
        this.userRepository = userRepository;
    }

    public User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
    }

    /**
     * List all lectures for a course owned by the authenticated instructor, in order.
     */
    @Transactional(readOnly = true)
    public List<LectureResponse> getCourseLectures(User currentUser, Integer courseId) {
        validateInstructor(currentUser);
        verifyCourseOwnership(courseId, currentUser);

        List<Lecture> lectures = lectureRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        return lectures.stream()
                .map(LectureResponse::fromEntity)
                .toList();
    }

    /**
     * Reorder lectures within an owned course.
     */
    @Transactional
    public List<LectureResponse> reorderLectures(User currentUser, Integer courseId, ReorderLecturesRequest request) {
        validateInstructor(currentUser);
        verifyCourseOwnership(courseId, currentUser);

        if (request == null || request.getLectureIds() == null || request.getLectureIds().isEmpty()) {
            throw new IllegalArgumentException("Lecture IDs list cannot be empty");
        }

        List<Integer> requestedIds = request.getLectureIds();

        // 1. Check for null elements
        for (Integer id : requestedIds) {
            if (id == null) {
                throw new IllegalArgumentException("Lecture IDs list must not contain null elements");
            }
        }

        // 2. Check for duplicate IDs
        Set<Integer> uniqueIds = new HashSet<>(requestedIds);
        if (uniqueIds.size() != requestedIds.size()) {
            throw new IllegalArgumentException("Duplicate lecture IDs found in reorder request");
        }

        // 3. Fetch all current lectures for the course
        List<Lecture> existingLectures = lectureRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        Map<Integer, Lecture> lectureMap = existingLectures.stream()
                .collect(Collectors.toMap(Lecture::getId, l -> l));

        // 4. Verify all IDs belong to this course
        for (Integer id : requestedIds) {
            if (!lectureMap.containsKey(id)) {
                throw new IllegalArgumentException("Lecture with ID " + id + " does not belong to course " + courseId);
            }
        }

        // 5. Verify all existing lectures are included
        if (requestedIds.size() != existingLectures.size()) {
            throw new IllegalArgumentException("Reorder request must include all " + existingLectures.size() + " lectures of the course");
        }

        // 6. Update orderIndex sequentially starting from 1
        for (int i = 0; i < requestedIds.size(); i++) {
            Integer id = requestedIds.get(i);
            Lecture lecture = lectureMap.get(id);
            lecture.setOrderIndex(i + 1);
        }

        lectureRepository.saveAll(existingLectures);

        // 7. Return refreshed lectures sorted by new orderIndex
        List<Lecture> reorderedLectures = lectureRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        return reorderedLectures.stream()
                .map(LectureResponse::fromEntity)
                .toList();
    }

    /**
     * Delete a lecture from an owned course.
     */
    @Transactional
    public void deleteLecture(User currentUser, Integer courseId, Integer lectureId) {
        validateInstructor(currentUser);
        verifyCourseOwnership(courseId, currentUser);

        if (lectureId == null) {
            throw new IllegalArgumentException("Lecture ID cannot be null");
        }

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found with id: " + lectureId));

        if (lecture.getCourse() == null || !lecture.getCourse().getId().equals(courseId)) {
            throw new IllegalArgumentException("Lecture with ID " + lectureId + " does not belong to course " + courseId);
        }

        lectureRepository.delete(lecture);

        // Normalize remaining order indexes to keep order contiguous (1, 2, 3...)
        List<Lecture> remaining = lectureRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setOrderIndex(i + 1);
        }
        lectureRepository.saveAll(remaining);
    }

    /**
     * Create a new lecture for an owned course.
     */
    @Transactional
    public LectureResponse createLecture(User currentUser, Integer courseId, CreateLectureRequest request) {
        validateInstructor(currentUser);
        Course course = verifyCourseOwnership(courseId, currentUser);

        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }

        String rawUrl = request.getEffectiveYoutubeUrl();
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("YouTube URL is required");
        }

        String videoId = YoutubeUtil.extractVideoId(rawUrl);
        if (videoId == null) {
            throw new IllegalArgumentException("Invalid YouTube URL: " + rawUrl + ". Accepted formats: youtube.com/watch?v=..., youtu.be/..., youtube.com/embed/...");
        }

        int nextOrder;
        if (request.getEffectiveOrderIndex() != null && request.getEffectiveOrderIndex() > 0) {
            nextOrder = request.getEffectiveOrderIndex();
        } else {
            nextOrder = lectureRepository.countByCourseId(courseId) + 1;
        }

        Lecture lecture = new Lecture(
                course,
                request.getTitle().trim(),
                rawUrl.trim(),
                videoId,
                nextOrder
        );

        Lecture saved = lectureRepository.save(lecture);
        return LectureResponse.fromEntity(saved);
    }

    /**
     * Update an existing lecture in an owned course.
     */
    @Transactional
    public LectureResponse updateLecture(User currentUser, Integer courseId, Integer lectureId, UpdateLectureRequest request) {
        validateInstructor(currentUser);
        verifyCourseOwnership(courseId, currentUser);

        if (lectureId == null) {
            throw new IllegalArgumentException("Lecture ID cannot be null");
        }

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found with id: " + lectureId));

        if (lecture.getCourse() == null || !lecture.getCourse().getId().equals(courseId)) {
            throw new IllegalArgumentException("Lecture with ID " + lectureId + " does not belong to course " + courseId);
        }

        if (request != null) {
            if (request.getTitle() != null) {
                if (request.getTitle().trim().isEmpty()) {
                    throw new IllegalArgumentException("Title cannot be blank");
                }
                lecture.setTitle(request.getTitle().trim());
            }

            String rawUrl = request.getEffectiveYoutubeUrl();
            if (rawUrl != null) {
                if (rawUrl.isBlank()) {
                    throw new IllegalArgumentException("YouTube URL cannot be blank");
                }
                String videoId = YoutubeUtil.extractVideoId(rawUrl);
                if (videoId == null) {
                    throw new IllegalArgumentException("Invalid YouTube URL: " + rawUrl);
                }
                lecture.setYoutubeUrl(rawUrl.trim());
                lecture.setVideoId(videoId);
            }

            if (request.getEffectiveOrderIndex() != null && request.getEffectiveOrderIndex() > 0) {
                lecture.setOrderIndex(request.getEffectiveOrderIndex());
            }
        }

        Lecture updated = lectureRepository.save(lecture);
        return LectureResponse.fromEntity(updated);
    }

    private Course verifyCourseOwnership(Integer courseId, User currentUser) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        if (course.getInstructor() == null || !currentUser.getId().equals(course.getInstructor().getId())) {
            throw new AccessDeniedException("Cannot modify another instructor's course");
        }

        return course;
    }

    private void validateInstructor(User currentUser) {
        if (currentUser == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (!"instructor".equalsIgnoreCase(currentUser.getRole())) {
            throw new AccessDeniedException("Access denied: only instructors can access instructor lectures");
        }
    }
}
