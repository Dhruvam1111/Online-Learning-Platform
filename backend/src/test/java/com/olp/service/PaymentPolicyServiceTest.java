package com.olp.service;

import com.olp.entity.Course;
import com.olp.entity.User;
import com.olp.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentPolicyServiceTest {

    @Mock
    private PaymentAuditService audit;

    @Mock
    private WalletRepository locks;

    @Mock
    private FinancialGovernanceService governance;

    @InjectMocks
    private PaymentPolicyService policyService;

    private User admin() {
        User u = new User("Admin", "admin@olp.test", "hash", "admin");
        u.setId(1);
        return u;
    }

    private Course course(Integer id, String title, BigDecimal price) {
        Course c = new Course();
        c.setId(id);
        c.setTitle(title);
        c.setPrice(price);
        return c;
    }

    @Test
    void current_returnsDefaultRatesAndAppendsGovernanceCommissionTaxRate() {
        when(audit.list("financial.policy")).thenReturn(List.of());
        ReflectionTestUtils.setField(policyService, "governance", governance);
        when(governance.commissionTaxRate()).thenReturn(new BigDecimal("0.15"));

        Map<String, Object> result = policyService.current();
        assertEquals(new BigDecimal("0.20"), result.get("commissionRate"));
        assertEquals(new BigDecimal("0.18"), result.get("taxRate"));
        assertEquals(new BigDecimal("499.00"), result.get("defaultPrice"));
        assertEquals(new BigDecimal("0.15"), result.get("commissionTaxRate"));
    }

    @Test
    void update_validatesAdminRoleRateScaleAndPriceBounds() {
        User instructor = new User("Inst", "inst@olp.test", "hash", "instructor");
        instructor.setId(5);

        assertThrows(AccessDeniedException.class,
                () -> policyService.update(null, new BigDecimal("0.2"), new BigDecimal("0.18"), new BigDecimal("499")));
        assertThrows(AccessDeniedException.class,
                () -> policyService.update(instructor, new BigDecimal("0.2"), new BigDecimal("0.18"), new BigDecimal("499")));

        assertThrows(IllegalArgumentException.class,
                () -> policyService.update(admin(), new BigDecimal("1.01"), new BigDecimal("0.18"), new BigDecimal("499")));
        assertThrows(IllegalArgumentException.class,
                () -> policyService.update(admin(), new BigDecimal("0.2"), new BigDecimal("0.12345"), new BigDecimal("499")));
        assertThrows(IllegalArgumentException.class,
                () -> policyService.update(admin(), new BigDecimal("0.2"), new BigDecimal("0.18"), BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> policyService.update(admin(), new BigDecimal("0.2"), new BigDecimal("0.18"), new BigDecimal("1000001")));
        assertThrows(IllegalArgumentException.class,
                () -> policyService.update(admin(), new BigDecimal("0.2"), new BigDecimal("0.18"), new BigDecimal("499.999")));

        Map<String, Object> updated = policyService.update(
                admin(), new BigDecimal("0.25"), new BigDecimal("0.12"), new BigDecimal("799.00"));
        verify(locks).lockStudent(1);
        verify(audit).record(startsWith("policy-"), eq("financial.policy"), eq(updated));
        assertEquals(new BigDecimal("799.00"), updated.get("defaultPrice"));
    }

    @Test
    void courseOverridesAndSnapshots_resolvePrecedenceAndFallback() {
        when(audit.list("course.policy")).thenReturn(List.of(
                Map.of("courseId", 10, "price", "599.00", "taxRate", "0.10"),
                Map.of("courseId", 10, "price", "699.00", "taxRate", "0.05")
        ));
        when(audit.list("financial.policy")).thenReturn(List.of());

        assertEquals(Optional.of(new BigDecimal("699.00")), policyService.getAdminOverridePrice(10));
        assertEquals(Optional.empty(), policyService.getAdminOverridePrice(null));
        assertEquals(new BigDecimal("0.05"), policyService.getTaxRateForCourse(10));
        assertEquals(new BigDecimal("0.18"), policyService.getTaxRateForCourse(999));

        Course overridden = course(10, "Java", new BigDecimal("299.00"));
        assertEquals(new BigDecimal("699.00"), policyService.price(overridden));

        Course explicit = course(20, "SQL", new BigDecimal("350.00"));
        assertEquals(new BigDecimal("350.00"), policyService.price(explicit));

        Course unsetPrice = course(30, "General Course", new BigDecimal("-1"));
        assertEquals(new BigDecimal("499.00"), policyService.price(unsetPrice));
        assertEquals(new BigDecimal("499.00"), policyService.price(null));

        policyService.snapshot("ORD-1", overridden);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(audit).record(eq("policy-order-ORD-1"), eq("order.policy"), captor.capture());
        assertEquals("0.05", captor.getValue().get("taxRate").toString());

        when(audit.exists("policy-order-MISSING")).thenReturn(false);
        assertEquals(new BigDecimal("0.20"), policyService.forOrder("MISSING").get("commissionRate"));
    }
}
