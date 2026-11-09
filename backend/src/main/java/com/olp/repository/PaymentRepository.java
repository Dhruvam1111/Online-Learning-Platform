package com.olp.repository;

import com.olp.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    @Query("select p.orderId from Payment p where p.status = 'created' and p.createdAt < :before order by p.id")
    List<String> findExpiredOrderIds(@Param("before") java.time.LocalDateTime before);

    Optional<Payment> findByOrderId(String orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId")
    Optional<Payment> findByOrderIdForUpdate(@Param("orderId") String orderId);

    Optional<Payment> findByOrderIdIgnoreCase(String orderId);

    Optional<Payment> findByInvoiceNumber(String invoiceNumber);

    Optional<Payment> findByPaymentId(String paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.paymentId = :paymentId")
    Optional<Payment> findByPaymentIdForUpdate(@Param("paymentId") String paymentId);

    List<Payment> findByStudentIdOrderByCreatedAtDesc(Integer studentId);

    List<Payment> findAllByOrderByCreatedAtDesc();

    boolean existsByStudentIdAndCourseIdAndStatus(Integer studentId, Integer courseId, String status);
}
