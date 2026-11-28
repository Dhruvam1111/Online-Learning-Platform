package com.olp.service;

import com.olp.entity.User;
import com.olp.entity.Wallet;
import com.olp.entity.WalletTransaction;
import com.olp.repository.WalletRepository;
import com.olp.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository transactionRepository;

    @Mock
    private PaymentAuditService audit;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(walletRepository, transactionRepository);
        ReflectionTestUtils.setField(walletService, "audit", audit);
    }

    private User student() {
        User u = new User("Student", "student@olp.test", "hash", "student");
        u.setId(21);
        return u;
    }

    @Test
    void getOrCreateAndBalance_requireAuthenticatedUserAndCreateZeroBalanceWalletIfAbsent() {
        assertThrows(AccessDeniedException.class, () -> walletService.getOrCreateWallet(null));
        assertThrows(AccessDeniedException.class, () -> walletService.getBalance(null));

        User s = student();
        when(walletRepository.findByStudentId(21)).thenReturn(Optional.empty());
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(BigDecimal.ZERO, walletService.getBalance(s));
        Wallet created = walletService.getOrCreateWallet(s);
        assertEquals(BigDecimal.ZERO, created.getBalance());
        verify(walletRepository, times(2)).lockStudent(21);
    }

    @Test
    void creditAndDebitWallet_updateBalanceCapsOverdraftAndIgnoreNonPositiveAmounts() {
        User s = student();
        Wallet wallet = new Wallet(s, new BigDecimal("100.00"));
        wallet.setId(5);
        when(walletRepository.findByStudentId(21)).thenReturn(Optional.of(wallet));

        walletService.creditWallet(s, BigDecimal.ZERO, "ignored");
        walletService.creditWallet(s, new BigDecimal("-5.00"), "ignored");
        assertEquals(BigDecimal.ZERO, walletService.debitWallet(s, BigDecimal.ZERO, "ignored"));
        verify(walletRepository, never()).save(any());

        walletService.creditWallet(s, new BigDecimal("50.00"), "Cashback");
        assertEquals(new BigDecimal("150.00"), wallet.getBalance());

        // Requesting debit > balance caps at available balance (150.00)
        BigDecimal deducted = walletService.debitWallet(s, new BigDecimal("250.00"), "Course purchase");
        assertEquals(new BigDecimal("150.00"), deducted);
        assertEquals(new BigDecimal("0.00"), wallet.getBalance());

        // Subsequent debit against zero balance returns ZERO without writing ledger row
        assertEquals(BigDecimal.ZERO, walletService.debitWallet(s, new BigDecimal("10.00"), "No funds"));
        verify(transactionRepository, times(2)).save(any(WalletTransaction.class));
    }

    @Test
    void topUp_enforcesStudentRoleBoundsRequestIdFormatAndIdempotencyConflicts() {
        User instructor = new User("Inst", "inst@olp.test", "hash", "instructor");
        instructor.setId(3);
        User s = student();

        assertThrows(AccessDeniedException.class,
                () -> walletService.topUp(null, new BigDecimal("100"), "req-1", "success"));
        assertThrows(AccessDeniedException.class,
                () -> walletService.topUp(instructor, new BigDecimal("100"), "req-1", "success"));

        assertThrows(IllegalArgumentException.class,
                () -> walletService.topUp(s, BigDecimal.ZERO, "req-1", "success"));
        assertThrows(IllegalArgumentException.class,
                () -> walletService.topUp(s, new BigDecimal("100000.01"), "req-1", "success"));
        assertThrows(IllegalArgumentException.class,
                () -> walletService.topUp(s, new BigDecimal("10.123"), "req-1", "success"));
        assertThrows(IllegalArgumentException.class,
                () -> walletService.topUp(s, new BigDecimal("100"), "bad id!", "success"));
        assertThrows(IllegalArgumentException.class,
                () -> walletService.topUp(s, new BigDecimal("100"), "req-1", "pending"));

        Wallet wallet = new Wallet(s, new BigDecimal("25.00"));
        when(walletRepository.findByStudentId(21)).thenReturn(Optional.of(wallet));

        // Idempotent conflict: same requestId but different amount or status
        when(audit.exists("topup-21-req-dup")).thenReturn(true);
        when(audit.get("topup-21-req-dup")).thenReturn(Map.of("amount", "100.00", "status", "success"));
        assertThrows(IllegalArgumentException.class,
                () -> walletService.topUp(s, new BigDecimal("200.00"), "req-dup", "success"));
        assertThrows(IllegalArgumentException.class,
                () -> walletService.topUp(s, new BigDecimal("100.00"), "req-dup", "failed"));

        // Exact replay returns previous without re-crediting
        Map<String, Object> replay = walletService.topUp(s, new BigDecimal("100.00"), "req-dup", "success");
        assertEquals("success", replay.get("status"));
        assertEquals(new BigDecimal("25.00"), wallet.getBalance());

        // Failed outcome does not credit wallet, but records audit event
        when(audit.exists("topup-21-req-fail")).thenReturn(false);
        when(audit.get("topup-21-req-fail")).thenReturn(Map.of("status", "failed", "balance", new BigDecimal("25.00")));
        walletService.topUp(s, new BigDecimal("50.00"), "req-fail", "failed");
        assertEquals(new BigDecimal("25.00"), wallet.getBalance());

        // Successful new top-up credits wallet and records audit
        when(audit.exists("topup-21-req-ok")).thenReturn(false);
        when(audit.get("topup-21-req-ok")).thenReturn(Map.of("status", "success", "balance", new BigDecimal("75.00")));
        walletService.topUp(s, new BigDecimal("50.00"), "req-ok", "success");
        assertEquals(new BigDecimal("75.00"), wallet.getBalance());
        verify(audit).record(eq("topup-21-req-ok"), eq("gateway.topup"), any());
    }

    @Test
    void getTransactionHistory_loadsTransactionsForStudentWallet() {
        User s = student();
        Wallet wallet = new Wallet(s, BigDecimal.ZERO);
        wallet.setId(88);
        when(walletRepository.findByStudentId(21)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByWalletIdOrderByCreatedAtDesc(88)).thenReturn(List.of());

        assertTrue(walletService.getTransactionHistory(s).isEmpty());
        verify(transactionRepository).findByWalletIdOrderByCreatedAtDesc(88);
    }
}
