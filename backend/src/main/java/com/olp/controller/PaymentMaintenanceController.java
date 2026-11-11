package com.olp.controller;
import com.olp.entity.User;
import com.olp.service.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/payments/maintenance")
public class PaymentMaintenanceController {
    private final PaymentMaintenanceService maintenance;
    private final PaymentSubscriptionService subscriptions;
    private final OrderExpirationService expiry;
    private final RoyaltyRefundService royalties;
    public PaymentMaintenanceController(PaymentMaintenanceService maintenance,PaymentSubscriptionService subscriptions,OrderExpirationService expiry,RoyaltyRefundService royalties){this.maintenance=maintenance;this.subscriptions=subscriptions;this.expiry=expiry;this.royalties=royalties;}
    @PostMapping("/run") public Map<String,Object> run(@AuthenticationPrincipal User user){
        if(user==null || !"admin".equals(user.getRole()))throw new AccessDeniedException("Administrator required.");
        int count=expiry.expireStaleOrders();int repaired=royalties.repairProcessedRefunds();subscriptions.prepareNotices();maintenance.reconcileDaily();
        return Map.of("expiredOrders",count,"royaltyAdjustments",repaired,"status","completed","mode","simulation");
    }
}
