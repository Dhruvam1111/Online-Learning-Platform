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

    List<InstructorEarning> findByInstructorIdOrderByCreatedAtDesc(Integer instructorId);

    List<InstructorEarning> findByCourseIdOrderByCreatedAtDesc(Integer courseId);

    boolean existsByPaymentId(Integer paymentId);

    List<InstructorEarning> findByPaymentId(Integer paymentId);

    @Query("SELECT COALESCE(SUM(ie.netInstructorEarning), 0) FROM InstructorEarning ie WHERE ie.instructor.id = :instructorId")
    BigDecimal getTotalEarningsForInstructor(@Param("instructorId") Integer instructorId);

    @Query("SELECT COALESCE(SUM(ie.netInstructorEarning), 0) FROM InstructorEarning ie WHERE ie.instructor.id = :instructorId AND ie.payoutStatus = 'unpaid'")
    BigDecimal getUnpaidBalanceForInstructor(@Param("instructorId") Integer instructorId);
}
