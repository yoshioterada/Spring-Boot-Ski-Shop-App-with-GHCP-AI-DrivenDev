package com.skishop.point.repository;

import com.skishop.point.entity.UserTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserTierRepository extends JpaRepository<UserTier, UUID> {
    
    Optional<UserTier> findByUserId(UUID userId);
    
    @Query("""
           SELECT ut FROM UserTier ut 
           WHERE ut.tierLevel = :tierLevel
           """)
    List<UserTier> findByTierLevel(@Param("tierLevel") String tierLevel);
    
    @Query("""
           SELECT ut FROM UserTier ut 
           WHERE ut.totalPointsEarned >= (
               SELECT td.minPointsRequired FROM TierDefinition td 
               WHERE td.tierLevel = ut.tierLevel
           ) + 1000 
           AND ut.tierLevel != 'platinum'
           """)
    List<UserTier> findUsersEligibleForUpgrade();
    
    @Query("""
           SELECT COUNT(ut) FROM UserTier ut 
           WHERE ut.tierLevel = :tierLevel
           """)
    Long countByTierLevel(@Param("tierLevel") String tierLevel);
}
