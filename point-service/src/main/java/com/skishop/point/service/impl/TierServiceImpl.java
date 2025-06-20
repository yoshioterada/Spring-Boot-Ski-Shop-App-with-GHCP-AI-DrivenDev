package com.skishop.point.service.impl;

import com.skishop.point.dto.UserTierDto;
import com.skishop.point.entity.TierDefinition;
import com.skishop.point.entity.UserTier;
import com.skishop.point.repository.TierDefinitionRepository;
import com.skishop.point.repository.UserTierRepository;
import com.skishop.point.service.TierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TierServiceImpl implements TierService {
    
    private final UserTierRepository userTierRepository;
    private final TierDefinitionRepository tierDefinitionRepository;
    
    @Override
    @Transactional(readOnly = true)
    public UserTierDto getUserTier(UUID userId) {
        UserTier userTier = userTierRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultUserTier(userId));
        
        TierDefinition tierDefinition = tierDefinitionRepository.findById(userTier.getTierLevel())
                .orElse(null);
        
        return mapToDto(userTier, tierDefinition);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TierDefinition> getAllTiers() {
        return tierDefinitionRepository.findByIsActiveTrueOrderByMinPointsRequiredAsc();
    }
    
    @Override
    @Transactional(readOnly = true)
    public TierDefinition getTierDefinition(String tierLevel) {
        return tierDefinitionRepository.findById(tierLevel).orElse(null);
    }
    
    @Override
    public void updateUserTier(UUID userId, Integer totalPointsEarned) {
        log.debug("Updating tier for user {} with {} total points", userId, totalPointsEarned);
        
        UserTier userTier = userTierRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultUserTier(userId));
        
        // Find appropriate tier based on points
        TierDefinition newTier = tierDefinitionRepository.findHighestTierForPoints(totalPointsEarned)
                .orElse(getDefaultTier());
        
        String currentTierLevel = userTier.getTierLevel();
        String newTierLevel = newTier.getTierLevel();
        
        // Update user tier if changed
        if (!currentTierLevel.equals(newTierLevel)) {
            userTier.setTierLevel(newTierLevel);
            userTier.setTierUpgradedAt(LocalDateTime.now());
            userTier.setUpdatedAt(LocalDateTime.now());
            
            log.info("User {} tier upgraded from {} to {}", userId, currentTierLevel, newTierLevel);
        }
        
        userTier.setTotalPointsEarned(totalPointsEarned);
        userTier.setUpdatedAt(LocalDateTime.now());
        
        userTierRepository.save(userTier);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserTierDto checkUpgradeEligibility(UUID userId) {
        UserTier userTier = userTierRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultUserTier(userId));
        
        TierDefinition currentTierDef = tierDefinitionRepository.findById(userTier.getTierLevel())
                .orElse(getDefaultTier());
        
        TierDefinition nextTierDef = null;
        if (currentTierDef.getNextTier() != null) {
            nextTierDef = tierDefinitionRepository.findById(currentTierDef.getNextTier())
                    .orElse(null);
        }
        
        return mapToDto(userTier, currentTierDef, nextTierDef);
    }
    
    @Override
    public void processAllTierUpgrades() {
        log.info("Processing tier upgrades for all users");
        
        List<UserTier> eligibleUsers = userTierRepository.findUsersEligibleForUpgrade();
        
        for (UserTier userTier : eligibleUsers) {
            updateUserTier(userTier.getUserId(), userTier.getTotalPointsEarned());
        }
        
        log.info("Processed tier upgrades for {} users", eligibleUsers.size());
    }
    
    private UserTier createDefaultUserTier(UUID userId) {
        UserTier userTier = UserTier.builder()
                .userId(userId)
                .tierLevel("bronze")
                .totalPointsEarned(0)
                .currentPoints(0)
                .build();
        
        return userTierRepository.save(userTier);
    }
    
    private TierDefinition getDefaultTier() {
        return tierDefinitionRepository.findById("bronze")
                .orElseThrow(() -> new IllegalStateException("Default bronze tier not found"));
    }
    
    private UserTierDto mapToDto(UserTier userTier, TierDefinition tierDefinition) {
        return mapToDto(userTier, tierDefinition, null);
    }
    
    private UserTierDto mapToDto(UserTier userTier, TierDefinition tierDefinition, TierDefinition nextTierDefinition) {
        UserTierDto.UserTierDtoBuilder builder = UserTierDto.builder()
                .id(userTier.getId())
                .userId(userTier.getUserId())
                .tierLevel(userTier.getTierLevel())
                .totalPointsEarned(userTier.getTotalPointsEarned())
                .currentPoints(userTier.getCurrentPoints())
                .tierUpgradedAt(userTier.getTierUpgradedAt())
                .createdAt(userTier.getCreatedAt())
                .updatedAt(userTier.getUpdatedAt());
        
        if (tierDefinition != null) {
            builder.tierName(tierDefinition.getTierName())
                   .pointMultiplier(tierDefinition.getPointMultiplier())
                   .benefits(tierDefinition.getBenefits())
                   .nextTier(tierDefinition.getNextTier());
        }
        
        if (nextTierDefinition != null) {
            int pointsToNextTier = nextTierDefinition.getMinPointsRequired() - userTier.getTotalPointsEarned();
            builder.pointsToNextTier(Math.max(0, pointsToNextTier));
        }
        
        return builder.build();
    }
}
