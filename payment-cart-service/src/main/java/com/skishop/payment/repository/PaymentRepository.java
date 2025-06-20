package com.skishop.payment.repository;

import com.skishop.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByUserId(UUID userId);

    Page<Payment> findByUserId(UUID userId, Pageable pageable);

    Optional<Payment> findByPaymentIntentId(String paymentIntentId);

    List<Payment> findByStatus(Payment.PaymentStatus status);

    List<Payment> findByUserIdAndStatus(UUID userId, Payment.PaymentStatus status);

    @Query("""
           SELECT p FROM Payment p 
           WHERE p.userId = :userId 
           AND p.createdAt BETWEEN :startDate AND :endDate
           """)
    List<Payment> findByUserIdAndDateRange(@Param("userId") UUID userId, 
                                         @Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);

    @Query("""
           SELECT p FROM Payment p 
           WHERE p.status = :status 
           AND p.createdAt < :beforeDate
           """)
    List<Payment> findStalePayments(@Param("status") Payment.PaymentStatus status, 
                                  @Param("beforeDate") LocalDateTime beforeDate);

    @Query("""
           SELECT SUM(p.amount) 
           FROM Payment p 
           WHERE p.userId = :userId 
           AND p.status = 'COMPLETED'
           """)
    Double getTotalAmountByUserId(@Param("userId") UUID userId);

    @Query("""
           SELECT COUNT(p) 
           FROM Payment p 
           WHERE p.status = 'COMPLETED' 
           AND p.completedAt BETWEEN :startDate AND :endDate
           """)
    long countCompletedPaymentsBetween(@Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate);

    Optional<Payment> findByCartId(UUID cartId);

    boolean existsByCartIdAndStatus(UUID cartId, Payment.PaymentStatus status);
}
