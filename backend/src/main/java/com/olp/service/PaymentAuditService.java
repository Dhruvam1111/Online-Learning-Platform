package com.olp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olp.entity.WebhookEvent;
import com.olp.repository.WebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.domain.PageRequest;

/** Append-only simulator and governance journal; never stores full bank/card details. */
@Service
public class PaymentAuditService {
    private final WebhookEventRepository events;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    public PaymentAuditService(WebhookEventRepository events) { this.events = events; }
    @Transactional
    public void record(String id, String type, Map<String, ?> data) {
        if (events.existsByEventId(id)) return;
        try { events.save(new WebhookEvent(id, type, json.writeValueAsString(data))); }
        catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException("Cannot record financial audit", ex); }
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> list(String type) {
        return events.findByEventType(type).stream().map(this::decode).toList();
    }
    @Transactional(readOnly=true)
    public Map<String,Object> get(String id) { return events.findByEventId(id).map(this::decode).orElseThrow(() -> new IllegalArgumentException("Audit record not found")); }
    @Transactional(readOnly=true)
    public boolean exists(String id) { return events.existsByEventId(id); }
    @Transactional(readOnly = true)
    public RefundQueue refundQueue(String status, String search, int page, int size) {
        var results = events.findRefundQueue(status, search, PageRequest.of(page, size));
        var stats = events.getRefundQueueStats();
        var rows = results.getContent().stream().map(row -> {
            var request = decode(row.getRequestPayload(), row.getEventId(), row.getRequestedAt());
            request.put("studentName", row.getStudentName());
            request.put("studentEmail", row.getStudentEmail());
            request.put("courseTitle", row.getCourseTitle());
            request.put("status", "pending");
            if (row.getDecisionPayload() != null) {
                var decision = decode(row.getDecisionPayload(), "decision-" + row.getEventId(), row.getDecidedAt());
                request.put("status", decision.get("decision"));
                request.put("decisionRecord", decision);
            }
            return request;
        }).toList();
        return new RefundQueue(rows, results.getTotalPages(), stats.getPendingCount(), stats.getOldestPendingAt());
    }

    public record RefundQueue(List<Map<String,Object>> content, int totalPages, long pendingCount,
                              LocalDateTime oldestPendingAt) {}

    private Map<String,Object> decode(WebhookEvent event) {
        return decode(event.getPayload(), event.getEventId(), event.getProcessedAt());
    }

    private Map<String,Object> decode(String payload, String eventId, LocalDateTime recordedAt) {
        try {
            Map<String,Object> data = json.readValue(payload, new TypeReference<LinkedHashMap<String,Object>>() {});
            data.put("eventId", eventId);
            data.put("recordedAt", recordedAt.toString());
            return data;
        } catch (Exception ex) { throw new IllegalStateException("Invalid financial audit record", ex); }
    }
}
