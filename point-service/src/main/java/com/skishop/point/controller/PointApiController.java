package com.skishop.point.controller;

import com.skishop.point.dto.*;
import com.skishop.point.service.PointService;
import com.skishop.point.service.TierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Point Management API v1", description = "ポイント管理のREST API v1")
public class PointApiController {
    
    private final PointService pointService;
    private final TierService tierService;
    
    /**
     * ポイント残高取得
     */
    @GetMapping("/balance")
    @Operation(summary = "ポイント残高取得", description = "認証されたユーザーのポイント残高とティア情報を取得")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getPointBalance(Authentication authentication) {
        log.info("Getting point balance for user: {}", authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        PointBalanceResponse balance = pointService.getPointBalance(userId);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", balance
        ));
    }
    
    /**
     * ポイント付与（内部API）
     */
    @PostMapping("/award")
    @Operation(summary = "ポイント付与", description = "ユーザーにポイントを付与（サービス間通信用）")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<Map<String, Object>> awardPoints(@Valid @RequestBody PointAwardRequest request) {
        log.info("Awarding {} points to user {} for reason: {}", 
                request.getAmount(), request.getUserId(), request.getReason());
        
        PointTransactionDto transaction = pointService.awardPoints(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "success", true,
                        "data", transaction
                ));
    }
    
    /**
     * ポイント使用
     */
    @PostMapping("/redeem")
    @Operation(summary = "ポイント使用", description = "ポイントを使用して割引や商品と交換")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> redeemPoints(
            @Valid @RequestBody PointRedemptionRequest request,
            Authentication authentication) {
        
        log.info("Redeeming {} points for user: {}", request.getPointsToRedeem(), authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        request.setUserId(userId);
        
        PointRedemptionResponse redemption = pointService.redeemPointsV2(request);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", redemption
        ));
    }
    
    /**
     * ポイント履歴取得
     */
    @GetMapping("/history")
    @Operation(summary = "ポイント履歴取得", description = "ユーザーのポイント取引履歴を取得")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getPointHistory(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "all") String type,
            Authentication authentication) {
        
        log.debug("Getting point history for user: {}", authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        List<PointTransactionDto> history = pointService.getPointHistory(userId);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "transactions", history,
                        "pagination", Map.of(
                                "limit", limit,
                                "offset", offset,
                                "total", history.size()
                        )
                )
        ));
    }
    
    /**
     * 有効期限が近いポイント取得
     */
    @GetMapping("/expiring")
    @Operation(summary = "有効期限が近いポイント", description = "指定した日数以内に有効期限が切れるポイントを取得")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getExpiringPoints(
            @RequestParam(defaultValue = "30") int days,
            Authentication authentication) {
        
        log.debug("Getting expiring points for user: {}", authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        List<PointTransactionDto> expiringPoints = pointService.getExpiringPoints(userId, days);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "expiringPoints", expiringPoints,
                        "daysUntilExpiry", days
                )
        ));
    }
    
    /**
     * ポイント交換オプション取得
     */
    @GetMapping("/redemption-options")
    @Operation(summary = "ポイント交換オプション", description = "利用可能なポイント交換オプションを取得")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getRedemptionOptions(Authentication authentication) {
        log.debug("Getting redemption options for user: {}", authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        List<RedemptionOptionDto> options = pointService.getRedemptionOptions(userId);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of("options", options)
        ));
    }
    
    /**
     * ポイント譲渡
     */
    @PostMapping("/transfer")
    @Operation(summary = "ポイント譲渡", description = "他のユーザーにポイントを譲渡")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> transferPoints(
            @Valid @RequestBody PointTransferRequest request,
            Authentication authentication) {
        
        log.info("Transferring {} points from user: {} to user: {}", 
                request.getAmount(), authentication.getName(), request.getToUserId());
        
        UUID fromUserId = UUID.fromString(authentication.getName());
        pointService.transferPoints(fromUserId, request.getToUserId(), 
                request.getAmount(), request.getReason());
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Points transferred successfully"
        ));
    }
}
