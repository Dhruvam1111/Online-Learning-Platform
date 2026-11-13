package com.olp.config;

import com.olp.service.PaymentAuditService;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Links completed payment-admin requests to returned object IDs without recording secrets or bodies. */
@RestControllerAdvice(basePackages = "com.olp.controller")
public class PaymentAdminResponseAudit implements ResponseBodyAdvice<Object> {

    private final PaymentAuditService audit;

    public PaymentAdminResponseAudit(PaymentAuditService audit) {
        this.audit = audit;
    }

    @Override
    public boolean supports(
            @NonNull MethodParameter parameter,
            @NonNull Class<? extends HttpMessageConverter<?>> converter) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            @Nullable Object body,
            @NonNull MethodParameter parameter,
            @NonNull MediaType mediaType,
            @NonNull Class<? extends HttpMessageConverter<?>> converter,
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response) {

        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return body;
        }

        var servlet = attributes.getRequest();
        Object id = servlet.getAttribute("paymentAuditId");
        if (id == null) {
            return body;
        }

        var subject = new LinkedHashMap<String, Object>();
        for (String field : List.of("orderId", "paymentId", "refundId", "instructorId", "earningId", "accountId", "id", "reference", "utrNumber")) {
            Object value = null;
            if (body instanceof Map<?, ?> map) {
                value = map.get(field);
            } else if (body != null) {
                var bean = new BeanWrapperImpl(body);
                if (bean.isReadableProperty(field)) {
                    value = bean.getPropertyValue(field);
                }
            }
            if (value instanceof Number || (value instanceof String text && text.length() <= 128)) {
                subject.put(field, value);
            }
        }
        audit.record(
                id + "-subject",
                "admin.payment.subject",
                Map.of("requestEventId", id, "actorId", servlet.getAttribute("paymentAuditActor"), "subject", subject)
        );
        return body;
    }
}
