package com.olp.repository;

import com.olp.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Integer> {

    @org.springframework.data.jpa.repository.Query("select e from WebhookEvent e where e.eventType=:eventType order by e.id")
    java.util.List<WebhookEvent> findByEventType(@org.springframework.data.repository.query.Param("eventType") String eventType);

    boolean existsByEventId(String eventId);

    Optional<WebhookEvent> findByEventId(String eventId);
}
