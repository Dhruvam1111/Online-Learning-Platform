package com.olp.dto;

import com.olp.entity.Course;
import com.olp.entity.CourseInstructor;
import com.olp.entity.CourseInstructorRole;
import com.olp.entity.CourseStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CourseResponse {

    private Integer id;
    private Integer instructorId;
    private String title;
    private String description;
    private String category = "General";
    private List<String> learningOutcomes = new ArrayList<>();
    private List<CoursePrerequisiteResponse> prerequisites = new ArrayList<>();
    private List<CourseInstructorResponse> instructors = new ArrayList<>();
    private CourseStatus status;
    private LocalDateTime createdAt;

    public CourseResponse() {}

    public CourseResponse(Integer id, Integer instructorId, String title, String description,
                          CourseStatus status, LocalDateTime createdAt) {
        this(id, instructorId, title, description, status, createdAt, "General", new ArrayList<>(), new ArrayList<>());
    }

    public CourseResponse(Integer id, Integer instructorId, String title, String description,
                          CourseStatus status, LocalDateTime createdAt,
                          String category, List<String> learningOutcomes,
                          List<CoursePrerequisiteResponse> prerequisites) {
        this.id = id;
        this.instructorId = instructorId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.category = category != null && !category.trim().isEmpty() ? category : "General";
        this.learningOutcomes = learningOutcomes != null ? learningOutcomes : new ArrayList<>();
        this.prerequisites = prerequisites != null ? prerequisites : new ArrayList<>();
    }

    public static CourseResponse fromEntity(Course course) {
        if (course == null) {
            return null;
        }

        List<CoursePrerequisiteResponse> prereqResponses = new ArrayList<>();
        if (course.getPrerequisites() != null) {
            prereqResponses = course.getPrerequisites().stream()
                    .map(CoursePrerequisiteResponse::fromEntity)
                    .toList();
        }

        CourseResponse response = new CourseResponse(
                course.getId(),
                course.getInstructor() != null ? course.getInstructor().getId() : null,
                course.getTitle(),
                course.getDescription(),
                course.getStatus(),
                course.getCreatedAt(),
                course.getCategory(),
                course.getLearningOutcomesList(),
                prereqResponses
        );

        Map<Integer, CourseInstructorResponse> instructorMap = new LinkedHashMap<>();
        if (course.getInstructor() != null && course.getInstructor().getId() != null) {
            instructorMap.put(
                    course.getInstructor().getId(),
                    CourseInstructorResponse.fromUser(
                            course.getInstructor(), CourseInstructorRole.OWNER, null, course.getCreatedAt()));
        }
        if (course.getInstructors() != null) {
            for (CourseInstructor ci : course.getInstructors()) {
                if (ci != null && ci.getInstructor() != null && ci.getInstructor().getId() != null) {
                    CourseInstructorResponse mapped = CourseInstructorResponse.from(ci, null);
                    instructorMap.put(ci.getInstructor().getId(), mapped);
                }
            }
        }
        response.setInstructors(new ArrayList<>(instructorMap.values()));

        return response;
    }

    // ===== Getters and Setters =====

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Integer instructorId) {
        this.instructorId = instructorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getLearningOutcomes() {
        return learningOutcomes;
    }

    public void setLearningOutcomes(List<String> learningOutcomes) {
        this.learningOutcomes = learningOutcomes != null ? learningOutcomes : new ArrayList<>();
    }

    public List<CoursePrerequisiteResponse> getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(List<CoursePrerequisiteResponse> prerequisites) {
        this.prerequisites = prerequisites != null ? prerequisites : new ArrayList<>();
    }

    public List<CourseInstructorResponse> getInstructors() {
        return instructors;
    }

    public void setInstructors(List<CourseInstructorResponse> instructors) {
        this.instructors = instructors != null ? instructors : new ArrayList<>();
    }

    public CourseStatus getStatus() {
        return status;
    }

    public void setStatus(CourseStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
