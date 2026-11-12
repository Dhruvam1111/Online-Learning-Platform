package com.olp.controller;
import com.olp.entity.User;
import com.olp.service.PaymentSubscriptionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/payments/subscriptions")
public class PaymentSubscriptionController {
    private final PaymentSubscriptionService service;
    public PaymentSubscriptionController(PaymentSubscriptionService service){this.service=service;}
    public record Create(Integer courseId,String token,boolean consent){}
    public record Approval(boolean approval,java.math.BigDecimal expectedAmount,String requestId){}
    @GetMapping public List<Map<String,Object>> list(@AuthenticationPrincipal User user){return service.list(user);}
    @PostMapping public Map<String,Object> create(@AuthenticationPrincipal User user,@RequestBody Create request){return service.create(user,request.courseId(),request.token(),request.consent());}
    @DeleteMapping("/{id}") public Map<String,String> cancel(@AuthenticationPrincipal User user,@PathVariable String id){service.cancel(user,id);return Map.of("status","cancelled");}
    @GetMapping("/{id}/quote") public Map<String,Object> quote(@AuthenticationPrincipal User user,@PathVariable String id){return service.quote(user,id);}
    @PostMapping("/{id}/approve") public Map<String,Object> approve(@AuthenticationPrincipal User user,@PathVariable String id,@RequestBody Approval request){return service.approve(user,id,request.approval(),request.expectedAmount(),request.requestId());}
}
