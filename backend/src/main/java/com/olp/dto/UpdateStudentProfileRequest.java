package com.olp.dto;

import jakarta.validation.constraints.Size;

/**
 * All fields are optional: a null field means "leave unchanged".
 * Role, email, password and IDs are intentionally absent so they can never be updated here.
 */
public record UpdateStudentProfileRequest(
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Size(max = 100, message = "Education level must be at most 100 characters")
        String educationLevel,

        @Size(max = 1000, message = "Interests must be at most 1000 characters")
        String interests,

        @Size(max = 1000, message = "Learning goals must be at most 1000 characters")
        String learningGoals,

        @Size(max = 2000, message = "Bio must be at most 2000 characters")
        String bio
) {}