package com.olp.repository;

import com.olp.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Integer> {

    java.util.List<WebhookEvent> findByEventType(String eventType);

    boolean existsByEventId(String eventId);

    Optional<WebhookEvent> findByEventId(String eventId);
}
