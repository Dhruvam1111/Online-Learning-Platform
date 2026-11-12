package com.olp.service;

import com.fasterxml.jackson.databind.*;
import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

/** Opaque simulated provider tokens. Durable consent/revocation events contain no card data. */
@Service
public class SimulatedPaymentMethods {
    @org.springframework.beans.factory.annotation.Autowired(required=false) private PaymentGateway gateway;
    private final WebhookEventRepository events;
    private final PaymentRepository payments;
    private final WalletRepository locks;
    private final PaymentService settlement;
    private final ObjectMapper json=new ObjectMapper();
    public SimulatedPaymentMethods(WebhookEventRepository events,PaymentRepository payments,WalletRepository locks,PaymentService settlement) {
        this.events=events;this.payments=payments;this.locks=locks;this.settlement=settlement;
    }
    @Transactional
    public Map<String,Object> create(User user,String method,boolean consent) {
        requireUser(user);
        if (!consent) throw new IllegalArgumentException("Explicit consent is required to save a simulated payment method.");
        if (!Set.of("card","upi","netbanking","wallet").contains(method)) throw new IllegalArgumentException("Unsupported payment method.");
        String token=gateway==null?"simtok_"+UUID.randomUUID().toString().replace("-",""):gateway.createMethodToken(method);
        Map<String,Object> data=Map.of("token",token,"owner",user.getId(),"method",method,"consent",true,
            "createdAt",Instant.now().toString(),"expiresAt",Instant.now().plusSeconds(31536000).toString());
        events.save(new WebhookEvent(token,"method.created",encode(data))); return data;
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> list(User user) {
        requireUser(user);
        return events.findByEventType("method.created").stream().map(e -> decode(e.getPayload()))
            .filter(d -> ((Number)d.get("owner")).intValue()==user.getId())
            .filter(d -> !events.existsByEventId("revoked_"+d.get("token")))
            .filter(d -> Instant.parse((String)d.get("expiresAt")).isAfter(Instant.now())).toList();
    }
    @Transactional
    public void revoke(User user,String token) {
        requireUser(user); locks.lockStudent(user.getId()); validate(user,token);
        if (!events.existsByEventId("revoked_"+token)) events.save(new WebhookEvent("revoked_"+token,"method.revoked",encode(Map.of("owner",user.getId(),"token",token))));
    }
    /** A repeat/subscription renewal pays an existing order; retrying that order is idempotent. */
    @Transactional(noRollbackFor=com.olp.exception.PaymentOrderExpiredException.class)
    public PaymentTransactionResponse pay(User user,String token,String orderId) {
        requireUser(user);
        Payment p=payments.findByOrderIdForUpdate(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found."));
        if (!p.getStudent().getId().equals(user.getId())) throw new AccessDeniedException("This order belongs to another account.");
        locks.lockStudent(user.getId()); Map<String,Object> methodData = validate(user,token);
        if (events.existsByEventId("revoked_"+token)) throw new IllegalArgumentException("This saved method was revoked.");
        String savedMethod = String.valueOf(methodData.get("method"));
        String label = "card".equalsIgnoreCase(savedMethod) ? "Card" :
                       ("upi".equalsIgnoreCase(savedMethod) ? "UPI" :
                       ("netbanking".equalsIgnoreCase(savedMethod) ? "Net Banking" :
                       ("wallet".equalsIgnoreCase(savedMethod) ? "Wallet" : savedMethod)));
        return settlement.confirmPayment(orderId, label);
    }
    private Map<String,Object> validate(User user,String token) {
        if (token==null || !token.matches("simtok_[a-f0-9]{32}")) throw new IllegalArgumentException("Invalid saved method.");
        Map<String,Object> d=decode(events.findByEventId(token).filter(e -> e.getEventType().equals("method.created"))
            .orElseThrow(() -> new IllegalArgumentException("Saved method not found.")).getPayload());
        if (((Number)d.get("owner")).intValue()!=user.getId()) throw new AccessDeniedException("Saved method belongs to another account.");
        if (!Instant.parse((String)d.get("expiresAt")).isAfter(Instant.now())) throw new IllegalArgumentException("Saved method expired.");
        return d;
    }
    private void requireUser(User u) { if(u==null || !Set.of("student","admin").contains(u.getRole())) throw new AccessDeniedException("Sign in as a student."); }
    private String encode(Object value) { try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException(e);} }
    @SuppressWarnings("unchecked") private Map<String,Object> decode(String value) { try{return json.readValue(value,Map.class);}catch(Exception e){throw new IllegalStateException("Invalid saved method record.",e);} }
}
