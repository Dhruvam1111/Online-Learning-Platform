package com.olp.dto;

public record StudentProfileResponse(
        Integer id,
        String name,
        String email,
        String role,
        String educationLevel,
        String interests,
        String learningGoals,
        String bio
) {}