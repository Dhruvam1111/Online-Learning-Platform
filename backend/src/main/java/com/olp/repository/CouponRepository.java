package com.olp.repository;

import com.olp.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Integer> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Coupon c where lower(c.code)=lower(:code)")
    Optional<Coupon> findByCodeForUpdate(@org.springframework.data.repository.query.Param("code") String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Coupon> findByIsActiveTrue();
}
