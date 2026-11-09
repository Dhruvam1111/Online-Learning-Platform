package com.olp.repository;

import com.olp.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Integer> {

    Optional<Refund> findByRazorpayRefundId(String razorpayRefundId);

    List<Refund> findByPaymentIdOrderByCreatedAtDesc(Integer paymentId);

    boolean existsByPaymentId(Integer paymentId);

    List<Refund> findAllByOrderByCreatedAtDesc();
}
