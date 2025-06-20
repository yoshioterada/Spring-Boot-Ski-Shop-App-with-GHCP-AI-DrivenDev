package com.skishop.point.controller;

import com.skishop.point.dto.UserTierDto;
import com.skishop.point.entity.TierDefinition;
import com.skishop.point.service.TierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tiers")
@RequiredArgsConstructor
@Tag(name = "Tier Management", description = "Customer tier and loyalty level management")
public class TierController {
    
    private final TierService tierService;
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user tier", description = "Retrieves the current tier information for a user")
    public ResponseEntity<UserTierDto> getUserTier(@PathVariable UUID userId) {
        UserTierDto userTier = tierService.getUserTier(userId);
        return ResponseEntity.ok(userTier);
    }
    
    @GetMapping
    @Operation(summary = "Get all tiers", description = "Retrieves all available tier definitions")
    public ResponseEntity<List<TierDefinition>> getAllTiers() {
        List<TierDefinition> tiers = tierService.getAllTiers();
        return ResponseEntity.ok(tiers);
    }
    
    @GetMapping("/{tierLevel}")
    @Operation(summary = "Get tier definition", description = "Retrieves specific tier definition")
    public ResponseEntity<TierDefinition> getTierDefinition(@PathVariable String tierLevel) {
        TierDefinition tierDefinition = tierService.getTierDefinition(tierLevel);
        return tierDefinition != null ? 
                ResponseEntity.ok(tierDefinition) : 
                ResponseEntity.notFound().build();
    }
    
    @GetMapping("/upgrade-eligibility/{userId}")
    @Operation(summary = "Check upgrade eligibility", description = "Checks if user is eligible for tier upgrade")
    public ResponseEntity<UserTierDto> checkUpgradeEligibility(@PathVariable UUID userId) {
        UserTierDto eligibility = tierService.checkUpgradeEligibility(userId);
        return ResponseEntity.ok(eligibility);
    }
    
    @PostMapping("/process-upgrades")
    @Operation(summary = "Process all tier upgrades", description = "Process tier upgrades for all eligible users (admin operation)")
    public ResponseEntity<Void> processAllTierUpgrades() {
        tierService.processAllTierUpgrades();
        return ResponseEntity.ok().build();
    }
}
