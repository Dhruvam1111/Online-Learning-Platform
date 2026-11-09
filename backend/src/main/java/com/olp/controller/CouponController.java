package com.olp.controller;

import com.olp.entity.Coupon;
import com.olp.entity.User;
import com.olp.service.CouponService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Controller exposing promotional discount coupon management and checkout validation (FR-A25).
 * Owned by Dhruvam (Pricing & Discount Engine).
 */
@RestController
@RequestMapping("/api/payments/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    /**
     * Pre-evaluates promotional code and returns simulated discount amount.
     */
    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyCoupon(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, Object> payload) {
        String code = (String) payload.get("code");
        BigDecimal amount = new BigDecimal(String.valueOf(payload.getOrDefault("orderAmount", 499)));

        Map<String, Object> discountDetails = couponService.validateAndApply(code, amount, currentUser);
        return ResponseEntity.ok(discountDetails);
    }

    /**
     * Lists all publicly active coupons for student checkout.
     */
    @GetMapping("/active")
    public ResponseEntity<List<Coupon>> getActiveCoupons() {
        List<Coupon> coupons = couponService.getActiveCoupons();
        return ResponseEntity.ok(coupons);
    }

    /**
     * Admin view: lists all campaigns and promo codes.
     */
    @GetMapping("/admin/all")
    public ResponseEntity<List<Coupon>> getAllCoupons(
            @AuthenticationPrincipal User currentUser) {
        List<Coupon> coupons = couponService.getAllCoupons(currentUser);
        return ResponseEntity.ok(coupons);
    }

    /**
     * Admin endpoint: creates a new promotional campaign.
     */
    @PostMapping("/create")
    public ResponseEntity<Coupon> createCoupon(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Coupon coupon) {
        Coupon created = couponService.createCoupon(currentUser, coupon);
        return ResponseEntity.ok(created);
    }

    /**
     * Admin endpoint: toggles coupon active/inactive status (US-A25).
     */
    @PatchMapping("/{couponId}/toggle")
    public ResponseEntity<Coupon> toggleCoupon(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Integer couponId) {
        Coupon updated = couponService.toggleCouponStatus(currentUser, couponId);
        return ResponseEntity.ok(updated);
    }
}
