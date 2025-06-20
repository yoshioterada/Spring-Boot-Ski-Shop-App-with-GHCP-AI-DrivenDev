package com.skishop.point.repository;

import com.skishop.point.entity.TierDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TierDefinitionRepository extends JpaRepository<TierDefinition, String> {
    
    List<TierDefinition> findByIsActiveTrueOrderByMinPointsRequiredAsc();
    
    @Query("""
           SELECT td FROM TierDefinition td 
           WHERE td.minPointsRequired <= :points AND td.isActive = true 
           ORDER BY td.minPointsRequired DESC
           """)
    Optional<TierDefinition> findHighestTierForPoints(@Param("points") Integer points);
    
    @Query("""
           SELECT td FROM TierDefinition td 
           WHERE td.tierLevel = :currentTier
           """)
    Optional<TierDefinition> findNextTier(@Param("currentTier") String currentTierLevel);
}
