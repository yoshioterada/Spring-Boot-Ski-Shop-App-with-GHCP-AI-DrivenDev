package com.skishop.user.controller;

import com.skishop.user.dto.request.RoleCreateRequest;
import com.skishop.user.dto.request.RoleUpdateRequest;
import com.skishop.user.dto.request.UserRoleUpdateRequest;
import com.skishop.user.dto.request.UserStatusUpdateRequest;
import com.skishop.user.dto.response.RoleListResponse;
import com.skishop.user.dto.response.RoleResponse;
import com.skishop.user.dto.response.UserListResponse;
import com.skishop.user.dto.response.UserResponse;
import com.skishop.user.service.AdminService;
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

import java.util.UUID;

/**
 * 管理者機能 API コントローラー
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin API", description = "管理者機能API")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /**
     * ユーザー一覧取得
     */
    @GetMapping("/users")
    @Operation(summary = "ユーザー一覧取得", description = "管理者用のユーザー一覧を取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "ユーザー一覧の取得成功"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    public ResponseEntity<UserListResponse> getUsers(
            @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "検索キーワード") @RequestParam(required = false) String search,
            @Parameter(description = "ステータスフィルター") @RequestParam(required = false) String status,
            @Parameter(description = "ロールフィルター") @RequestParam(required = false) String role) {
        
        log.info("Getting users list with search: {}, status: {}, role: {}", search, status, role);
        UserListResponse response = adminService.getUsers(pageable, search, status, role);
        return ResponseEntity.ok(response);
    }

    /**
     * ユーザーロール更新
     */
    @PostMapping("/users/{userId}/roles")
    @Operation(summary = "ユーザーロール更新", description = "指定ユーザーのロールを更新します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "ロール更新成功"),
        @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません"),
        @ApiResponse(responseCode = "400", description = "リクエスト内容が不正です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    public ResponseEntity<UserResponse> updateUserRoles(
            @Parameter(description = "ユーザーID") @PathVariable UUID userId,
            @Parameter(description = "ロール更新リクエスト") @Valid @RequestBody UserRoleUpdateRequest request) {
        
        log.info("Updating roles for user: {}, roles: {}", userId, request.roleIds());
        UserResponse response = adminService.updateUserRoles(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * ユーザーステータス更新
     */
    @PutMapping("/users/{userId}/status")
    @Operation(summary = "ユーザーステータス更新", description = "指定ユーザーのステータスを更新します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "ステータス更新成功"),
        @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません"),
        @ApiResponse(responseCode = "400", description = "リクエスト内容が不正です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    public ResponseEntity<UserResponse> updateUserStatus(
            @Parameter(description = "ユーザーID") @PathVariable UUID userId,
            @Parameter(description = "ステータス更新リクエスト") @Valid @RequestBody UserStatusUpdateRequest request) {
        
        log.info("Updating status for user: {}, active: {}", userId, request.active());
        UserResponse response = adminService.updateUserStatus(userId.toString(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * ロール一覧取得
     */
    @GetMapping("/roles")
    @Operation(summary = "ロール一覧取得", description = "システム内の全ロール一覧を取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "ロール一覧の取得成功"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    public ResponseEntity<RoleListResponse> getRoles(@PageableDefault(size = 20) Pageable pageable) {
        log.info("Getting all roles");
        RoleListResponse response = adminService.getRoles(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 新規ロール作成
     */
    @PostMapping("/roles")
    @Operation(summary = "新規ロール作成", description = "新しいロールを作成します")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "ロール作成成功"),
        @ApiResponse(responseCode = "400", description = "リクエスト内容が不正です"),
        @ApiResponse(responseCode = "409", description = "既に存在するロール名です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    public ResponseEntity<RoleResponse> createRole(
            @Parameter(description = "ロール作成リクエスト") @Valid @RequestBody RoleCreateRequest request) {
        
        log.info("Creating new role: {}", request.name());
        RoleResponse response = adminService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * ロール更新
     */
    @PutMapping("/roles/{roleId}")
    @Operation(summary = "ロール更新", description = "指定ロールの情報を更新します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "ロール更新成功"),
        @ApiResponse(responseCode = "404", description = "ロールが見つかりません"),
        @ApiResponse(responseCode = "400", description = "リクエスト内容が不正です"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    public ResponseEntity<RoleResponse> updateRole(
            @Parameter(description = "ロールID") @PathVariable UUID roleId,
            @Parameter(description = "ロール更新リクエスト") @Valid @RequestBody RoleUpdateRequest request) {
        
        log.info("Updating role: {}", roleId);
        RoleResponse response = adminService.updateRole(roleId.toString(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * ロール削除
     */
    @DeleteMapping("/roles/{roleId}")
    @Operation(summary = "ロール削除", description = "指定ロールを削除します")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "ロール削除成功"),
        @ApiResponse(responseCode = "404", description = "ロールが見つかりません"),
        @ApiResponse(responseCode = "400", description = "使用中のロールは削除できません"),
        @ApiResponse(responseCode = "403", description = "管理者権限が必要です")
    })
    public ResponseEntity<Void> deleteRole(
            @Parameter(description = "ロールID") @PathVariable UUID roleId) {
        
        log.info("Deleting role: {}", roleId);
        adminService.deleteRole(roleId.toString());
        return ResponseEntity.noContent().build();
    }
}
