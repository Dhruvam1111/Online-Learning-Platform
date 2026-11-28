package com.olp.service;

import com.olp.entity.User;
import com.olp.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialGovernanceServiceTest {

    @Mock
    private PaymentAuditService audit;

    @Mock
    private WalletRepository locks;

    @InjectMocks
    private FinancialGovernanceService governanceService;

    private User admin() {
        User u = new User("Admin", "admin@olp.test", "hash", "admin");
        u.setId(1);
        return u;
    }

    @Test
    void policy_returnsDefaultsWhenNoAuditHistoryExists() {
        when(audit.list("finance.governance")).thenReturn(List.of());

        Map<String, Object> policy = governanceService.policy();
        assertEquals(2557, policy.get("financialRetentionDays"));
        assertEquals(365, policy.get("adminAuditRetentionDays"));
        assertEquals(BigDecimal.ZERO, governanceService.withholdingRate());
        assertEquals(new BigDecimal("0.18"), governanceService.commissionTaxRate());
    }

    @Test
    void policy_returnsLatestAuditRecordWhenPresent() {
        when(audit.list("finance.governance")).thenReturn(List.of(
                Map.of("financialRetentionDays", 2557, "withholdingRate", "0.05", "commissionTaxRate", "0.18"),
                Map.of("financialRetentionDays", 3000, "withholdingRate", "0.10", "commissionTaxRate", "0.12")
        ));

        assertEquals(3000, governanceService.policy().get("financialRetentionDays"));
        assertEquals(new BigDecimal("0.10"), governanceService.withholdingRate());
        assertEquals(new BigDecimal("0.12"), governanceService.commissionTaxRate());
    }

    @Test
    void update_enforcesAdminRoleRetentionBoundsAndRatePrecision() {
        User student = new User("Student", "s@olp.test", "hash", "student");
        student.setId(9);

        assertThrows(AccessDeniedException.class,
                () -> governanceService.update(null, 2557, 365, new BigDecimal("0.10")));
        assertThrows(AccessDeniedException.class,
                () -> governanceService.update(student, 2557, 365, new BigDecimal("0.10")));

        assertThrows(IllegalArgumentException.class,
                () -> governanceService.update(admin(), 2556, 365, new BigDecimal("0.10"), new BigDecimal("0.18")));
        assertThrows(IllegalArgumentException.class,
                () -> governanceService.update(admin(), 2557, 364, new BigDecimal("0.10"), new BigDecimal("0.18")));
        assertThrows(IllegalArgumentException.class,
                () -> governanceService.update(admin(), 2557, 365, new BigDecimal("0.12345"), new BigDecimal("0.18")));
        assertThrows(IllegalArgumentException.class,
                () -> governanceService.update(admin(), 2557, 365, new BigDecimal("0.10"), new BigDecimal("1.01")));

        when(audit.list("finance.governance")).thenReturn(List.of());
        Map<String, Object> saved = governanceService.update(admin(), 3000, 730, new BigDecimal("0.0500"));

        verify(locks).lockStudent(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(audit).record(startsWith("finance-policy-"), eq("finance.governance"), captor.capture());
        assertEquals(3000, saved.get("financialRetentionDays"));
        assertEquals(new BigDecimal("0.18"), saved.get("commissionTaxRate"));
        assertEquals("simulation", captor.getValue().get("mode"));
    }
}
