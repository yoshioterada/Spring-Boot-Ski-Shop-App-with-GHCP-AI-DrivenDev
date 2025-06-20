package com.skishop.point.service;

import com.skishop.point.dto.UserTierDto;
import com.skishop.point.entity.TierDefinition;

import java.util.List;
import java.util.UUID;

public interface TierService {
    
    UserTierDto getUserTier(UUID userId);
    
    List<TierDefinition> getAllTiers();
    
    TierDefinition getTierDefinition(String tierLevel);
    
    void updateUserTier(UUID userId, Integer totalPointsEarned);
    
    UserTierDto checkUpgradeEligibility(UUID userId);
    
    void processAllTierUpgrades();
}
