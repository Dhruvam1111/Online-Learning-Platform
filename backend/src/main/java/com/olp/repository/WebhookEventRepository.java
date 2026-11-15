package com.olp.repository;

import com.olp.entity.WebhookEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Integer> {

    @org.springframework.data.jpa.repository.Query("select e from WebhookEvent e where e.eventType=:eventType order by e.id")
    java.util.List<WebhookEvent> findByEventType(@org.springframework.data.repository.query.Param("eventType") String eventType);

    String REFUND_QUEUE_FROM = """
            FROM webhook_events r
            LEFT JOIN webhook_events d ON d.event_id = concat('decision-', r.event_id)
                AND d.event_type = 'refund.exception.decision'
            LEFT JOIN payments p ON p.order_id = substring(r.event_id, 11)
            LEFT JOIN users u ON u.id = p.student_id
            LEFT JOIN courses c ON c.id = p.course_id
            WHERE r.event_type = 'refund.exception.request'
            AND (:search = '' OR p.order_id = :search OR r.event_id = concat('exception-', :search)
                OR lower(u.email) = lower(:search))
            AND CASE :status
                WHEN 'pending' THEN d.event_id IS NULL
                WHEN 'history' THEN d.event_id IS NOT NULL
                ELSE jsonb_extract_path_text(CAST(d.payload AS jsonb), 'decision') = :status
            END
            """;

    @Query(value = """
            SELECT r.event_id AS "eventId", r.payload AS "requestPayload",
                r.processed_at AS "requestedAt", d.payload AS "decisionPayload",
                d.processed_at AS "decidedAt", u.name AS "studentName",
                u.email AS "studentEmail", c.title AS "courseTitle"
            """ + REFUND_QUEUE_FROM + " ORDER BY r.processed_at, r.id",
            countQuery = "SELECT count(*) " + REFUND_QUEUE_FROM, nativeQuery = true)
    Page<RefundQueueRow> findRefundQueue(@Param("status") String status, @Param("search") String search, Pageable page);

    @Query(value = """
            SELECT count(*) AS "pendingCount", min(r.processed_at) AS "oldestPendingAt"
            FROM webhook_events r
            WHERE r.event_type = 'refund.exception.request'
            AND NOT EXISTS (
                SELECT 1 FROM webhook_events d WHERE d.event_id = concat('decision-', r.event_id)
                AND d.event_type = 'refund.exception.decision'
            )
            """, nativeQuery = true)
    RefundQueueStats getRefundQueueStats();

    interface RefundQueueRow {
        String getEventId();
        String getRequestPayload();
        LocalDateTime getRequestedAt();
        String getDecisionPayload();
        LocalDateTime getDecidedAt();
        String getStudentName();
        String getStudentEmail();
        String getCourseTitle();
    }

    interface RefundQueueStats {
        long getPendingCount();
        LocalDateTime getOldestPendingAt();
    }

    boolean existsByEventId(String eventId);

    Optional<WebhookEvent> findByEventId(String eventId);
}
