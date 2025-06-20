package com.skishop.user.controller;

import com.skishop.user.dto.request.UserPreferenceUpdateRequest;
import com.skishop.user.dto.response.UserPreferenceResponse;
import com.skishop.user.dto.response.UserPreferencesListResponse;
import com.skishop.user.service.UserPreferencesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * ユーザー設定API
 */
@RestController
@RequestMapping("/users/{userId}/preferences")
@Tag(name = "User Preferences", description = "ユーザー設定管理API")
@RequiredArgsConstructor
@Slf4j
public class UserPreferencesController {

    private final UserPreferencesService userPreferencesService;

    /**
     * 設定一覧取得
     */
    @GetMapping
    @Operation(summary = "設定一覧取得", description = "ユーザーの設定一覧を取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "設定一覧の取得成功"),
        @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.name")
    public ResponseEntity<UserPreferencesListResponse> getUserPreferences(
            @Parameter(description = "ユーザーID") @PathVariable UUID userId,
            @Parameter(description = "ページネーション") @ParameterObject Pageable pageable) {
        
        log.info("Getting preferences for user: {}", userId);
        UserPreferencesListResponse response = userPreferencesService.getUserPreferences(userId.toString(), pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 特定設定取得
     */
    @GetMapping("/{key}")
    @Operation(summary = "特定設定取得", description = "指定キーの設定値を取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "設定の取得成功"),
        @ApiResponse(responseCode = "404", description = "設定またはユーザーが見つかりません"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.name")
    public ResponseEntity<UserPreferenceResponse> getUserPreference(
            @Parameter(description = "ユーザーID") @PathVariable UUID userId,
            @Parameter(description = "設定キー") @PathVariable String key) {
        
        log.info("Getting preference for user: {}, key: {}", userId, key);
        UserPreferenceResponse response = userPreferencesService.getUserPreference(userId.toString(), key);
        return ResponseEntity.ok(response);
    }

    /**
     * 設定更新
     */
    @PutMapping("/{key}")
    @Operation(summary = "設定更新", description = "指定キーの設定値を更新します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "設定の更新成功"),
        @ApiResponse(responseCode = "400", description = "リクエスト内容が不正です"),
        @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.name")
    public ResponseEntity<UserPreferenceResponse> updateUserPreference(
            @Parameter(description = "ユーザーID") @PathVariable UUID userId,
            @Parameter(description = "設定キー") @PathVariable String key,
            @Parameter(description = "設定更新リクエスト") @Valid @RequestBody UserPreferenceUpdateRequest request) {
        
        log.info("Updating preference for user: {}, key: {}", userId, key);
        UserPreferenceResponse response = userPreferencesService.updateUserPreference(userId.toString(), key, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 設定削除
     */
    @DeleteMapping("/{key}")
    @Operation(summary = "設定削除", description = "指定キーの設定を削除します")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "設定の削除成功"),
        @ApiResponse(responseCode = "404", description = "設定またはユーザーが見つかりません"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.name")
    public ResponseEntity<Void> deleteUserPreference(
            @Parameter(description = "ユーザーID") @PathVariable UUID userId,
            @Parameter(description = "設定キー") @PathVariable String key) {
        
        log.info("Deleting preference for user: {}, key: {}", userId, key);
        userPreferencesService.deleteUserPreference(userId, key);
        return ResponseEntity.noContent().build();
    }
}
