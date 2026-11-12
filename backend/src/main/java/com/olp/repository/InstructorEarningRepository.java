package com.olp.repository;

import com.olp.entity.InstructorEarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface InstructorEarningRepository extends JpaRepository<InstructorEarning, Integer> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"payment","course","instructor"})
    List<InstructorEarning> findByInstructorIdOrderByCreatedAtDesc(Integer instructorId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"payment","course","instructor"})
    List<InstructorEarning> findByCourseIdOrderByCreatedAtDesc(Integer courseId);

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"payment","course","instructor"})
    List<InstructorEarning> findAll();

    boolean existsByPaymentId(Integer paymentId);

    List<InstructorEarning> findByPaymentId(Integer paymentId);

    @Query("SELECT COALESCE(SUM(ie.netInstructorEarning), 0) FROM InstructorEarning ie WHERE ie.instructor.id = :instructorId")
    BigDecimal getTotalEarningsForInstructor(@Param("instructorId") Integer instructorId);

    @Query("SELECT COALESCE(SUM(ie.netInstructorEarning), 0) FROM InstructorEarning ie WHERE ie.instructor.id = :instructorId AND ie.payoutStatus = 'unpaid'")
    BigDecimal getUnpaidBalanceForInstructor(@Param("instructorId") Integer instructorId);

    interface EarningsTotals { BigDecimal getGross(); BigDecimal getCommission(); BigDecimal getNet(); BigDecimal getUnpaid(); }
    @Query("select coalesce(sum(e.grossAmount),0) as gross, coalesce(sum(e.platformCommission),0) as commission, coalesce(sum(e.netInstructorEarning),0) as net, coalesce(sum(case when e.payoutStatus='unpaid' and e.payment.status='success' and (e.payment.refundStatus is null or e.payment.refundStatus not in ('refunded','pending') or (e.payment.refundStatus='refunded' and e.payment.refundAmount>0 and e.payment.refundAmount<(e.payment.amount+coalesce(e.payment.walletAmountUsed,0)))) then e.netInstructorEarning else 0 end),0) as unpaid from InstructorEarning e where (:owner is null or e.instructor.id=:owner)")
    EarningsTotals summarize(@Param("owner") Integer owner);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"payment","course","instructor"})
    @Query("select e from InstructorEarning e where (:owner is null or e.instructor.id=:owner) order by e.createdAt desc,e.id desc")
    org.springframework.data.domain.Page<InstructorEarning> findLedger(@Param("owner") Integer owner,org.springframework.data.domain.Pageable pageable);
}
