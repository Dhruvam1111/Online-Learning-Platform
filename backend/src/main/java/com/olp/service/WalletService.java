package com.olp.service;

import com.olp.entity.User;
import com.olp.entity.Wallet;
import com.olp.entity.WalletTransaction;
import com.olp.repository.WalletRepository;
import com.olp.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service managing student virtual wallet credits and split-tender payments (FR-PG02).
 * Owned by Shubh (Wallet Engine).
 */
@Service
public class WalletService {

    @org.springframework.beans.factory.annotation.Autowired private PaymentAuditService audit;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    public WalletService(WalletRepository walletRepository,
                         WalletTransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Wallet getOrCreateWallet(User student) {
        if (student == null) throw new org.springframework.security.access.AccessDeniedException("Sign in to access the wallet.");
        walletRepository.lockStudent(student.getId());
        return walletRepository.findByStudentId(student.getId())
                .orElseGet(() -> walletRepository.save(new Wallet(student, BigDecimal.ZERO)));
    }

    @Transactional
    public BigDecimal getBalance(User student) {
        if (student == null) throw new org.springframework.security.access.AccessDeniedException("Sign in to access the wallet.");
        walletRepository.lockStudent(student.getId());
        return walletRepository.findByStudentId(student.getId())
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional
    public void creditWallet(User student, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;

        Wallet wallet = getOrCreateWallet(student);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction(wallet, amount, "CREDIT", description);
        transactionRepository.save(tx);
    }

    @Transactional
    public BigDecimal debitWallet(User student, BigDecimal maxAmountToDeduct, String description) {
        if (maxAmountToDeduct == null || maxAmountToDeduct.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        Wallet wallet = getOrCreateWallet(student);
        BigDecimal currentBalance = wallet.getBalance();

        if (currentBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal toDeduct = currentBalance.min(maxAmountToDeduct);
        wallet.setBalance(currentBalance.subtract(toDeduct));
        walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction(wallet, toDeduct, "DEBIT", description);
        transactionRepository.save(tx);

        return toDeduct;
    }

    @Transactional
    public List<WalletTransaction> getTransactionHistory(User student) {
        Wallet wallet = getOrCreateWallet(student);
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
    }
    @Transactional
    public java.util.Map<String,Object> topUp(User user, BigDecimal amount, String requestId, String outcome) {
        if (user == null || !"student".equalsIgnoreCase(user.getRole())) throw new org.springframework.security.access.AccessDeniedException("Student account required.");
        if (amount == null || amount.signum() <= 0 || amount.compareTo(new BigDecimal("100000")) > 0 || amount.stripTrailingZeros().scale() > 2) throw new IllegalArgumentException("Top-up must be ₹0.01–₹100,000 in whole paisa.");
        if (requestId == null || !requestId.matches("[A-Za-z0-9_-]{1,60}") || outcome == null || !java.util.Set.of("success","failed","cancelled").contains(outcome)) throw new IllegalArgumentException("Valid request ID and simulation outcome required.");
        Wallet wallet = getOrCreateWallet(user);
        String key="topup-"+user.getId()+"-"+requestId;
        if(audit.exists(key)) {
            var previous=audit.get(key);
            if(new BigDecimal(previous.get("amount").toString()).compareTo(amount)!=0 || !outcome.equals(previous.get("status"))) throw new IllegalArgumentException("Request ID was already used with different top-up details.");
            return previous;
        }
        if("success".equals(outcome)) creditWallet(user,amount,"Simulated top-up "+requestId);
        audit.record(key,"gateway.topup",java.util.Map.of("studentId",user.getId(),"amount",amount,"status",outcome,"balance",wallet.getBalance(),"simulation",true));
        return audit.get(key);
    }

}
