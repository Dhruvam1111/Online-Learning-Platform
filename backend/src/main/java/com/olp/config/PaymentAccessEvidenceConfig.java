package com.olp.config;
import com.olp.entity.User;
import com.olp.repository.EnrollmentRepository;
import com.olp.service.PaymentAuditService;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.*;
import java.util.*;
@Configuration
public class PaymentAccessEvidenceConfig implements WebMvcConfigurer {
    private final EnrollmentRepository enrollments;
    private final PaymentAuditService audit;
    public PaymentAccessEvidenceConfig(EnrollmentRepository enrollments,PaymentAuditService audit){this.enrollments=enrollments;this.audit=audit;}
    @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new HandlerInterceptor(){
        @Override public void afterCompletion(HttpServletRequest request,HttpServletResponse response,Object handler,Exception exception){
            if(response.getStatus()>=400 || exception!=null)return;
            var auth=SecurityContextHolder.getContext().getAuthentication();
            var matcher=java.util.regex.Pattern.compile("/api/courses/([0-9]+)/(progress|lectures/.*)").matcher(request.getRequestURI());
            if(auth==null || !(auth.getPrincipal() instanceof User user) || !"student".equals(user.getRole()) || !matcher.matches())return;
            Integer course;try{course=Integer.valueOf(matcher.group(1));}catch(NumberFormatException ex){return;}
            if(enrollments.existsByStudentIdAndCourseId(user.getId(),course))audit.record("access-"+UUID.randomUUID(),"course.access",Map.of("studentId",user.getId(),"courseId",course,"method",request.getMethod(),"path",request.getRequestURI(),"status",response.getStatus()));
        }
    }).addPathPatterns("/api/courses/**");}
}
