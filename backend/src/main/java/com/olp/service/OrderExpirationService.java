package com.olp.service;

import com.olp.entity.Payment;
import com.olp.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service managing automatic session expiration and stale order cleanup.
 * Owned by Shubh (Order Lifecycle Engine).
 * Releases held discounts and returns reserved wallet funds when checkout times
 * out.
 */
@Service
public class OrderExpirationService {

    private static final Logger log = LoggerFactory.getLogger(OrderExpirationService.class);

    private final PaymentRepository paymentRepository;
    private final WalletService walletService;

    public OrderExpirationService(PaymentRepository paymentRepository,
            WalletService walletService) {
        this.paymentRepository = paymentRepository;
        this.walletService = walletService;
    }

    /**
     * Finds and expires uncompleted checkout orders older than 15 minutes.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "${payments.expiry-scan-ms:60000}")
    @Transactional
    public int expireStaleOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(15);
        List<String> staleOrders = paymentRepository.findExpiredOrderIds(threshold);
        int expiredCount = 0;
        for (String orderId : staleOrders) {
            Payment payment = paymentRepository.findByOrderIdForUpdate(orderId).orElse(null);
            if (payment == null || !"created".equalsIgnoreCase(payment.getStatus()))
                continue;
            payment.setStatus("expired");
            payment.setFailureReason("Checkout authorization session timed out ");

            // If student spent wallet credits on this order, refund credits back to wallet!
            if (payment.getWalletAmountUsed() != null && payment.getWalletAmountUsed().compareTo(BigDecimal.ZERO) > 0) {
                walletService.creditWallet(
                        payment.getStudent(),
                        payment.getWalletAmountUsed(),
                        "Auto-refund from expired checkout order " + payment.getOrderId());
                log.info("[Order Expiry] Restored ₹{} wallet credits to Student ID {}",
                        payment.getWalletAmountUsed(), payment.getStudent().getId());
            }

            payment.setWalletAmountUsed(BigDecimal.ZERO);
            paymentRepository.save(payment);
            expiredCount++;
            log.info("[Order Expiry] Expired stale Order {}", payment.getOrderId());
        }

        return expiredCount;
    }
}
