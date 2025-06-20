package com.skishop.coupon.controller;

import com.skishop.coupon.dto.CampaignDto;
import com.skishop.coupon.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Campaign Management", description = "キャンペーン管理API")
public class CampaignController {

    private final CampaignService campaignService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "キャンペーン作成", description = "新しいキャンペーンを作成します")
    public ResponseEntity<Map<String, Object>> createCampaign(
            @Valid @RequestBody CampaignDto.CampaignRequest request) {
        
        log.info("Creating campaign: {}", request.getName());
        CampaignDto.CampaignResponse response = campaignService.createCampaign(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "success", true,
            "data", response
        ));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "キャンペーン一覧取得", description = "キャンペーン一覧を取得します")
    public ResponseEntity<Map<String, Object>> getCampaigns(
            @Valid @ModelAttribute CampaignDto.CampaignListRequest request) {
        
        Page<CampaignDto.CampaignResponse> campaigns = campaignService.getCampaigns(request);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", campaigns.getContent(),
            "pagination", Map.of(
                "page", campaigns.getNumber(),
                "size", campaigns.getSize(),
                "totalElements", campaigns.getTotalElements(),
                "totalPages", campaigns.getTotalPages()
            )
        ));
    }

    @PutMapping("/{campaignId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "キャンペーン更新", description = "キャンペーン情報を更新します")
    public ResponseEntity<Map<String, Object>> updateCampaign(
            @Parameter(description = "キャンペーンID") @PathVariable UUID campaignId,
            @Valid @RequestBody CampaignDto.CampaignUpdateRequest request) {
        
        log.info("Updating campaign: {}", campaignId);
        CampaignDto.CampaignResponse response = campaignService.updateCampaign(campaignId, request);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", response
        ));
    }

    @PostMapping("/{campaignId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "キャンペーン有効化", description = "キャンペーンを有効化します")
    public ResponseEntity<Map<String, Object>> activateCampaign(
            @Parameter(description = "キャンペーンID") @PathVariable UUID campaignId) {
        
        log.info("Activating campaign: {}", campaignId);
        CampaignDto.CampaignResponse response = campaignService.activateCampaign(campaignId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", response
        ));
    }

    @GetMapping("/{campaignId}/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "キャンペーン分析取得", description = "キャンペーンの分析データを取得します")
    public ResponseEntity<Map<String, Object>> getCampaignAnalytics(
            @Parameter(description = "キャンペーンID") @PathVariable UUID campaignId) {
        
        CampaignDto.CampaignAnalyticsResponse analytics = campaignService.getCampaignAnalytics(campaignId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", analytics
        ));
    }

    @GetMapping("/active")
    @Operation(summary = "アクティブキャンペーン取得", description = "現在アクティブなキャンペーン一覧を取得します")
    public ResponseEntity<Map<String, Object>> getActiveCampaigns() {
        
        List<CampaignDto.CampaignResponse> campaigns = campaignService.getActiveCampaigns();
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", campaigns
        ));
    }

    @GetMapping("/{campaignId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "キャンペーン詳細取得", description = "指定されたキャンペーンの詳細情報を取得します")
    public ResponseEntity<Map<String, Object>> getCampaignById(
            @Parameter(description = "キャンペーンID") @PathVariable UUID campaignId) {
        
        CampaignDto.CampaignResponse campaign = campaignService.getCampaignById(campaignId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", campaign
        ));
    }
}
