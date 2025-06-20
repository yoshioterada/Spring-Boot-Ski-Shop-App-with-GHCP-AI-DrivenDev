package com.skishop.point.repository;

import com.skishop.point.entity.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, UUID> {
    
    List<PointTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    List<PointTransaction> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID userId, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("""
           SELECT SUM(pt.amount) FROM PointTransaction pt 
           WHERE pt.userId = :userId AND pt.transactionType = 'EARNED' AND pt.isExpired = false
           """)
    Integer getEarnedBalance(@Param("userId") UUID userId);
    
    @Query("""
           SELECT SUM(pt.amount) FROM PointTransaction pt 
           WHERE pt.userId = :userId AND pt.transactionType = 'REDEEMED'
           """)
    Integer getRedeemedBalance(@Param("userId") UUID userId);
    
    @Query("""
           SELECT pt FROM PointTransaction pt 
           WHERE pt.expiresAt IS NOT NULL AND pt.expiresAt <= :date AND pt.isExpired = false
           """)
    List<PointTransaction> findExpiringTransactions(@Param("date") LocalDateTime date);
    
    @Query("""
           SELECT pt FROM PointTransaction pt 
           WHERE pt.userId = :userId AND pt.expiresAt IS NOT NULL AND pt.expiresAt <= :date AND pt.isExpired = false
           """)
    List<PointTransaction> findExpiringTransactionsByUser(@Param("userId") UUID userId, @Param("date") LocalDateTime date);
    
    List<PointTransaction> findByReferenceIdAndTransactionType(String referenceId, PointTransaction.TransactionType transactionType);
}
