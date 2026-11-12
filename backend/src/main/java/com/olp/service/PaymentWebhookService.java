package com.olp.service;

import com.fasterxml.jackson.databind.*;
import com.olp.entity.*;
import com.olp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

/** Authenticated simulator protocol. The event ledger and domain changes commit together. */
@Service
public class PaymentWebhookService {
    private final PaymentRepository payments;
    private final WebhookEventRepository events;
    private final PaymentService settlement;
    private final PaymentEventActions actions;
    private final PaymentWebhookAuthenticator signatures;
    private final ObjectMapper json=new ObjectMapper();
    private static final Set<String> TYPES=Set.of("payment.confirmed","payment.failed","payment.pending",
        "refund.pending","refund.processed","refund.failed","payout.processing","payout.successful",
        "payout.failed","payout.delayed","chargeback.opened","chargeback.resolved");
    public PaymentWebhookService(PaymentRepository payments, WebhookEventRepository events,
            PaymentService settlement, PaymentEventActions actions, PaymentWebhookAuthenticator signatures) {
        this.payments=payments; this.events=events; this.settlement=settlement; this.actions=actions; this.signatures=signatures;
    }
    @Transactional
    public boolean processWebhook(String rawPayload,String signatureHeader) {
        if (!signatures.verify(rawPayload,signatureHeader)) return false;
        JsonNode payload;
        try { payload=json.readTree(rawPayload); } catch (Exception ex) { return false; }
        if (payload==null || !payload.isObject()) return false;
        java.util.Iterator<String> fields=payload.fieldNames();
        while(fields.hasNext()) if (!Set.of("event","event_id","order_id","refund_id","earning_id","outcome","reason").contains(fields.next())) return false;
        String type=payload.path("event").asText(), id=payload.path("event_id").asText(), order=payload.path("order_id").asText();
        if (!TYPES.contains(type) || !id.matches("[A-Za-z0-9_-]{1,80}") || !order.matches("OLP-[A-Za-z0-9-]{1,60}")) return false;
        // Serialize all events for the same order, including distinct event IDs racing with checkout.
        Payment p=payments.findByOrderIdForUpdate(order).orElseThrow(() -> new IllegalArgumentException("Unknown payment order."));
        var old=events.findByEventId(id);
        if (old.isPresent()) return rawPayload.equals(old.get().getPayload());
        switch(type) {
            case "payment.confirmed" -> {
                // Polling applies timeout and releases reserved funds in this transaction.
                String status=(String)settlement.getOrderStatus(order).get("status");
                if ("created".equals(status)) settlement.confirmPayment(order);
                else if(Set.of("expired","failed","cancelled").contains(status))actions.reverseLateCapture(p);
            }
            case "payment.failed" -> settlement.failPayment(order);
            case "payment.pending" -> { /* Never downgrade an existing terminal state. */ }
            case "refund.pending", "refund.processed", "refund.failed" -> actions.refund(p,required(payload,"refund_id"),type.substring(7));
            case "payout.processing", "payout.successful", "payout.failed", "payout.delayed" -> {
                int earning=payload.path("earning_id").asInt(-1);
                if (earning<1) throw new IllegalArgumentException("earning_id is required.");
                actions.payout(p,earning,type.equals("payout.successful")?"paid":type.substring(7));
            }
            case "chargeback.opened", "chargeback.resolved" -> actions.dispute(p,type,payload.path("outcome").asText());
            default -> throw new IllegalArgumentException("Unsupported event.");
        }
        events.save(new WebhookEvent(id,type,rawPayload));
        return true;
    }
    private String required(JsonNode payload,String field) {
        String value=payload.path(field).asText();
        if (value.isBlank()) throw new IllegalArgumentException(field+" is required.");
        return value;
    }
}
