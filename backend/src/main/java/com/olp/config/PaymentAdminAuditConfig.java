package com.olp.config;

import com.olp.entity.User;
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

/** Records authenticated administrator API actions without persisting request bodies or credentials. */
@Configuration
public class PaymentAdminAuditConfig implements WebMvcConfigurer {

    private final PaymentAuditService audit;

    public PaymentAdminAuditConfig(PaymentAuditService audit) {
        this.audit = audit;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(
                    @NonNull HttpServletRequest request,
                    @NonNull HttpServletResponse response,
                    @NonNull Object handler) {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof User user && "admin".equals(user.getRole())) {
                    String id = "admin-action-" + UUID.randomUUID();
                    request.setAttribute("paymentAuditId", id);
                    request.setAttribute("paymentAuditActor", user.getId());
                    audit.record(
                            id,
                            "admin.payment.action",
                            Map.of("actorId", user.getId(), "method", request.getMethod(), "path", request.getRequestURI(), "phase", "requested")
                    );
                }
                return true;
            }

            @Override
            public void afterCompletion(
                    @NonNull HttpServletRequest request,
                    @NonNull HttpServletResponse response,
                    @NonNull Object handler,
                    @Nullable Exception exception) {
                Object id = request.getAttribute("paymentAuditId");
                if (id == null) {
                    return;
                }
                audit.record(
                        id + "-result",
                        "admin.payment.outcome",
                        Map.of(
                                "requestEventId", id,
                                "actorId", request.getAttribute("paymentAuditActor"),
                                "method", request.getMethod(),
                                "path", request.getRequestURI(),
                                "httpStatus", response.getStatus(),
                                "outcome", response.getStatus() < 400 && exception == null ? "succeeded" : "failed"
                        )
                );
            }
        }).addPathPatterns("/api/**");
    }
}
