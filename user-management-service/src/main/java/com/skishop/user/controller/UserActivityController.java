package com.skishop.user.controller;

import com.skishop.user.dto.response.UserActivityListResponse;
import com.skishop.user.service.UserActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * ユーザーアクティビティ管理 API コントローラー
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Activity API", description = "ユーザーアクティビティ管理API")
public class UserActivityController {

    private final UserActivityService userActivityService;

    /**
     * ユーザーアクティビティ一覧取得
     */
    @GetMapping("/{userId}/activities")
    @Operation(summary = "ユーザーアクティビティ一覧取得", description = "指定ユーザーのアクティビティ履歴を取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "アクティビティ一覧の取得成功"),
        @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.name")
    public ResponseEntity<UserActivityListResponse> getUserActivities(
            @Parameter(description = "ユーザーID") @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "アクティビティタイプフィルター") @RequestParam(required = false) String activityType,
            @Parameter(description = "開始日 (YYYY-MM-DD)") @RequestParam(required = false) String fromDate,
            @Parameter(description = "終了日 (YYYY-MM-DD)") @RequestParam(required = false) String toDate) {
        
        log.info("Getting activities for user: {}, type: {}, from: {}, to: {}", 
                userId, activityType, fromDate, toDate);
        
        UserActivityListResponse response = userActivityService.getUserActivities(
                userId, pageable, activityType, fromDate, toDate);
        return ResponseEntity.ok(response);
    }

    /**
     * 自分のアクティビティ一覧取得
     */
    @GetMapping("/me/activities")
    @Operation(summary = "自分のアクティビティ一覧取得", description = "認証されたユーザーのアクティビティ履歴を取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "アクティビティ一覧の取得成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserActivityListResponse> getCurrentUserActivities(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "アクティビティタイプフィルター") @RequestParam(required = false) String activityType,
            @Parameter(description = "開始日 (YYYY-MM-DD)") @RequestParam(required = false) String fromDate,
            @Parameter(description = "終了日 (YYYY-MM-DD)") @RequestParam(required = false) String toDate) {
        
        log.info("Getting activities for current user: {}, type: {}, from: {}, to: {}", 
                userDetails.getUsername(), activityType, fromDate, toDate);
        
        UserActivityListResponse response = userActivityService.getCurrentUserActivities(
                userDetails.getUsername(), pageable, activityType, fromDate, toDate);
        return ResponseEntity.ok(response);
    }
}
