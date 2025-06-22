package com.skishop.user.controller;

import com.skishop.user.dto.request.UserRoleUpdateRequest;
import com.skishop.user.dto.response.UserListResponse;
import com.skishop.user.dto.response.UserResponse;
import com.skishop.user.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 管理者用コントローラー
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
@Tag(name = "管理者機能", description = "管理者専用のAPI")
public class AdminController {

    private final AdminService adminService;

    /**
     * ユーザー一覧取得
     */
    @GetMapping("/users")
    @Operation(summary = "ユーザー一覧取得", description = "管理者用のユーザー一覧を取得します")
    @ApiResponse(responseCode = "200", description = "ユーザー一覧の取得成功")
    @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    public ResponseEntity<UserListResponse> getUsers(
            @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "検索キーワード") @RequestParam(required = false) String search,
            @Parameter(description = "ステータスフィルター") @RequestParam(required = false) String status,
            @Parameter(description = "ロールフィルター") @RequestParam(required = false) String role) {
        
        log.info("Getting user list with search: {}, status: {}, role: {}", search, status, role);
        UserListResponse response = adminService.getUsersWithAdvancedFilters(pageable, search, status, role);
        return ResponseEntity.ok(response);
    }

    /**
     * ユーザーロール更新
     */
    @PostMapping("/users/{userId}/roles")
    @Operation(summary = "ユーザーロール更新", description = "指定ユーザーのロールを更新します")
    @ApiResponse(responseCode = "200", description = "ロール更新成功")
    @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません")
    @ApiResponse(responseCode = "400", description = "リクエスト内容が不正です")
    @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    public ResponseEntity<UserResponse> updateUserRoles(
            @Parameter(description = "ユーザーID") @PathVariable UUID userId,
            @Parameter(description = "ロール更新リクエスト") @Valid @RequestBody UserRoleUpdateRequest request) {
        
        log.info("Updating roles for user: {}, roles: {}", userId, request.roleIds());
        UserResponse response = adminService.updateUserRoles(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * ユーザーアカウント有効化
     */
    @PostMapping("/users/{userId}/activate")
    @Operation(summary = "ユーザーアカウント有効化", description = "指定ユーザーのアカウントを有効化します")
    @ApiResponse(responseCode = "200", description = "アカウント有効化成功")
    @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません")
    @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    public ResponseEntity<UserResponse> activateUser(
            @Parameter(description = "ユーザーID") @PathVariable UUID userId) {
        
        log.info("Activating user account: {}", userId);
        UserResponse response = adminService.activateUser(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * ユーザーアカウント無効化
     */
    @PostMapping("/users/{userId}/deactivate")
    @Operation(summary = "ユーザーアカウント無効化", description = "指定ユーザーのアカウントを無効化します")
    @ApiResponse(responseCode = "200", description = "アカウント無効化成功")
    @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません")
    @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    public ResponseEntity<UserResponse> deactivateUser(
            @Parameter(description = "ユーザーID") @PathVariable UUID userId) {
        
        log.info("Deactivating user account: {}", userId);
        UserResponse response = adminService.deactivateUser(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * ユーザー強制削除
     */
    @DeleteMapping("/users/{userId}")
    @Operation(summary = "ユーザー強制削除", description = "指定ユーザーを強制的に削除します")
    @ApiResponse(responseCode = "204", description = "ユーザー削除成功")
    @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません")
    @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ユーザーID") @PathVariable UUID userId) {
        
        log.info("Force deleting user: {}", userId);
        adminService.hardDeleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * システム統計情報取得
     */
    @GetMapping("/stats")
    @Operation(summary = "システム統計情報取得", description = "システムの統計情報を取得します")
    @ApiResponse(responseCode = "200", description = "統計情報の取得成功")
    @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    public ResponseEntity<Object> getSystemStats() {
        
        log.info("Getting system statistics");
        Object stats = adminService.getSystemStats();
        return ResponseEntity.ok(stats);
    }
}
