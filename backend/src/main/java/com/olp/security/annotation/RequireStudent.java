package com.olp.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

/**
 * Meta-annotation for student-only endpoints.
 * Enforces that the current authenticated user has the 'student' role.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@PreAuthorize("hasRole('student')")
public @interface RequireStudent {
}

