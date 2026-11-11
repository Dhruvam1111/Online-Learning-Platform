package com.olp.controller;
import com.olp.entity.User;
import com.olp.service.PaymentEvidenceService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/payments")
public class PaymentEvidenceController {
    private final PaymentEvidenceService evidence;
    public PaymentEvidenceController(PaymentEvidenceService evidence){this.evidence=evidence;}
    public record Acceptance(String policyVersion){}
    @PostMapping("/orders/{id}/policy-acceptance") public Map<String,Object> accept(@AuthenticationPrincipal User user,@PathVariable String id,@RequestBody Acceptance request){evidence.accept(user,id,request.policyVersion());return Map.of("accepted",true);}
    @GetMapping("/disputes/{id}/evidence") public Map<String,Object> evidence(@AuthenticationPrincipal User user,@PathVariable String id){return evidence.evidence(user,id);}
}
