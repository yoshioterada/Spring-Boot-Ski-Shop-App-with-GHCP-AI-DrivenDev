package com.skishop.point.repository;

import com.skishop.point.entity.PointRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PointRedemptionRepository extends JpaRepository<PointRedemption, UUID> {
    
    List<PointRedemption> findByUserIdOrderByRedeemedAtDesc(UUID userId);
    
    List<PointRedemption> findByUserIdAndRedeemedAtBetweenOrderByRedeemedAtDesc(
            UUID userId, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT pr FROM PointRedemption pr WHERE pr.status = :status")
    List<PointRedemption> findByStatus(@Param("status") String status);
    
    @Query("SELECT SUM(pr.pointsUsed) FROM PointRedemption pr " +
           "WHERE pr.userId = :userId AND pr.status = 'completed'")
    Integer getTotalRedeemedPointsByUser(@Param("userId") UUID userId);
    
    List<PointRedemption> findByRedemptionTypeAndStatus(String redemptionType, String status);
}
