package com.olp.dto;

import com.olp.entity.Course;
import com.olp.entity.CourseInstructor;
import com.olp.entity.CourseInstructorRole;
import com.olp.entity.InstructorProfile;
import com.olp.entity.User;
import com.olp.repository.CourseInstructorRepository;
import com.olp.repository.InstructorProfileRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Response DTO representing an instructor associated with a course,
 * including their role (OWNER or CO_INSTRUCTOR) and basic profile information (FR-I59).
 */
public class CourseInstructorResponse {

    private Integer id;
    private Integer instructorId;
    private String name;
    private String email;
    private CourseInstructorRole role;
    private String qualifications;
    private String expertiseAreas;
    private String bio;
    private String profilePictureUrl;
    private LocalDateTime assignedAt;

    public CourseInstructorResponse() {
    }

    public CourseInstructorResponse(
            Integer instructorId,
            String name,
            String email,
            CourseInstructorRole role,
            String qualifications,
            String expertiseAreas,
            String bio,
            String profilePictureUrl,
            LocalDateTime assignedAt) {
        this.id = instructorId;
        this.instructorId = instructorId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.qualifications = qualifications;
        this.expertiseAreas = expertiseAreas;
        this.bio = bio;
        this.profilePictureUrl = profilePictureUrl;
        this.assignedAt = assignedAt;
    }

    public static CourseInstructorResponse from(CourseInstructor mapping, InstructorProfile profile) {
        if (mapping == null || mapping.getInstructor() == null) {
            return null;
        }
        return fromUser(mapping.getInstructor(), mapping.getRole(), profile, mapping.getCreatedAt());
    }

    public static CourseInstructorResponse fromUser(
            User instructor,
            CourseInstructorRole role,
            InstructorProfile profile,
            LocalDateTime assignedAt) {
        if (instructor == null) {
            return null;
        }
        return new CourseInstructorResponse(
                instructor.getId(),
                instructor.getName(),
                instructor.getEmail(),
                role != null ? role : CourseInstructorRole.OWNER,
                profile != null ? profile.getQualifications() : null,
                profile != null ? profile.getExpertiseAreas() : null,
                profile != null ? profile.getBio() : null,
                profile != null ? profile.getProfilePictureUrl() : null,
                assignedAt
        );
    }

    /**
     * Resolves deduplicated instructors for a course (OWNER first, then CO_INSTRUCTORs),
     * enriched with basic profile details when InstructorProfileRepository is available.
     */
    public static List<CourseInstructorResponse> resolveInstructorsForCourse(
            Course course,
            CourseInstructorRepository courseInstructorRepository,
            InstructorProfileRepository instructorProfileRepository) {
        if (course == null) {
            return List.of();
        }
        Map<Integer, List<CourseInstructorResponse>> batch = resolveInstructorsForCourses(
                List.of(course), courseInstructorRepository, instructorProfileRepository);
        if (course.getId() != null && batch.containsKey(course.getId())) {
            return batch.get(course.getId());
        }
        return batch.values().stream().findFirst().orElse(List.of());
    }

    /**
     * Batch-resolves deduplicated instructors for a list of courses in constant queries
     * (OWNER first, then CO_INSTRUCTORs), avoiding N+1 queries.
     */
    public static Map<Integer, List<CourseInstructorResponse>> resolveInstructorsForCourses(
            List<Course> courses,
            CourseInstructorRepository courseInstructorRepository,
            InstructorProfileRepository instructorProfileRepository) {
        if (courses == null || courses.isEmpty()) {
            return Map.of();
        }

        List<Integer> courseIds = new ArrayList<>();
        for (Course c : courses) {
            if (c != null && c.getId() != null) {
                courseIds.add(c.getId());
            }
        }

        Map<Integer, List<CourseInstructor>> mappingsByCourse = new HashMap<>();
        if (courseInstructorRepository != null && !courseIds.isEmpty()) {
            List<CourseInstructor> fetchedMappings = courseIds.size() == 1
                    ? courseInstructorRepository.findByCourseIdWithInstructor(courseIds.get(0))
                    : courseInstructorRepository.findByCourseIdInWithInstructor(courseIds);
            if (fetchedMappings != null) {
                for (CourseInstructor ci : fetchedMappings) {
                    if (ci != null && ci.getCourse() != null && ci.getCourse().getId() != null) {
                        mappingsByCourse
                                .computeIfAbsent(ci.getCourse().getId(), k -> new ArrayList<>())
                                .add(ci);
                    }
                }
            }
        }

        Map<Integer, Map<Integer, CourseInstructor>> mergedByCourse = new LinkedHashMap<>();
        LinkedHashMap<Integer, Integer> distinctInstructorIds = new LinkedHashMap<>();

        for (int idx = 0; idx < courses.size(); idx++) {
            Course course = courses.get(idx);
            if (course == null) {
                continue;
            }
            Integer courseKey = course.getId() != null ? course.getId() : -(idx + 1);
            Map<Integer, CourseInstructor> byInstructorId = new LinkedHashMap<>();

            if (course.getInstructor() != null && course.getInstructor().getId() != null) {
                User primary = course.getInstructor();
                CourseInstructor ownerEntry = new CourseInstructor(course, primary, CourseInstructorRole.OWNER);
                ownerEntry.setCreatedAt(course.getCreatedAt());
                byInstructorId.put(primary.getId(), ownerEntry);
                distinctInstructorIds.putIfAbsent(primary.getId(), primary.getId());
            }

            List<CourseInstructor> mappings = course.getId() != null ? mappingsByCourse.get(course.getId()) : null;
            if ((mappings == null || mappings.isEmpty()) && course.getInstructors() != null) {
                mappings = course.getInstructors();
            }

            if (mappings != null) {
                for (CourseInstructor ci : mappings) {
                    if (ci == null || ci.getInstructor() == null || ci.getInstructor().getId() == null) {
                        continue;
                    }
                    Integer instId = ci.getInstructor().getId();
                    distinctInstructorIds.putIfAbsent(instId, instId);
                    if (!byInstructorId.containsKey(instId)) {
                        byInstructorId.put(instId, ci);
                    } else if (ci.getRole() != null) {
                        CourseInstructor existing = byInstructorId.get(instId);
                        if (existing.getCreatedAt() == null && ci.getCreatedAt() != null) {
                            existing.setCreatedAt(ci.getCreatedAt());
                        }
                    }
                }
            }

            mergedByCourse.put(courseKey, byInstructorId);
        }

        Map<Integer, InstructorProfile> profileMap = new HashMap<>();
        if (instructorProfileRepository != null && !distinctInstructorIds.isEmpty()) {
            List<InstructorProfile> profiles = instructorProfileRepository
                    .findByInstructorIdIn(new ArrayList<>(distinctInstructorIds.keySet()));
            if (profiles != null) {
                for (InstructorProfile profile : profiles) {
                    if (profile != null && profile.getInstructor() != null && profile.getInstructor().getId() != null) {
                        profileMap.put(profile.getInstructor().getId(), profile);
                    }
                }
            }
        }

        Map<Integer, List<CourseInstructorResponse>> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, Map<Integer, CourseInstructor>> courseEntry : mergedByCourse.entrySet()) {
            List<CourseInstructorResponse> owners = new ArrayList<>();
            List<CourseInstructorResponse> coInstructors = new ArrayList<>();

            for (Map.Entry<Integer, CourseInstructor> instEntry : courseEntry.getValue().entrySet()) {
                InstructorProfile profile = profileMap.get(instEntry.getKey());
                CourseInstructorResponse dto = from(instEntry.getValue(), profile);
                if (dto.getRole() == CourseInstructorRole.OWNER) {
                    owners.add(dto);
                } else {
                    coInstructors.add(dto);
                }
            }

            List<CourseInstructorResponse> ordered = new ArrayList<>(owners.size() + coInstructors.size());
            ordered.addAll(owners);
            ordered.addAll(coInstructors);
            result.put(courseEntry.getKey(), ordered);
        }

        return result;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
        this.instructorId = id;
    }

    public Integer getInstructorId() {
        return instructorId != null ? instructorId : id;
    }

    public void setInstructorId(Integer instructorId) {
        this.instructorId = instructorId;
        this.id = instructorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public CourseInstructorRole getRole() {
        return role;
    }

    public void setRole(CourseInstructorRole role) {
        this.role = role;
    }

    public String getQualifications() {
        return qualifications;
    }

    public void setQualifications(String qualifications) {
        this.qualifications = qualifications;
    }

    public String getExpertiseAreas() {
        return expertiseAreas;
    }

    public void setExpertiseAreas(String expertiseAreas) {
        this.expertiseAreas = expertiseAreas;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}
