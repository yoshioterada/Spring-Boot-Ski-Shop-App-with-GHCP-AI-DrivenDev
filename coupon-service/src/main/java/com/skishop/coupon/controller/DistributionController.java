package com.skishop.coupon.controller;

import com.skishop.coupon.dto.request.DistributionRuleCreateRequest;
import com.skishop.coupon.dto.request.DistributionRuleUpdateRequest;
import com.skishop.coupon.dto.response.DistributionHistoryResponse;
import com.skishop.coupon.dto.response.DistributionRuleResponse;
import com.skishop.coupon.dto.response.DistributionRuleListResponse;
import com.skishop.coupon.service.DistributionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * クーポン配布管理 API コントローラー
 */
@RestController
@RequestMapping("/api/v1/distributions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Distribution API", description = "クーポン配布管理API")
@PreAuthorize("hasRole('ADMIN') or hasRole('CAMPAIGN_MANAGER')")
public class DistributionController {

    private final DistributionService distributionService;

    /**
     * 配布ルール取得
     */
    @GetMapping("/rules/{campaignId}")
    @Operation(summary = "配布ルール取得", description = "指定キャンペーンの配布ルールを取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "配布ルールの取得成功"),
        @ApiResponse(responseCode = "404", description = "キャンペーンが見つかりません"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    public ResponseEntity<DistributionRuleListResponse> getDistributionRules(
            @Parameter(description = "キャンペーンID") @PathVariable String campaignId) {
        
        log.info("Getting distribution rules for campaign: {}", campaignId);
        DistributionRuleListResponse response = distributionService.getDistributionRules(campaignId);
        return ResponseEntity.ok(response);
    }

    /**
     * 配布ルール作成
     */
    @PostMapping("/rules/{campaignId}")
    @Operation(summary = "配布ルール作成", description = "新しい配布ルールを作成します")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "配布ルール作成成功"),
        @ApiResponse(responseCode = "400", description = "リクエスト内容が不正です"),
        @ApiResponse(responseCode = "404", description = "キャンペーンが見つかりません"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    public ResponseEntity<DistributionRuleResponse> createDistributionRule(
            @Parameter(description = "キャンペーンID") @PathVariable String campaignId,
            @Parameter(description = "配布ルール作成リクエスト") @Valid @RequestBody DistributionRuleCreateRequest request) {
        
        log.info("Creating distribution rule for campaign: {}", campaignId);
        DistributionRuleResponse response = distributionService.createDistributionRule(campaignId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 配布ルール更新
     */
    @PutMapping("/rules/{ruleId}")
    @Operation(summary = "配布ルール更新", description = "配布ルールの内容を更新します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "配布ルール更新成功"),
        @ApiResponse(responseCode = "404", description = "配布ルールが見つかりません"),
        @ApiResponse(responseCode = "400", description = "リクエスト内容が不正です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    public ResponseEntity<DistributionRuleResponse> updateDistributionRule(
            @Parameter(description = "配布ルールID") @PathVariable String ruleId,
            @Parameter(description = "配布ルール更新リクエスト") @Valid @RequestBody DistributionRuleUpdateRequest request) {
        
        log.info("Updating distribution rule: {}", ruleId);
        DistributionRuleResponse response = distributionService.updateDistributionRule(ruleId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 配布ルール削除
     */
    @DeleteMapping("/rules/{ruleId}")
    @Operation(summary = "配布ルール削除", description = "指定配布ルールを削除します")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "配布ルール削除成功"),
        @ApiResponse(responseCode = "404", description = "配布ルールが見つかりません"),
        @ApiResponse(responseCode = "400", description = "使用中のルールは削除できません"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    public ResponseEntity<Void> deleteDistributionRule(
            @Parameter(description = "配布ルールID") @PathVariable String ruleId) {
        
        log.info("Deleting distribution rule: {}", ruleId);
        distributionService.deleteDistributionRule(ruleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 配布履歴取得
     */
    @GetMapping("/history/{campaignId}")
    @Operation(summary = "配布履歴取得", description = "指定キャンペーンの配布履歴を取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "配布履歴の取得成功"),
        @ApiResponse(responseCode = "404", description = "キャンペーンが見つかりません"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    public ResponseEntity<DistributionHistoryResponse> getDistributionHistory(
            @Parameter(description = "キャンペーンID") @PathVariable String campaignId,
            @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "開始日 (YYYY-MM-DD)") @RequestParam(required = false) String fromDate,
            @Parameter(description = "終了日 (YYYY-MM-DD)") @RequestParam(required = false) String toDate,
            @Parameter(description = "配布ステータスフィルター") @RequestParam(required = false) String status) {
        
        log.info("Getting distribution history for campaign: {}, from: {}, to: {}, status: {}", 
                campaignId, fromDate, toDate, status);
        
        DistributionHistoryResponse response = distributionService.getDistributionHistory(
                campaignId, pageable, fromDate, toDate, status);
        return ResponseEntity.ok(response);
    }

    /**
     * 手動配布実行
     */
    @PostMapping("/execute/{campaignId}")
    @Operation(summary = "手動配布実行", description = "指定キャンペーンのクーポンを手動で配布実行します")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "配布処理を開始しました"),
        @ApiResponse(responseCode = "404", description = "キャンペーンが見つかりません"),
        @ApiResponse(responseCode = "400", description = "配布実行できない状態です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    public ResponseEntity<Void> executeDistribution(
            @Parameter(description = "キャンペーンID") @PathVariable String campaignId,
            @Parameter(description = "対象ユーザーIDリスト") @RequestParam(required = false) String[] targetUserIds) {
        
        log.info("Executing manual distribution for campaign: {}, targetUsers: {}", 
                campaignId, targetUserIds != null ? targetUserIds.length : "all");
        
        distributionService.executeDistribution(campaignId, targetUserIds);
        return ResponseEntity.accepted().build();
    }
}
