package com.skishop.point.controller;

import com.skishop.point.dto.UserTierDto;
import com.skishop.point.entity.TierDefinition;
import com.skishop.point.service.TierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tiers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tier Management API v1", description = "ティア管理のREST API v1")
public class TierApiController {
    
    private final TierService tierService;
    
    /**
     * ユーザーティア情報取得
     */
    @GetMapping("/user")
    @Operation(summary = "ユーザーティア情報取得", description = "認証されたユーザーの現在のティア情報を取得")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getUserTier(Authentication authentication) {
        log.debug("Getting tier information for user: {}", authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        UserTierDto userTier = tierService.getUserTier(userId);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", userTier
        ));
    }
    
    /**
     * ティア特典取得
     */
    @GetMapping("/benefits")
    @Operation(summary = "ティア特典取得", description = "指定されたティアの特典情報を取得")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getTierBenefits(
            @RequestParam(required = false) String tierLevel,
            Authentication authentication) {
        
        log.debug("Getting tier benefits for tier: {}", tierLevel);
        
        if (tierLevel == null) {
            // ユーザーの現在のティア特典を取得
            UUID userId = UUID.fromString(authentication.getName());
            UserTierDto userTier = tierService.getUserTier(userId);
            tierLevel = userTier.getTierLevel();
        }
        
        TierDefinition tierDefinition = tierService.getTierDefinition(tierLevel);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "tier", tierDefinition,
                        "benefits", tierDefinition.getBenefits()
                )
        ));
    }
    
    /**
     * ティア進捗状況取得
     */
    @GetMapping("/progress")
    @Operation(summary = "ティア進捗状況取得", description = "次のティアまでの進捗状況を取得")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getTierProgress(Authentication authentication) {
        log.debug("Getting tier progress for user: {}", authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        UserTierDto eligibility = tierService.checkUpgradeEligibility(userId);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", eligibility
        ));
    }
    
    /**
     * 全ティア定義取得
     */
    @GetMapping
    @Operation(summary = "全ティア定義取得", description = "利用可能な全ティアの定義を取得")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAllTiers() {
        log.debug("Getting all tier definitions");
        
        List<TierDefinition> tiers = tierService.getAllTiers();
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of("tiers", tiers)
        ));
    }
    
    /**
     * ティア詳細取得
     */
    @GetMapping("/{tierLevel}")
    @Operation(summary = "ティア詳細取得", description = "指定されたティアの詳細情報を取得")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getTierDetails(@PathVariable String tierLevel) {
        log.debug("Getting tier details for tier: {}", tierLevel);
        
        TierDefinition tierDefinition = tierService.getTierDefinition(tierLevel);
        
        if (tierDefinition == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", tierDefinition
        ));
    }
}
