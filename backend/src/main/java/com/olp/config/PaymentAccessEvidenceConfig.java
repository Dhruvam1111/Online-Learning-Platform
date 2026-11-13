package com.olp.config;

import com.olp.entity.User;
import com.olp.repository.EnrollmentRepository;
import com.olp.service.PaymentAuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Configuration
public class PaymentAccessEvidenceConfig implements WebMvcConfigurer {

    private final EnrollmentRepository enrollments;
    private final PaymentAuditService audit;

    public PaymentAccessEvidenceConfig(EnrollmentRepository enrollments, PaymentAuditService audit) {
        this.enrollments = enrollments;
        this.audit = audit;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public void afterCompletion(
                    @NonNull HttpServletRequest request,
                    @NonNull HttpServletResponse response,
                    @NonNull Object handler,
                    @Nullable Exception exception) {
                if (response.getStatus() >= 400 || exception != null) {
                    return;
                }
                var auth = SecurityContextHolder.getContext().getAuthentication();
                var matcher = Pattern.compile("/api/courses/([0-9]+)/(progress|lectures/.*)").matcher(request.getRequestURI());
                if (auth == null || !(auth.getPrincipal() instanceof User user) || !"student".equals(user.getRole()) || !matcher.matches()) {
                    return;
                }
                Integer course;
                try {
                    course = Integer.valueOf(matcher.group(1));
                } catch (NumberFormatException ex) {
                    return;
                }
                if (enrollments.existsByStudentIdAndCourseId(user.getId(), course)) {
                    audit.record(
                            "access-" + UUID.randomUUID(),
                            "course.access",
                            Map.of(
                                    "studentId", user.getId(),
                                    "courseId", course,
                                    "method", request.getMethod(),
                                    "path", request.getRequestURI(),
                                    "status", response.getStatus()
                            )
                    );
                }
            }
        }).addPathPatterns("/api/courses/**");
    }
}
