package com.skishop.user.controller;

import com.skishop.user.dto.request.ChangePasswordRequest;
import com.skishop.user.dto.request.UserCreateRequest;
import com.skishop.user.dto.request.UserUpdateRequest;
import com.skishop.user.dto.response.CheckEmailResponse;
import com.skishop.user.dto.response.UserResponse;
import com.skishop.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * ユーザー管理コントローラー
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "ユーザー管理", description = "ユーザー関連のAPI")
public class UserController {

    private final UserService userService;

    /**
     * 新規ユーザー登録
     */
    @PostMapping
    @Operation(summary = "新規ユーザー登録", description = "新しいユーザーアカウントを作成します")
    @ApiResponse(responseCode = "201", description = "ユーザー登録成功")
    @ApiResponse(responseCode = "400", description = "リクエスト内容が不正です")
    @ApiResponse(responseCode = "409", description = "既に登録済みのメールアドレスです")
    public ResponseEntity<UserResponse> createUser(
            @Parameter(description = "ユーザー作成リクエスト") @Valid @RequestBody UserCreateRequest request) {
        
        log.info("Creating new user with email: {}", request.email());
        UserResponse userResponse = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    /**
     * ユーザー情報取得
     */
    @GetMapping("/{id}")
    @Operation(summary = "ユーザー情報取得", description = "指定IDのユーザー情報を取得します")
    @ApiResponse(responseCode = "200", description = "ユーザー情報の取得成功")
    @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません")
    @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.name")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "ユーザーID") @PathVariable UUID id) {
        
        log.info("Getting user information for id: {}", id);
        UserResponse userResponse = userService.getUserById(id);
        return ResponseEntity.ok(userResponse);
    }

    /**
     * ユーザー情報更新
     */
    @PutMapping("/{id}")
    @Operation(summary = "ユーザー情報更新", description = "ユーザー情報を更新します")
    @ApiResponse(responseCode = "200", description = "ユーザー情報の更新成功")
    @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません")
    @ApiResponse(responseCode = "400", description = "リクエスト内容が不正です")
    @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.name")
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "ユーザーID") @PathVariable UUID id,
            @Parameter(description = "ユーザー更新リクエスト") @Valid @RequestBody UserUpdateRequest request) {
        
        log.info("Updating user information for id: {}", id);
        UserResponse userResponse = userService.updateUser(id, request);
        return ResponseEntity.ok(userResponse);
    }

    /**
     * メールアドレス存在確認
     */
    @GetMapping("/check-email")
    @Operation(summary = "メールアドレス存在確認", description = "メールアドレスが既に登録されているかを確認します")
    @ApiResponse(responseCode = "200", description = "チェック完了")
    @ApiResponse(responseCode = "400", description = "メールアドレスが不正です")
    public ResponseEntity<CheckEmailResponse> checkEmailExists(
            @Parameter(description = "メールアドレス") @RequestParam String email) {
        
        log.info("Checking email existence: {}", email);
        CheckEmailResponse response = userService.checkEmailExists(email);
        return ResponseEntity.ok(response);
    }

    /**
     * パスワード変更
     */
    @PostMapping("/{id}/change-password")
    @Operation(summary = "パスワード変更", description = "ユーザーのパスワードを変更します")
    @ApiResponse(responseCode = "200", description = "パスワード変更成功")
    @ApiResponse(responseCode = "400", description = "現在のパスワードが一致しません")
    @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません")
    @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.name")
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "ユーザーID") @PathVariable UUID id,
            @Parameter(description = "パスワード変更リクエスト") @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Changing password for user id: {}", id);
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }

    /**
     * 現在のユーザー情報取得
     */
    @GetMapping("/me")
    @Operation(summary = "現在のユーザー情報取得", description = "認証されたユーザーの情報を取得します")
    @ApiResponse(responseCode = "200", description = "ユーザー情報の取得成功")
    @ApiResponse(responseCode = "401", description = "認証が必要です")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Getting current user information for: {}", userDetails.getUsername());
        UserResponse userResponse = userService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(userResponse);
    }
}