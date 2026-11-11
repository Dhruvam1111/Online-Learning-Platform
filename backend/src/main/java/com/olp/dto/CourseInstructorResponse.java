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

        List<CourseInstructor> mappings = null;
        if (courseInstructorRepository != null && course.getId() != null) {
            mappings = courseInstructorRepository.findByCourseIdWithInstructor(course.getId());
        }
        if ((mappings == null || mappings.isEmpty()) && course.getInstructors() != null) {
            mappings = course.getInstructors();
        }

        Map<Integer, CourseInstructor> byInstructorId = new LinkedHashMap<>();
        if (course.getInstructor() != null && course.getInstructor().getId() != null) {
            User primary = course.getInstructor();
            CourseInstructor ownerEntry = new CourseInstructor(course, primary, CourseInstructorRole.OWNER);
            ownerEntry.setCreatedAt(course.getCreatedAt());
            byInstructorId.put(primary.getId(), ownerEntry);
        }

        if (mappings != null) {
            for (CourseInstructor ci : mappings) {
                if (ci == null || ci.getInstructor() == null || ci.getInstructor().getId() == null) {
                    continue;
                }
                Integer instId = ci.getInstructor().getId();
                // Preserve OWNER timestamp/details if mapping row exists for creator, otherwise append co-instructor
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

        if (byInstructorId.isEmpty()) {
            return List.of();
        }

        Map<Integer, InstructorProfile> profileMap = new HashMap<>();
        if (instructorProfileRepository != null) {
            List<Integer> ids = new ArrayList<>(byInstructorId.keySet());
            List<InstructorProfile> profiles = instructorProfileRepository.findByInstructorIdIn(ids);
            if (profiles != null) {
                for (InstructorProfile profile : profiles) {
                    if (profile != null && profile.getInstructor() != null && profile.getInstructor().getId() != null) {
                        profileMap.put(profile.getInstructor().getId(), profile);
                    }
                }
            }
        }

        List<CourseInstructorResponse> owners = new ArrayList<>();
        List<CourseInstructorResponse> coInstructors = new ArrayList<>();
        for (Map.Entry<Integer, CourseInstructor> entry : byInstructorId.entrySet()) {
            CourseInstructor ci = entry.getValue();
            InstructorProfile profile = profileMap.get(entry.getKey());
            CourseInstructorResponse dto = from(ci, profile);
            if (dto.getRole() == CourseInstructorRole.OWNER) {
                owners.add(dto);
            } else {
                coInstructors.add(dto);
            }
        }

        List<CourseInstructorResponse> ordered = new ArrayList<>(owners.size() + coInstructors.size());
        ordered.addAll(owners);
        ordered.addAll(coInstructors);
        return ordered;
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
