package com.olp.service;
import com.olp.entity.Payment;
import com.olp.repository.*;
import org.springframework.stereotype.Service;
import java.math.*;
import java.util.*;
/** Proportional royalty correction and recovery of already-paid refunded royalties. */
@Service
@org.springframework.transaction.annotation.Transactional
public class RoyaltyRefundService {
    @org.springframework.beans.factory.annotation.Autowired(required=false) private RefundRepository refunds;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private final InstructorEarningRepository earnings;
    private final WalletRepository locks;
    private final PaymentAuditService audit;
    public RoyaltyRefundService(InstructorEarningRepository earnings,WalletRepository locks,PaymentAuditService audit){this.earnings=earnings;this.locks=locks;this.audit=audit;}
    public void adjust(Payment payment,BigDecimal refund,String refundId){
        String key="royalty-adjustment-"+refundId;if(audit.exists(key))return;
        BigDecimal gross=payment.getAmount().add(RefundPolicy.wallet(payment));
        BigDecimal fee=payment.getPlatformFee()==null?BigDecimal.ZERO:payment.getPlatformFee();
        BigDecimal feeReturned=refund.multiply(fee).divide(gross,2,RoundingMode.HALF_UP);
        BigDecimal royaltyReturned=refund.subtract(feeReturned);
        for(var earning:earnings.findByPaymentId(payment.getId())){
            locks.lockStudent(earning.getInstructor().getId());
            boolean paid=Set.of("paid","disputed_paid","refunded_paid").contains(earning.getPayoutStatus());
            if(!paid && "halted".equals(earning.getPayoutStatus()))for(var payout:audit.list("payout.settled"))if(payout.get("earningIds") instanceof Collection<?> ids && ids.stream().anyMatch(id -> id instanceof Number n && n.intValue()==earning.getId())){paid=true;break;}
            earning.setGrossAmount(gross.subtract(refund).max(BigDecimal.ZERO));earning.setPlatformCommission(fee.subtract(feeReturned).max(BigDecimal.ZERO));
            earning.setNetInstructorEarning(earning.getGrossAmount().subtract(earning.getPlatformCommission()).max(BigDecimal.ZERO));
            if(paid){earning.setPayoutStatus(earning.getNetInstructorEarning().signum()==0?"refunded_paid":"paid");audit.record("royalty-debt-"+refundId,"royalty.debt",Map.of("instructorId",earning.getInstructor().getId(),"orderId",payment.getOrderId(),"refundId",refundId,"amount",royaltyReturned));}
            else if(earning.getNetInstructorEarning().signum()==0)earning.setPayoutStatus("refunded");
            else if(!"halted".equals(earning.getPayoutStatus()))earning.setPayoutStatus("unpaid");
            earnings.save(earning);
        }
        audit.record(key,"royalty.refund",Map.of("orderId",payment.getOrderId(),"refundId",refundId,"refundedCommission",feeReturned,"refundedRoyalty",royaltyReturned));
    }
    public BigDecimal debt(Integer instructor){
        BigDecimal value=BigDecimal.ZERO;
        for(var row:audit.list("royalty.debt"))if(((Number)row.get("instructorId")).intValue()==instructor)value=value.add(new BigDecimal(row.get("amount").toString()));
        for(var row:audit.list("royalty.recovered"))if(((Number)row.get("instructorId")).intValue()==instructor)value=value.subtract(new BigDecimal(row.get("amount").toString()));
        return value.max(BigDecimal.ZERO);
    }
    public void recovered(Integer instructor,String reference,BigDecimal amount){if(amount.signum()>0)audit.record("royalty-recovered-"+reference,"royalty.recovered",Map.of("instructorId",instructor,"reference",reference,"amount",amount));}

    /** Explicit admin upgrade step; journals make repeated repair safe. */
    public int repairProcessedRefunds(){
        if(refunds==null || entityManager==null)throw new IllegalStateException("Refund repair is unavailable.");
        int count=0;
        for(var refund:refunds.findAllByOrderByCreatedAtDesc()){
            if(!"processed".equals(refund.getStatus()) || audit.exists("royalty-adjustment-"+refund.getRazorpayRefundId()))continue;
            var payment=entityManager.find(Payment.class,refund.getPayment().getId(),jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);entityManager.refresh(payment);
            if(earnings.findByPaymentId(payment.getId()).isEmpty())continue;
            adjust(payment,refund.getAmount(),refund.getRazorpayRefundId());count++;
        }
        return count;
    }
}
