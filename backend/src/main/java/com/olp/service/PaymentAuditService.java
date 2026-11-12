package com.olp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olp.entity.WebhookEvent;
import com.olp.repository.WebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

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
    private Map<String,Object> decode(WebhookEvent event) {
        try {
            Map<String,Object> data = json.readValue(event.getPayload(), new TypeReference<LinkedHashMap<String,Object>>() {});
            data.put("eventId", event.getEventId()); data.put("recordedAt", event.getProcessedAt().toString()); return data;
        } catch (Exception ex) { throw new IllegalStateException("Invalid financial audit record", ex); }
    }
}
