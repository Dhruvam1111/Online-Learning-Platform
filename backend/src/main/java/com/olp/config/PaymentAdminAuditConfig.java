package com.olp.config;
import com.olp.entity.User;
import com.olp.service.PaymentAuditService;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.*;
import java.util.*;
/** Records authenticated administrator API actions without persisting request bodies or credentials. */
@Configuration
public class PaymentAdminAuditConfig implements WebMvcConfigurer {
    private final PaymentAuditService audit;
    public PaymentAdminAuditConfig(PaymentAuditService audit){this.audit=audit;}
    @Override public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(new HandlerInterceptor(){
            @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler){
                var auth=SecurityContextHolder.getContext().getAuthentication();
                if(auth!=null && auth.getPrincipal() instanceof User user && "admin".equals(user.getRole()))
                {
                    String id="admin-action-"+UUID.randomUUID();
                    request.setAttribute("paymentAuditId",id);request.setAttribute("paymentAuditActor",user.getId());
                    audit.record(id,"admin.payment.action",Map.of("actorId",user.getId(),"method",request.getMethod(),"path",request.getRequestURI(),"phase","requested"));
                }
                return true;
            }
            @Override public void afterCompletion(HttpServletRequest request,HttpServletResponse response,Object handler,Exception exception){
                Object id=request.getAttribute("paymentAuditId");if(id==null)return;
                audit.record(id+"-result","admin.payment.outcome",Map.of("requestEventId",id,"actorId",request.getAttribute("paymentAuditActor"),"method",request.getMethod(),"path",request.getRequestURI(),"httpStatus",response.getStatus(),"outcome",response.getStatus()<400 && exception==null?"succeeded":"failed"));
            }
        }).addPathPatterns("/api/**");
    }
}
