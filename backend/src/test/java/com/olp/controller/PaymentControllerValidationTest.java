package com.olp.controller;

import com.olp.dto.PaymentSimulationDtos.*;
import com.olp.entity.User;
import com.olp.exception.GlobalExceptionHandler;
import com.olp.exception.InvalidCredentialsException;
import com.olp.repository.CourseRepository;
import com.olp.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerValidationTest {

    @Mock private CurrencyService currencyService;
    @Mock private WalletService walletService;
    @Mock private SimulatedPaymentMethods simulatedMethods;
    @Mock private BankAccountService bankAccountService;
    @Mock private PaymentPolicyService policyService;
    @Mock private CourseRepository courseRepository;
    @Mock private FinancialGovernanceService governanceService;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private PaymentEvidenceService evidenceService;
    @Mock private PaymentEventActions paymentActions;
    @Mock private PaymentWebhookService webhookService;
    @Mock private PaymentWebhookAuthenticator webhookAuth;
    @Mock private PaymentSubscriptionService subscriptionService;
    @Mock private ExceptionalRefundService exceptionalRefundService;

    private MockMvc mockMvc;
    private User student;
    private User admin;

    @BeforeEach
    void setUp() {
        student = new User("Sam", "sam@olp.com", "hash", "student", "active");
        student.setId(10);
        admin = new User("Admin", "admin@olp.com", "hash", "admin", "active");
        admin.setId(1);

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new CurrencyController(currencyService),
                        new WalletController(walletService, simulatedMethods),
                        new BankAccountController(bankAccountService),
                        new PaymentPolicyController(policyService, courseRepository),
                        new FinancialGovernanceController(governanceService, jdbcTemplate),
                        new PaymentEvidenceController(evidenceService),
                        new PaymentSimulationController(simulatedMethods, paymentActions, webhookService, webhookAuth),
                        new PaymentSubscriptionController(subscriptionService),
                        new ExceptionalRefundController(exceptionalRefundService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void currencyConvert_InvalidNumericAmountReturns400() throws Exception {
        mockMvc.perform(post("/api/payments/currencies/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"not-a-number\",\"targetCurrency\":\"USD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(currencyService);
    }

    @Test
    void currencyConvert_EmptyBodyUsesDefaults() throws Exception {
        when(currencyService.convertFromInr(BigDecimal.valueOf(499), "USD"))
                .thenReturn(Map.of("converted", 6));

        mockMvc.perform(post("/api/payments/currencies/convert"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.converted").value(6));
    }

    @Test
    void walletController_NullUserAndInvalidRewardAmountHandledCleanly() {
        WalletController wc = new WalletController(walletService, simulatedMethods);

        assertThrows(InvalidCredentialsException.class, () -> wc.getWallet(null));
        assertThrows(InvalidCredentialsException.class, () -> wc.addRewardCredit(null, Map.of("amount", 10)));
        assertThrows(IllegalArgumentException.class, () -> wc.addRewardCredit(student, Map.of("amount", "abc")));
        assertThrows(IllegalArgumentException.class, () -> wc.topUp(student, null));
    }

    @Test
    void financialGovernance_HandlesNullJdbcCountSafely() {
        FinancialGovernanceController fgc = new FinancialGovernanceController(governanceService, jdbcTemplate);
        when(jdbcTemplate.queryForList(anyString(), eq(10), eq(0L))).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(null);

        Map<String, Object> ledger = fgc.ledger(admin, 0, 10);
        assertEquals(0L, ledger.get("total"));
        assertEquals(0, Objects.requireNonNull((List<?>) ledger.get("records")).size());
        assertThrows(IllegalArgumentException.class, () -> fgc.update(admin, null));
    }

    @Test
    void relocatedDtosAndNullBodyGuardsWorkAcrossPaymentControllers() {
        BankAccountController bac = new BankAccountController(bankAccountService);
        assertThrows(IllegalArgumentException.class, () -> bac.save(admin, null));
        assertThrows(IllegalArgumentException.class, () -> bac.decide(admin, null));

        PaymentEvidenceController pec = new PaymentEvidenceController(evidenceService);
        assertThrows(IllegalArgumentException.class, () -> pec.accept(student, "ord-1", null));

        PaymentPolicyController ppc = new PaymentPolicyController(policyService, courseRepository);
        assertThrows(IllegalArgumentException.class, () -> ppc.course(admin, 1, null));
        assertThrows(IllegalArgumentException.class, () -> ppc.update(admin, null));

        PaymentSimulationController psc = new PaymentSimulationController(simulatedMethods, paymentActions, webhookService, webhookAuth);
        assertThrows(IllegalArgumentException.class, () -> psc.pay(student, "tok", null));

        PaymentSubscriptionController sub = new PaymentSubscriptionController(subscriptionService);
        assertThrows(IllegalArgumentException.class, () -> sub.create(student, null));
        assertThrows(IllegalArgumentException.class, () -> sub.approve(student, "sub-1", null));

        ExceptionalRefundController erc = new ExceptionalRefundController(exceptionalRefundService);
        assertThrows(IllegalArgumentException.class, () -> erc.decide(admin, "exception-1", null));
        assertThrows(IllegalArgumentException.class,
                () -> erc.decide(admin, "exception-1", Map.of("decision", "approved", "approvedAmount", "bad-decimal")));

        when(bankAccountService.save(eq(admin), any(), eq("Holder"), eq(true))).thenReturn(Map.of("saved", true));
        Map<String, Object> saved = bac.save(admin, new BankAccountRequest("1234567890", "HDFC0001234", "HDFC Bank", "Holder", true));
        assertEquals(true, saved.get("saved"));
    }

    @Test
    void currencyController_NullRateUpdateBodyReturns400() throws Exception {
        CurrencyController cc = new CurrencyController(currencyService);
        assertThrows(IllegalArgumentException.class, () -> cc.updateRate(admin, null));

        mockMvc.perform(get("/api/payments/policy"))
                .andExpect(status().isOk());
    }
}
