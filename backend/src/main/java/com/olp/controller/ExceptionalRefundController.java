package com.olp.controller;
import com.olp.dto.RefundRequest;
import com.olp.entity.User;
import com.olp.service.ExceptionalRefundService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.*;
@RestController
@RequestMapping("/api/payments/refunds/exceptions")
public class ExceptionalRefundController {
    private final ExceptionalRefundService service;
    public ExceptionalRefundController(ExceptionalRefundService service) { this.service=service; }
    @PostMapping public Map<String,Object> request(@AuthenticationPrincipal User user,@Valid @RequestBody RefundRequest request) { return service.request(user,request); }
    @GetMapping public List<Map<String,Object>> list(@AuthenticationPrincipal User user) { return service.list(user); }
    @PostMapping("/{id}/decision") public Map<String,Object> decide(@AuthenticationPrincipal User user,@PathVariable String id,@RequestBody Map<String,String> body) { return service.decide(user,id,body.get("decision"),body.get("reason")); }
}
