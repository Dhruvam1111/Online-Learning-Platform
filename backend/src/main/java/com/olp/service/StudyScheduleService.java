package com.olp.service;

import com.olp.dto.StudyScheduleGenerateRequest;
import com.olp.dto.StudyScheduleRequest;
import com.olp.dto.StudyScheduleResponse;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing student study schedules and automated schedule generation (FR-S52, DR-ED08).
 * Schedules are stored in UTC and respect course deadlines and student completion progress.
 */
@Service
public class StudyScheduleService {

    private final StudyScheduleRepository studyScheduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final LectureProgressRepository lectureProgressRepository;
    private final ExamRepository examRepository;
    private final AssignmentRepository assignmentRepository;

    public StudyScheduleService(StudyScheduleRepository studyScheduleRepository,
                                EnrollmentRepository enrollmentRepository,
                                CourseRepository courseRepository,
                                LectureRepository lectureRepository,
                                LectureProgressRepository lectureProgressRepository,
                                ExamRepository examRepository,
                                AssignmentRepository assignmentRepository) {
        this.studyScheduleRepository = studyScheduleRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.lectureRepository = lectureRepository;
        this.lectureProgressRepository = lectureProgressRepository;
        this.examRepository = examRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public List<StudyScheduleResponse> getStudentSchedule(User student, LocalDateTime start, LocalDateTime end) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        List<StudySchedule> items;
        if (start != null && end != null) {
            items = studyScheduleRepository.findByStudentIdAndWindow(student.getId(), start, end);
        } else {
            items = studyScheduleRepository.findByStudentIdOrderByStart(student.getId());
        }
        return items.stream().map(StudyScheduleResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public StudyScheduleResponse createCustomItem(User student, StudyScheduleRequest request) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
        if (request.getScheduledStart() == null || request.getScheduledEnd() == null) {
            throw new IllegalArgumentException("Scheduled start and end times are required");
        }
        if (request.getScheduledEnd().isBefore(request.getScheduledStart())) {
            throw new IllegalArgumentException("Scheduled end time cannot be before start time");
        }

        Course course = null;
        if (request.getCourseId() != null) {
            course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getCourseId()));
        }

        StudySchedule schedule = new StudySchedule(
                student,
                course,
                request.getTitle().trim(),
                request.getTaskDescription(),
                request.getItemType() != null ? request.getItemType().toUpperCase() : "CUSTOM",
                request.getReferenceId(),
                request.getScheduledStart(),
                request.getScheduledEnd(),
                request.getPriority() != null ? request.getPriority().toUpperCase() : "MEDIUM",
                request.getTimeZone() != null ? request.getTimeZone() : "UTC"
        );

        StudySchedule saved = studyScheduleRepository.save(schedule);
        return StudyScheduleResponse.fromEntity(saved);
    }

    @Transactional
    public StudyScheduleResponse markItemCompleted(User student, Integer itemId, Boolean isCompleted) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        StudySchedule schedule = studyScheduleRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Study schedule item not found: " + itemId));

        if (!student.getId().equals(schedule.getStudent().getId())) {
            throw new AccessDeniedException("You do not have permission to modify this schedule item");
        }

        boolean completed = Boolean.TRUE.equals(isCompleted);
        schedule.setIsCompleted(completed);
        schedule.setCompletedAt(completed ? LocalDateTime.now() : null);

        StudySchedule saved = studyScheduleRepository.save(schedule);
        return StudyScheduleResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteItem(User student, Integer itemId) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        StudySchedule schedule = studyScheduleRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Study schedule item not found: " + itemId));

        if (!student.getId().equals(schedule.getStudent().getId())) {
            throw new AccessDeniedException("You do not have permission to delete this schedule item");
        }

        studyScheduleRepository.delete(schedule);
    }

    @Transactional
    public List<StudyScheduleResponse> generatePersonalizedSchedule(User student, StudyScheduleGenerateRequest request) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }

        int daysAhead = (request != null && request.getDaysAhead() != null && request.getDaysAhead() > 0)
                ? request.getDaysAhead() : 14;
        String timeZone = (request != null && request.getTimeZone() != null) ? request.getTimeZone() : "Asia/Kolkata";

        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());
        List<StudySchedule> createdList = new ArrayList<>();

        LocalDateTime cursor = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        int dayOffset = 0;

        for (Enrollment enrollment : enrollments) {
            Course course = enrollment.getCourse();
            if (course == null) continue;

            // 1. Upcoming exams for course
            List<Exam> exams = examRepository.findByCourseId(course.getId());
            for (Exam exam : exams) {
                if (exam.getStartWindow() != null && exam.getStartWindow().isAfter(LocalDateTime.now())) {
                    List<StudySchedule> existing = studyScheduleRepository.findExistingSchedules(
                            student.getId(), course.getId(), "EXAM", exam.getId());
                    if (existing.isEmpty()) {
                        LocalDateTime prepStart = exam.getStartWindow().minusDays(1).withHour(10).withMinute(0);
                        LocalDateTime prepEnd = prepStart.plusHours(2);
                        StudySchedule item = new StudySchedule(
                                student, course, "Prepare for Exam: " + exam.getTitle(),
                                "Review syllabus and attempt practice questions before exam window opens.",
                                "EXAM", exam.getId(), prepStart, prepEnd, "HIGH", timeZone);
                        createdList.add(studyScheduleRepository.save(item));
                    }
                }
            }

            // 2. Upcoming assignments
            List<Assignment> assignments = assignmentRepository.findByCourseId(course.getId());
            for (Assignment assignment : assignments) {
                if (assignment.getDeadline() != null && assignment.getDeadline().isAfter(LocalDateTime.now())) {
                    List<StudySchedule> existing = studyScheduleRepository.findExistingSchedules(
                            student.getId(), course.getId(), "ASSIGNMENT", assignment.getId());
                    if (existing.isEmpty()) {
                        LocalDateTime slotStart = assignment.getDeadline().minusDays(2).withHour(14).withMinute(0);
                        if (slotStart.isBefore(LocalDateTime.now())) {
                            slotStart = LocalDateTime.now().plusHours(2);
                        }
                        LocalDateTime slotEnd = slotStart.plusHours(1).plusMinutes(30);
                        StudySchedule item = new StudySchedule(
                                student, course, "Assignment Deadline: " + assignment.getTitle(),
                                "Work on assignment rubric: " + assignment.getTitle(),
                                "ASSIGNMENT", assignment.getId(), slotStart, slotEnd, "HIGH", timeZone);
                        createdList.add(studyScheduleRepository.save(item));
                    }
                }
            }

            // 3. Incomplete lectures
            List<Lecture> lectures = lectureRepository.findByCourseIdOrderByOrderIndexAsc(course.getId());
            for (Lecture lecture : lectures) {
                boolean completed = lectureProgressRepository.existsByStudentIdAndLectureId(student.getId(), lecture.getId());
                if (!completed) {
                    List<StudySchedule> existing = studyScheduleRepository.findExistingSchedules(
                            student.getId(), course.getId(), "LECTURE", lecture.getId());
                    if (existing.isEmpty()) {
                        LocalDateTime slotStart = cursor.plusDays(dayOffset).withHour(16).withMinute(0);
                        LocalDateTime slotEnd = slotStart.plusMinutes(45);
                        StudySchedule item = new StudySchedule(
                                student, course, "Watch: " + lecture.getTitle(),
                                "Study lecture content and take notes.",
                                "LECTURE", lecture.getId(), slotStart, slotEnd, "MEDIUM", timeZone);
                        createdList.add(studyScheduleRepository.save(item));
                        dayOffset = (dayOffset + 1) % daysAhead;
                    }
                }
            }
        }

        return createdList.stream().map(StudyScheduleResponse::fromEntity).collect(Collectors.toList());
    }
}
