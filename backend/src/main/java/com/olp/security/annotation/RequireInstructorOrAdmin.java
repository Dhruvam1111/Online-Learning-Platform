package com.olp.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

/**
 * Meta-annotation for endpoints accessible by either instructors or administrators.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@PreAuthorize("hasAnyRole('instructor', 'admin')")
public @interface RequireInstructorOrAdmin {
}

