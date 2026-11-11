package com.olp.dto;

import java.time.LocalDateTime;

/**
 * Reusable Instructor Summary DTO alias for instructor cards on course pages.
 */
public class InstructorSummaryDTO extends InstructorSummaryResponse {

    public InstructorSummaryDTO() {
        super();
    }

    public InstructorSummaryDTO(Integer id, String name, String email,
                                String qualifications, String expertiseAreas, String bio) {
        super(id, name, email, qualifications, expertiseAreas, bio);
    }

    public InstructorSummaryDTO(Integer id, String name, String email,
                                String qualifications, String expertiseAreas, String bio,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, name, email, qualifications, expertiseAreas, bio, createdAt, updatedAt);
    }
}

