package com.olp.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter mapping CourseInstructorRole enum (OWNER, CO_INSTRUCTOR)
 * to/from the database representation ('owner', 'co-instructor').
 */
@Converter(autoApply = true)
public class CourseInstructorRoleConverter implements AttributeConverter<CourseInstructorRole, String> {

    @Override
    public String convertToDatabaseColumn(CourseInstructorRole role) {
        if (role == null) {
            return null;
        }
        return role.toDatabaseValue();
    }

    @Override
    public CourseInstructorRole convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return CourseInstructorRole.fromString(dbData);
    }
}

