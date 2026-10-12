package com.olp.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CourseStatusConverter implements AttributeConverter<CourseStatus, String> {

    @Override
    public String convertToDatabaseColumn(CourseStatus status) {
        if (status == null) {
            return null;
        }
        return status.name().toLowerCase();
    }

    @Override
    public CourseStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return CourseStatus.fromString(dbData);
    }
}
