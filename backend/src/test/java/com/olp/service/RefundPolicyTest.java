package com.olp.service;

import com.olp.entity.Payment;
import com.olp.entity.Refund;
import com.olp.repository.RefundRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundPolicyTest {

    @Mock
    private RefundRepository refundRepository;

    private Payment payment(BigDecimal gatewayAmount, BigDecimal walletAmount, LocalDateTime createdAt) {
        Payment p = new Payment();
        p.setId(11);
        p.setAmount(gatewayAmount);
        p.setWalletAmountUsed(walletAmount);
        p.setCreatedAt(createdAt);
        return p;
    }

    @Test
    void withinWindow_acceptsWithin30DaysAndRejectsExpiredOrNullCreatedAt() {
        assertDoesNotThrow(() -> RefundPolicy.withinWindow(
                payment(new BigDecimal("500.00"), BigDecimal.ZERO, LocalDateTime.now().minusDays(29))));

        assertThrows(IllegalArgumentException.class, () -> RefundPolicy.withinWindow(
                payment(new BigDecimal("500.00"), BigDecimal.ZERO, LocalDateTime.now().minusDays(31))));

        assertThrows(IllegalArgumentException.class, () -> RefundPolicy.withinWindow(
                payment(new BigDecimal("500.00"), BigDecimal.ZERO, null)));
    }

    @Test
    void amount_defaultsToTotalPaidAndRejectsNonPositiveExcessOrSubPaisaScale() {
        Payment split = payment(new BigDecimal("300.00"), new BigDecimal("200.00"), LocalDateTime.now());

        assertEquals(new BigDecimal("500.00"), RefundPolicy.amount(split, null));
        assertEquals(new BigDecimal("250.50"), RefundPolicy.amount(split, new BigDecimal("250.50")));

        assertThrows(IllegalArgumentException.class, () -> RefundPolicy.amount(split, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> RefundPolicy.amount(split, new BigDecimal("-10")));
        assertThrows(IllegalArgumentException.class, () -> RefundPolicy.amount(split, new BigDecimal("500.01")));
        assertThrows(IllegalArgumentException.class, () -> RefundPolicy.amount(split, new BigDecimal("10.123")));
    }

    @Test
    void walletShare_allocatesProportionallyAndNeverExceedsWalletUsed() {
        Payment split = payment(new BigDecimal("300.00"), new BigDecimal("200.00"), LocalDateTime.now());

        assertEquals(new BigDecimal("100.00"), RefundPolicy.walletShare(split, new BigDecimal("250.00")));
        assertEquals(new BigDecimal("200.00"), RefundPolicy.walletShare(split, new BigDecimal("500.00")));

        Payment gatewayOnly = payment(new BigDecimal("500.00"), null, LocalDateTime.now());
        assertEquals(BigDecimal.ZERO, RefundPolicy.wallet(gatewayOnly));
        assertEquals(new BigDecimal("0.00"), RefundPolicy.walletShare(gatewayOnly, new BigDecimal("250.00")));
    }

    @Test
    void hasActiveRefund_returnsFalseOnlyWhenAbsentOrAllFailed() {
        when(refundRepository.existsByPaymentId(11)).thenReturn(false);
        assertFalse(RefundPolicy.hasActiveRefund(refundRepository, 11));

        when(refundRepository.existsByPaymentId(11)).thenReturn(true);
        Refund failed = new Refund();
        failed.setStatus("failed");
        when(refundRepository.findByPaymentIdOrderByCreatedAtDesc(11)).thenReturn(List.of(failed));
        assertFalse(RefundPolicy.hasActiveRefund(refundRepository, 11));

        Refund processed = new Refund();
        processed.setStatus("processed");
        when(refundRepository.findByPaymentIdOrderByCreatedAtDesc(11)).thenReturn(List.of(failed, processed));
        assertTrue(RefundPolicy.hasActiveRefund(refundRepository, 11));
    }

    @Test
    void blocksPayout_blocksPendingAndFullRefundsButAllowsPartialRefunds() {
        Payment p = payment(new BigDecimal("400.00"), new BigDecimal("100.00"), LocalDateTime.now());

        p.setRefundStatus("none");
        assertFalse(RefundPolicy.blocksPayout(p));

        p.setRefundStatus("pending");
        assertTrue(RefundPolicy.blocksPayout(p));

        p.setRefundStatus("refunded");
        p.setRefundAmount(new BigDecimal("200.00"));
        assertFalse(RefundPolicy.blocksPayout(p));

        p.setRefundAmount(new BigDecimal("500.00"));
        assertTrue(RefundPolicy.blocksPayout(p));

        p.setRefundAmount(null);
        assertTrue(RefundPolicy.blocksPayout(p));
    }

    @Test
    void reason_trimsAndRejectsNullBlankAndOver1000Chars() {
        assertEquals("Course not as expected", RefundPolicy.reason("  Course not as expected  "));
        assertThrows(IllegalArgumentException.class, () -> RefundPolicy.reason(null));
        assertThrows(IllegalArgumentException.class, () -> RefundPolicy.reason("   "));
        assertThrows(IllegalArgumentException.class, () -> RefundPolicy.reason("x".repeat(1001)));
        assertEquals(1000, RefundPolicy.reason("x".repeat(1000)).length());
    }
}
