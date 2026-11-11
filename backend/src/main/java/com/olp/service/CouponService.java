package com.olp.service;

import com.olp.entity.Coupon;
import com.olp.entity.CouponUsage;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CouponRepository;
import com.olp.repository.CouponUsageRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Enterprise Promotional Discount & Coupon Service (FR-A25).
 * Owned by Dhruvam (Pricing & Discount Engine).
 */
@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository usageRepository;

    public CouponService(CouponRepository couponRepository,
                         CouponUsageRepository usageRepository) {
        this.couponRepository = couponRepository;
        this.usageRepository = usageRepository;
    }

    /**
     * Validates promotional coupon against business rules and computes discount.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> validateAndApply(String code, BigDecimal orderAmount, User student) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Promo coupon code is required.");
        }

        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid promo code: " + code.toUpperCase()));

        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            throw new IllegalArgumentException("This promo code is currently inactive.");
        }

        if (coupon.getValidUntil().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("This promo code has expired on " + coupon.getValidUntil().toLocalDate());
        }

        if (coupon.getUsageLimit() != null && coupon.getTimesUsed() >= coupon.getUsageLimit()) {
            throw new IllegalArgumentException("This promo coupon has reached its maximum redemption limit.");
        }

        if (coupon.getMinOrderAmount() != null && orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new IllegalArgumentException("Minimum order amount of ₹" + coupon.getMinOrderAmount() + " required to use this code.");
        }

        // Calculate discount percentage
        BigDecimal percentageMultiplier = coupon.getDiscountPercentage().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal discount = coupon.getFixedDiscountAmount() != null ? coupon.getFixedDiscountAmount() : orderAmount.multiply(percentageMultiplier).setScale(2, RoundingMode.HALF_UP);
        discount = discount.min(orderAmount);

        // Cap at max discount if configured
        if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
            discount = coupon.getMaxDiscountAmount();
        }

        BigDecimal finalPayable = orderAmount.subtract(discount).max(BigDecimal.ZERO);

        return Map.of(
                "couponCode", coupon.getCode(),
                "originalAmount", orderAmount,
                "discountAmount", discount,
                "finalPayableAmount", finalPayable,
                "discountPercentage", coupon.getDiscountPercentage(),
                "message", "Promo code applied! You saved ₹" + discount
        );
    }

    @Transactional
    public void recordRedemption(String code, BigDecimal discountApplied, String orderId, User student) {
        Coupon coupon = couponRepository.findByCodeForUpdate(code).orElseThrow(() -> new IllegalArgumentException("Coupon no longer exists."));
        if (!Boolean.TRUE.equals(coupon.getIsActive()) || !coupon.getValidUntil().isAfter(LocalDateTime.now()) || (coupon.getUsageLimit()!=null && coupon.getTimesUsed()>=coupon.getUsageLimit()))
            throw new IllegalArgumentException("Coupon expired, disabled or exhausted; restart checkout.");
        coupon.setTimesUsed(coupon.getTimesUsed()+1);
        couponRepository.save(coupon);
        usageRepository.save(new CouponUsage(coupon, student, orderId, discountApplied));
    }

    @Transactional
    public Coupon createCoupon(User admin, Coupon coupon) {
        if (admin == null || !"admin".equalsIgnoreCase(admin.getRole())) {
            throw new AccessDeniedException("Administrative privileges required to configure promo campaigns.");
        }
        if (coupon.getCode() == null || !coupon.getCode().matches("[A-Za-z0-9_-]{1,50}")) throw new IllegalArgumentException("Invalid coupon code.");
        coupon.setCode(coupon.getCode().toUpperCase(java.util.Locale.ROOT));
        if (coupon.getFixedDiscountAmount() != null) {
            if (coupon.getFixedDiscountAmount().signum() <= 0 || coupon.getFixedDiscountAmount().stripTrailingZeros().scale() > 2) throw new IllegalArgumentException("Fixed discount must be positive whole paisa.");
            coupon.setDiscountPercentage(BigDecimal.ZERO);
        } else if (coupon.getDiscountPercentage() == null || coupon.getDiscountPercentage().signum() <= 0 || coupon.getDiscountPercentage().compareTo(new BigDecimal("100")) > 0) throw new IllegalArgumentException("Percentage must be between 0 and 100.");
        if (coupon.getValidUntil() == null || !coupon.getValidUntil().isAfter(LocalDateTime.now()) || coupon.getUsageLimit() == null || coupon.getUsageLimit() < 1) throw new IllegalArgumentException("Future expiry and positive usage limit required.");
        if (coupon.getMinOrderAmount() != null && coupon.getMinOrderAmount().signum() < 0 || coupon.getMaxDiscountAmount() != null && coupon.getMaxDiscountAmount().signum() < 0) throw new IllegalArgumentException("Coupon limits cannot be negative.");
        coupon.setTimesUsed(0);
        if (couponRepository.existsByCodeIgnoreCase(coupon.getCode())) {
            throw new IllegalArgumentException("Coupon code already exists: " + coupon.getCode());
        }
        return couponRepository.save(coupon);
    }

    @Transactional(readOnly = true)
    public List<Coupon> getActiveCoupons() {
        return couponRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<Coupon> getAllCoupons(User admin) {
        if (admin == null || !"admin".equalsIgnoreCase(admin.getRole())) {
            throw new AccessDeniedException("Administrative privileges required to view all promotional campaigns.");
        }
        return couponRepository.findAll();
    }

    @Transactional
    public Coupon toggleCouponStatus(User admin, Integer couponId) {
        if (admin == null || !"admin".equalsIgnoreCase(admin.getRole())) {
            throw new AccessDeniedException("Administrative privileges required to manage coupons.");
        }
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + couponId));
        coupon.setIsActive(!Boolean.TRUE.equals(coupon.getIsActive()));
        return couponRepository.save(coupon);
    }
}
