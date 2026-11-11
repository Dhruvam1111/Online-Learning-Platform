package com.olp.repository;

import com.olp.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Integer> {

    boolean existsByCouponIdAndStudentId(Integer couponId, Integer studentId);

    List<CouponUsage> findByStudentIdOrderByUsedAtDesc(Integer studentId);
}
