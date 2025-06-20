package com.skishop.point.repository;

import com.skishop.point.entity.PointExpiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PointExpiryRepository extends JpaRepository<PointExpiry, UUID> {
    
    @Query("""
           SELECT pe FROM PointExpiry pe 
           WHERE pe.status = 'scheduled' AND pe.scheduledAt <= :date
           """)
    List<PointExpiry> findScheduledExpiriesByDate(@Param("date") LocalDateTime date);
    
    List<PointExpiry> findByTransactionId(UUID transactionId);
    
    @Query("""
           SELECT pe FROM PointExpiry pe 
           WHERE pe.status = :status
           """)
    List<PointExpiry> findByStatus(@Param("status") String status);
    
    @Query("""
           SELECT pe FROM PointExpiry pe 
           JOIN pe.transaction pt 
           WHERE pt.userId = :userId AND pe.status = 'scheduled'
           """)
    List<PointExpiry> findScheduledExpiriesByUser(@Param("userId") UUID userId);
}
