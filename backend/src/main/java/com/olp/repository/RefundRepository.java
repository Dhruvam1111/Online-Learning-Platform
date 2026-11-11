package com.olp.repository;

import com.olp.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Integer> {

    Optional<Refund> findByRazorpayRefundId(String razorpayRefundId);

    @org.springframework.data.jpa.repository.Query("select r from Refund r where r.payment.id=:id order by r.createdAt desc,r.id desc")
    List<Refund> findByPaymentIdOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("id") Integer paymentId);

    @org.springframework.data.jpa.repository.Query("select case when count(r)>0 then true else false end from Refund r where r.payment.id=:id")
    boolean existsByPaymentId(@org.springframework.data.repository.query.Param("id") Integer paymentId);

    List<Refund> findAllByOrderByCreatedAtDesc();
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"payment","payment.course"})
    org.springframework.data.domain.Page<Refund> findAllByOrderByCreatedAtDescIdDesc(org.springframework.data.domain.Pageable pageable);
}
