package com.olp.service;

import com.olp.entity.Coupon;
import com.olp.entity.CouponUsage;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CouponRepository;
import com.olp.repository.CouponUsageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponUsageRepository usageRepository;

    @InjectMocks
    private CouponService couponService;

    private User admin() {
        User u = new User("Admin", "admin@olp.test", "hash", "admin");
        u.setId(1);
        return u;
    }

    private User student() {
        User u = new User("Student", "student@olp.test", "hash", "student");
        u.setId(10);
        return u;
    }

    private Coupon activePercentCoupon() {
        Coupon c = new Coupon("SAVE20", new BigDecimal("20.00"), new BigDecimal("150.00"),
                new BigDecimal("200.00"), LocalDateTime.now().plusDays(10), 5);
        c.setId(3);
        c.setTimesUsed(1);
        return c;
    }

    @Test
    void validateAndApply_computesPercentageDiscountWithCapAndMinimumFloor() {
        Coupon coupon = activePercentCoupon();
        when(couponRepository.findByCodeIgnoreCase("save20")).thenReturn(Optional.of(coupon));
        when(couponRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(coupon));

        // 20% of 500 = 100 (below max 150)
        Map<String, Object> uncapped = couponService.validateAndApply("  save20 ", new BigDecimal("500.00"), student());
        assertEquals("SAVE20", uncapped.get("couponCode"));
        assertEquals(new BigDecimal("100.00"), uncapped.get("discountAmount"));
        assertEquals(new BigDecimal("400.00"), uncapped.get("finalPayableAmount"));

        // 20% of 1000 = 200 -> capped at 150.00
        Map<String, Object> capped = couponService.validateAndApply("SAVE20", new BigDecimal("1000.00"), student());
        assertEquals(new BigDecimal("150.00"), capped.get("discountAmount"));
        assertEquals(new BigDecimal("850.00"), capped.get("finalPayableAmount"));
    }

    @Test
    void validateAndApply_supportsFixedDiscountCappedAtOrderAmount() {
        Coupon fixed = new Coupon("FLAT300", BigDecimal.ZERO, null,
                new BigDecimal("100.00"), LocalDateTime.now().plusDays(5), 10);
        fixed.setFixedDiscountAmount(new BigDecimal("300.00"));
        when(couponRepository.findByCodeIgnoreCase("FLAT300")).thenReturn(Optional.of(fixed));

        Map<String, Object> result = couponService.validateAndApply("FLAT300", new BigDecimal("250.00"), student());
        assertEquals(new BigDecimal("250.00"), result.get("discountAmount"));
        assertEquals(BigDecimal.ZERO.setScale(2), ((BigDecimal) result.get("finalPayableAmount")).setScale(2));
    }

    @Test
    void validateAndApply_rejectsBlankMissingInactiveExpiredExhaustedAndBelowMinimum() {
        assertThrows(IllegalArgumentException.class,
                () -> couponService.validateAndApply("  ", new BigDecimal("500.00"), student()));
        assertThrows(IllegalArgumentException.class,
                () -> couponService.validateAndApply(null, new BigDecimal("500.00"), student()));

        when(couponRepository.findByCodeIgnoreCase("NONE")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> couponService.validateAndApply("NONE", new BigDecimal("500.00"), student()));

        Coupon inactive = activePercentCoupon();
        inactive.setIsActive(false);
        when(couponRepository.findByCodeIgnoreCase("INACT")).thenReturn(Optional.of(inactive));
        assertThrows(IllegalArgumentException.class,
                () -> couponService.validateAndApply("INACT", new BigDecimal("500.00"), student()));

        Coupon expired = activePercentCoupon();
        expired.setValidUntil(LocalDateTime.now().minusMinutes(1));
        when(couponRepository.findByCodeIgnoreCase("OLD")).thenReturn(Optional.of(expired));
        assertThrows(IllegalArgumentException.class,
                () -> couponService.validateAndApply("OLD", new BigDecimal("500.00"), student()));

        Coupon exhausted = activePercentCoupon();
        exhausted.setTimesUsed(5);
        when(couponRepository.findByCodeIgnoreCase("FULL")).thenReturn(Optional.of(exhausted));
        assertThrows(IllegalArgumentException.class,
                () -> couponService.validateAndApply("FULL", new BigDecimal("500.00"), student()));

        Coupon minOrder = activePercentCoupon();
        when(couponRepository.findByCodeIgnoreCase("MIN")).thenReturn(Optional.of(minOrder));
        assertThrows(IllegalArgumentException.class,
                () -> couponService.validateAndApply("MIN", new BigDecimal("199.99"), student()));
    }

    @Test
    void recordRedemption_incrementsUsageAndHandlesExhaustedRaceCondition() {
        Coupon coupon = activePercentCoupon();
        coupon.setTimesUsed(4);
        coupon.setUsageLimit(5);
        when(couponRepository.findByCodeForUpdate("SAVE20")).thenReturn(Optional.of(coupon));

        couponService.recordRedemption("SAVE20", new BigDecimal("100.00"), "ORD-1", student());

        assertEquals(5, coupon.getTimesUsed());
        verify(couponRepository).save(coupon);
        ArgumentCaptor<CouponUsage> usageCaptor = ArgumentCaptor.forClass(CouponUsage.class);
        verify(usageRepository).save(usageCaptor.capture());
        assertEquals("ORD-1", usageCaptor.getValue().getOrderId());
        assertEquals(new BigDecimal("100.00"), usageCaptor.getValue().getDiscountApplied());

        // Next locked redemption fails because limit reached
        assertThrows(IllegalArgumentException.class,
                () -> couponService.recordRedemption("SAVE20", new BigDecimal("100.00"), "ORD-2", student()));
    }

    @Test
    void createCoupon_validatesAdminRoleFieldsAndDuplicates() {
        Coupon draft = activePercentCoupon();
        draft.setCode("welcome_10");

        assertThrows(AccessDeniedException.class, () -> couponService.createCoupon(null, draft));
        assertThrows(AccessDeniedException.class, () -> couponService.createCoupon(student(), draft));

        Coupon badCode = activePercentCoupon();
        badCode.setCode("bad code!");
        assertThrows(IllegalArgumentException.class, () -> couponService.createCoupon(admin(), badCode));

        Coupon badFixed = activePercentCoupon();
        badFixed.setFixedDiscountAmount(new BigDecimal("10.123"));
        assertThrows(IllegalArgumentException.class, () -> couponService.createCoupon(admin(), badFixed));

        Coupon badPercent = activePercentCoupon();
        badPercent.setDiscountPercentage(new BigDecimal("101"));
        assertThrows(IllegalArgumentException.class, () -> couponService.createCoupon(admin(), badPercent));

        Coupon pastExpiry = activePercentCoupon();
        pastExpiry.setValidUntil(LocalDateTime.now().minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> couponService.createCoupon(admin(), pastExpiry));

        Coupon negativeLimit = activePercentCoupon();
        negativeLimit.setMinOrderAmount(new BigDecimal("-1"));
        assertThrows(IllegalArgumentException.class, () -> couponService.createCoupon(admin(), negativeLimit));

        when(couponRepository.existsByCodeIgnoreCase("WELCOME_10")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> couponService.createCoupon(admin(), draft));

        when(couponRepository.existsByCodeIgnoreCase("WELCOME_10")).thenReturn(false);
        when(couponRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Coupon saved = couponService.createCoupon(admin(), draft);
        assertEquals("WELCOME_10", saved.getCode());
        assertEquals(0, saved.getTimesUsed());
    }

    @Test
    void toggleCouponStatusAndGetAllCoupons_enforceAdminAndUpdateState() {
        assertThrows(AccessDeniedException.class, () -> couponService.getAllCoupons(student()));
        assertThrows(AccessDeniedException.class, () -> couponService.toggleCouponStatus(student(), 1));

        Coupon coupon = activePercentCoupon();
        when(couponRepository.findById(3)).thenReturn(Optional.of(coupon));
        when(couponRepository.save(coupon)).thenReturn(coupon);
        when(couponRepository.findById(99)).thenReturn(Optional.empty());
        when(couponRepository.findAll()).thenReturn(List.of(coupon));

        assertEquals(1, couponService.getAllCoupons(admin()).size());
        assertFalse(couponService.toggleCouponStatus(admin(), 3).getIsActive());
        assertTrue(couponService.toggleCouponStatus(admin(), 3).getIsActive());
        assertThrows(ResourceNotFoundException.class, () -> couponService.toggleCouponStatus(admin(), 99));
    }
}
