package com.olp.service;

import com.olp.dto.InstructorEarningsResponse;
import com.olp.entity.InstructorEarning;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.InstructorEarningRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing instructor course royalties, commission accounting, and payouts (FR-PG07, FR-PG08, FR-A25, FR-A26).
 * Owned by Dhruvam (Instructor Royalty & Payout Auditor).
 */
@Service
public class InstructorPayoutService {

    @org.springframework.beans.factory.annotation.Autowired private PaymentAuditService audit;
    @org.springframework.beans.factory.annotation.Autowired(required=false) private BankAccountService accounts;
    @org.springframework.beans.factory.annotation.Autowired(required=false) private FinancialGovernanceService governance;
    @org.springframework.beans.factory.annotation.Autowired(required=false) private PaymentGateway gateway;
    @org.springframework.beans.factory.annotation.Autowired(required=false) private RoyaltyRefundService royalties;
    private final InstructorEarningRepository earningRepository;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private boolean payable(InstructorEarning earning) {
        if (entityManager != null) {
            entityManager.find(com.olp.entity.Payment.class, earning.getPayment().getId(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            entityManager.refresh(earning);
            entityManager.refresh(earning.getPayment());
        }
        return eligibleForPayout(earning);
    }

    private boolean eligibleForPayout(InstructorEarning earning) {
        return "success".equals(earning.getPayment().getStatus())
            && !RefundPolicy.blocksPayout(earning.getPayment())
            && java.util.Set.of("unpaid", "processing", "failed", "delayed").contains(earning.getPayoutStatus());
    }

    public InstructorPayoutService(InstructorEarningRepository earningRepository) {
        this.earningRepository = earningRepository;
    }

    /**
     * Retrieves full revenue report and sales ledger for the authenticated instructor.
     */
    @Transactional(readOnly = true)
    public InstructorEarningsResponse getEarningsReport(User instructor) {
        if (instructor == null || (!"instructor".equalsIgnoreCase(instructor.getRole()) && !"admin".equalsIgnoreCase(instructor.getRole()))) {
            throw new AccessDeniedException("Access denied: Instructor or Administrative role required.");
        }

        List<InstructorEarning> earnings = ("instructor".equalsIgnoreCase(instructor.getRole()))
                ? earningRepository.findByInstructorIdOrderByCreatedAtDesc(instructor.getId())
                : earningRepository.findAll();

        BigDecimal lifetimeGross = earnings.stream()
                .map(InstructorEarning::getGrossAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal lifetimeCommission = earnings.stream()
                .map(InstructorEarning::getPlatformCommission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netTakeHome = earnings.stream()
                .map(InstructorEarning::getNetInstructorEarning)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal unpaidBalance = earnings.stream()
                .filter(this::eligibleForPayout)
                .filter(e -> "unpaid".equalsIgnoreCase(e.getPayoutStatus()))
                .map(InstructorEarning::getNetInstructorEarning)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<InstructorEarningsResponse.SaleRecord> sales = earnings.stream()
                .map(e -> new InstructorEarningsResponse.SaleRecord(
                        e.getId(),
                        e.getCourse().getId(),
                        e.getCourse().getTitle(),
                        e.getPayment().getOrderId(),
                        e.getGrossAmount(),
                        e.getPlatformCommission(),
                        e.getNetInstructorEarning(),
                        e.getPayoutStatus(),
                        e.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return new InstructorEarningsResponse(
                instructor != null ? instructor.getId() : 1,
                instructor != null ? instructor.getName() : "Instructor",
                lifetimeGross,
                lifetimeCommission,
                netTakeHome,
                unpaidBalance,
                sales
        );
    }

    /**
     * Admin view of all instructor earnings and payouts across the platform.
     */
    @Transactional(readOnly = true)
    public List<InstructorEarningsResponse.SaleRecord> getAllEarningsForAdmin(User admin) {
        if (admin == null || !"admin".equalsIgnoreCase(admin.getRole())) {
            throw new AccessDeniedException("Administrative privileges required.");
        }
        return earningRepository.findAll().stream()
                .map(e -> new InstructorEarningsResponse.SaleRecord(
                        e.getId(),
                        e.getCourse().getId(),
                        e.getCourse().getTitle(),
                        e.getPayment().getOrderId(),
                        e.getGrossAmount(),
                        e.getPlatformCommission(),
                        e.getNetInstructorEarning(),
                        e.getPayoutStatus(),
                        e.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Admin action to settle instructor payouts (FR-A26).
     */
    @Transactional
    public void settleEarningPayout(User admin, Integer earningId) {
        if (admin == null || !"admin".equalsIgnoreCase(admin.getRole())) {
            throw new AccessDeniedException("Administrative privileges required to settle payouts.");
        }

        InstructorEarning earning = earningRepository.findById(earningId)
                .orElseThrow(() -> new ResourceNotFoundException("Earning record not found: " + earningId));

        if ("paid".equals(earning.getPayoutStatus())) return;
        if (!payable(earning)) throw new IllegalStateException("This payout is not eligible for settlement.");
        if(accounts!=null && gateway!=null){settleEligible(earning.getInstructor(),new java.util.ArrayList<>(java.util.List.of(earning)),null);return;}
        earning.setPayoutStatus("paid");
        earningRepository.save(earning);
    }

    /**
     * Admin action to halt an instructor payout in case of disputes or policy reviews (US-A26).
     */
    @Transactional
    public void haltEarningPayout(User admin, Integer earningId, String reason) {
        if (admin == null || !"admin".equalsIgnoreCase(admin.getRole())) {
            throw new AccessDeniedException("Administrative privileges required to halt payouts.");
        }

        InstructorEarning earning = earningRepository.findById(earningId)
                .orElseThrow(() -> new ResourceNotFoundException("Earning record not found: " + earningId));

        RefundPolicy.reason(reason);
        if (!payable(earning)) throw new IllegalStateException("Only an unsettled eligible payout can be halted.");
        earning.setPayoutStatus("halted");
        if (audit != null) audit.record("hold-"+java.util.UUID.randomUUID(), "payout.hold", java.util.Map.of("earningId",earningId,"actorId",admin.getId(),"reason",reason));
        earningRepository.save(earning);
    }

    /**
     * Admin action to release/unhalt a previously disputed instructor payout (US-A26).
     */
    @Transactional
    public void unhaltEarningPayout(User admin, Integer earningId) {
        unhaltEarningPayout(admin, earningId, "Administrative release after dispute review");
    }
    @Transactional
    public void unhaltEarningPayout(User admin, Integer earningId, String reason) {
        RefundPolicy.reason(reason);
        if (admin == null || !"admin".equalsIgnoreCase(admin.getRole())) {
            throw new AccessDeniedException("Administrative privileges required to release payouts.");
        }

        InstructorEarning earning = earningRepository.findById(earningId)
                .orElseThrow(() -> new ResourceNotFoundException("Earning record not found: " + earningId));

        payable(earning); // Lock and refresh before checking the transition.
        if (!"halted".equals(earning.getPayoutStatus()) || !"success".equals(earning.getPayment().getStatus()) || RefundPolicy.blocksPayout(earning.getPayment()))
            throw new IllegalStateException("Only a held, non-refunded payout may be released.");
        earning.setPayoutStatus("unpaid");
        if (audit != null) audit.record("release-"+java.util.UUID.randomUUID(), "payout.release", java.util.Map.of("earningId",earningId,"actorId",admin.getId(),"reason",reason));
        earningRepository.save(earning);
    }

    /**
     * Viva Demo Simulation: Simulates direct bank transfer (IMPS/NEFT) payout to the instructor.
     */
    @Transactional
    public java.util.Map<String, Object> withdrawAllEarningsForInstructor(User instructor) {
        throw new IllegalArgumentException("Bank account, IFSC and bank name are required for simulated withdrawal.");
    }
    @Transactional
    public java.util.Map<String, Object> withdrawAllEarningsForInstructor(User instructor, com.olp.dto.BankPayoutRequest bank) {
        if (instructor == null || !"instructor".equalsIgnoreCase(instructor.getRole())) {
            throw new AccessDeniedException("Access denied: Instructor role required to withdraw earnings.");
        }

        if (bank == null) throw new IllegalArgumentException("Bank details are required.");
        bank.validate();
        return withdraw(instructor,bank);
    }
    @Transactional
    public java.util.Map<String,Object> withdrawSavedAccount(User instructor){
        if(instructor==null || !"instructor".equals(instructor.getRole()))throw new AccessDeniedException("Instructor required.");
        if(accounts==null)throw new IllegalStateException("Saved payout accounts are unavailable.");
        return withdraw(instructor,null);
    }
    private java.util.Map<String,Object> withdraw(User instructor,com.olp.dto.BankPayoutRequest bank){
        List<InstructorEarning> unpaidEarnings=earningRepository.findByInstructorIdOrderByCreatedAtDesc(instructor.getId()).stream().filter(e -> "unpaid".equalsIgnoreCase(e.getPayoutStatus())).collect(Collectors.toList());
        unpaidEarnings.removeIf(e -> !payable(e));
        return settleEligible(instructor,unpaidEarnings,bank);
    }
    private java.util.Map<String,Object> settleEligible(User instructor,List<InstructorEarning> unpaidEarnings,com.olp.dto.BankPayoutRequest bank){
        BigDecimal gross=unpaidEarnings.stream().map(InstructorEarning::getNetInstructorEarning).reduce(BigDecimal.ZERO,BigDecimal::add);
        if(gross.signum()<=0)throw new IllegalStateException("No eligible unpaid earnings to withdraw.");
        java.util.Map<String,Object> profile=accounts==null?null:accounts.requireVerified(instructor);
        if(profile==null && bank==null)throw new IllegalArgumentException("Bank payout destination or saved verified account is required.");
        if(profile!=null && bank!=null && (!profile.get("account").equals("•••• "+bank.accountNumber().substring(bank.accountNumber().length()-4)) || !profile.get("ifscCode").equals(bank.ifscCode())))throw new IllegalArgumentException("Withdraw to your saved verified account; update it before changing destinations.");
        String masked=profile==null?"•••• "+bank.accountNumber().substring(bank.accountNumber().length()-4):profile.get("account").toString();
        String bankName=profile==null?bank.bankName():profile.get("bankName").toString();
        String ifsc=profile==null?bank.ifscCode():profile.get("ifscCode").toString();
        BigDecimal rate=governance==null?BigDecimal.ZERO:governance.withholdingRate();
        BigDecimal debtRecovered=royalties==null?BigDecimal.ZERO:royalties.debt(instructor.getId()).min(gross);
        BigDecimal base=gross.subtract(debtRecovered);
        BigDecimal withheld=base.multiply(rate).setScale(2,java.math.RoundingMode.HALF_UP);
        BigDecimal net=base.subtract(withheld);
        String key="payout-"+instructor.getId()+"-"+unpaidEarnings.stream().map(InstructorEarning::getId).sorted().toList();
        String reference=profile!=null && gateway!=null?gateway.transferPayout(profile.get("providerAccountToken").toString(),net,"INR",key):"IMPS-SIM-"+java.util.UUID.randomUUID();
        if(reference==null || reference.isBlank())throw new IllegalStateException("Provider did not confirm payout; please retry later.");
        for(var earning:unpaidEarnings){earning.setPayoutStatus("paid");earningRepository.save(earning);}
        java.util.Map<String,Object> record=new java.util.LinkedHashMap<>();
        record.put("instructorId",instructor.getId());record.put("amount",net);record.put("grossRoyalty",gross);record.put("withholdingRate",rate);record.put("withholdingAmount",withheld);record.put("reference",reference);record.put("bankName",bankName);record.put("ifscCode",ifsc);record.put("account",masked);record.put("earningIds",unpaidEarnings.stream().map(InstructorEarning::getId).toList());record.put("status","paid");record.put("mode","simulation");
        record.put("refundDebtRecovered",debtRecovered);
        if(audit!=null)audit.record(reference,"payout.settled",record);
        if(royalties!=null)royalties.recovered(instructor.getId(),reference,debtRecovered);
        return java.util.Map.of("success",true,"amountWithdrawn",net,"grossRoyalty",gross,"withholdingAmount",withheld,"itemsSettled",unpaidEarnings.size(),"utrNumber",reference,"bankName",bankName+" (A/C "+masked+")","ifscCode",ifsc,"recipientName",instructor.getName(),"message","Bank payout simulated. No money was transferred.");
    }
    @Transactional(readOnly=true)
    public List<java.util.Map<String,Object>> payoutHistory(User user) {
        if (user == null || !java.util.Set.of("instructor","admin").contains(user.getRole())) throw new AccessDeniedException("Instructor or admin required.");
        return audit.list("payout.settled").stream().filter(row -> "admin".equals(user.getRole()) || user.getId().toString().equals(row.get("instructorId").toString())).toList();
    }


    @Transactional(readOnly=true)
    public java.util.Map<String,Object> earningsPage(User user,int page,int size){
        if(user==null || !java.util.Set.of("instructor","admin").contains(user.getRole()))throw new AccessDeniedException("Instructor or administrator required.");
        if(page<0 || page>1000000 || size<1 || size>200)throw new IllegalArgumentException("Choose page >=0 and size 1–200.");
        Integer owner="admin".equals(user.getRole())?null:user.getId();
        var rows=earningRepository.findLedger(owner,org.springframework.data.domain.PageRequest.of(page,size));
        var total=earningRepository.summarize(owner);
        var sales=rows.getContent().stream().map(e -> new InstructorEarningsResponse.SaleRecord(e.getId(),e.getCourse().getId(),e.getCourse().getTitle(),e.getPayment().getOrderId(),e.getGrossAmount(),e.getPlatformCommission(),e.getNetInstructorEarning(),e.getPayoutStatus(),e.getCreatedAt())).toList();
        var summary=new InstructorEarningsResponse(user.getId(),user.getName(),total.getGross(),total.getCommission(),total.getNet(),total.getUnpaid(),sales);
        return java.util.Map.of("summary",summary,"page",page,"size",size,"totalElements",rows.getTotalElements(),"totalPages",rows.getTotalPages());
    }

    @Transactional(readOnly=true)
    public java.util.Map<String,Object> payoutQuote(User user){
        if(user==null || !"instructor".equals(user.getRole()))throw new AccessDeniedException("Instructor required.");
        BigDecimal gross=earningRepository.summarize(user.getId()).getUnpaid();
        BigDecimal outstanding=royalties==null?BigDecimal.ZERO:royalties.debt(user.getId());
        BigDecimal debt=outstanding.min(gross);
        BigDecimal rate=governance==null?BigDecimal.ZERO:governance.withholdingRate();
        BigDecimal withheld=gross.subtract(debt).multiply(rate).setScale(2,java.math.RoundingMode.HALF_UP);
        return java.util.Map.of("grossRoyalty",gross,"refundDebtRecovered",debt,"outstandingRefundDebt",outstanding,"withholdingAmount",withheld,"netPayout",gross.subtract(debt).subtract(withheld),"mode","simulation");
    }
}
