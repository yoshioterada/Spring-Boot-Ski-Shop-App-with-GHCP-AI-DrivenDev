package com.skishop.user.controller;

import com.skishop.user.dto.request.ChangePasswordRequest;
import com.skishop.user.dto.request.ResendVerificationRequest;
import com.skishop.user.dto.request.UserCreateRequest;
import com.skishop.user.dto.request.UserUpdateRequest;
import com.skishop.user.dto.request.VerifyEmailRequest;
import com.skishop.user.dto.response.CheckEmailResponse;
import com.skishop.user.dto.response.UserResponse;
import com.skishop.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
 * ユーザー管理 API コントローラー
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User API", description = "ユーザー管理API")
public class UserController {

    private final UserService userService;

    /**
     * 新規ユーザー登録
     */
    @PostMapping
    @Operation(summary = "新規ユーザー登録", description = "新しいユーザーアカウントを作成します")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "ユーザー登録成功"),
        @ApiResponse(responseCode = "400", description = "リクエスト内容が不正です"),
        @ApiResponse(responseCode = "409", description = "既に登録済みのメールアドレスです")
    })
    public ResponseEntity<UserResponse> createUser(
            @Parameter(description = "ユーザー作成リクエスト") @Valid @RequestBody UserCreateRequest request) {
        
        log.info("Creating new user with email: {}", request.email());
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * ユーザー情報取得
     */
    @GetMapping("/{id}")
    @Operation(summary = "ユーザー情報取得", description = "指定IDのユーザー情報を取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "ユーザー情報の取得成功"),
        @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.name")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "ユーザーID") @PathVariable UUID id) {
        
        log.info("Getting user information for id: {}", id);
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * ユーザー情報更新
     */
    @PutMapping("/{id}")
    @Operation(summary = "ユーザー情報更新", description = "ユーザー情報を更新します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "ユーザー情報の更新成功"),
        @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません"),
        @ApiResponse(responseCode = "400", description = "リクエスト内容が不正です"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.name")
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "ユーザーID") @PathVariable UUID id,
            @Parameter(description = "ユーザー更新リクエスト") @Valid @RequestBody UserUpdateRequest request) {
        
        log.info("Updating user information for id: {}", id);
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * ユーザー削除
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "ユーザー削除", description = "ユーザーアカウントを削除します")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "ユーザー削除成功"),
        @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.name")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ユーザーID") @PathVariable UUID id) {
        
        log.info("Deleting user account for id: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 自分のユーザー情報取得
     */
    @GetMapping("/me")
    @Operation(summary = "自分のユーザー情報取得", description = "認証されたユーザーの情報を取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "ユーザー情報の取得成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Getting current user information for: {}", userDetails.getUsername());
        UserResponse response = userService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * パスワード変更
     */
    @PutMapping("/me/password")
    @Operation(summary = "パスワード変更", description = "認証されたユーザーのパスワードを変更します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "パスワード変更成功"),
        @ApiResponse(responseCode = "400", description = "現在のパスワードが正しくありません"),
        @ApiResponse(responseCode = "401", description = "認証が必要です")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "パスワード変更リクエスト") @Valid @RequestBody ChangePasswordRequest request) {
        
        log.info("Changing password for user: {}", userDetails.getUsername());
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }

    /**
     * メールアドレス検証
     */
    @PostMapping("/verify-email")
    @Operation(summary = "メールアドレス検証", description = "メールアドレス検証トークンを使用してメールアドレスを検証します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "メールアドレス検証成功"),
        @ApiResponse(responseCode = "400", description = "無効なトークンです"),
        @ApiResponse(responseCode = "410", description = "トークンの期限が切れています")
    })
    public ResponseEntity<Void> verifyEmail(
            @Parameter(description = "メール検証リクエスト") @Valid @RequestBody VerifyEmailRequest request) {
        
        log.info("Verifying email with token");
        userService.verifyEmail(request.token());
        return ResponseEntity.ok().build();
    }

    /**
     * 検証メール再送信
     */
    @PostMapping("/resend-verification")
    @Operation(summary = "検証メール再送信", description = "メールアドレス検証用のメールを再送信します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "検証メール送信成功"),
        @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません"),
        @ApiResponse(responseCode = "400", description = "既に検証済みです")
    })
    public ResponseEntity<Void> resendVerificationEmail(
            @Parameter(description = "検証メール再送信リクエスト") @Valid @RequestBody ResendVerificationRequest request) {
        
        log.info("Resending verification email to: {}", request.email());
        userService.resendVerificationEmail(request);
        return ResponseEntity.ok().build();
    }

    /**
     * メールアドレス存在確認
     */
    @GetMapping("/check-email")
    @Operation(summary = "メールアドレス存在確認", description = "指定のメールアドレスが既に登録されているかチェックします")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "チェック完了")
    })
    public ResponseEntity<CheckEmailResponse> checkEmailExists(
            @Parameter(description = "チェックするメールアドレス") @RequestParam String email) {
        
        log.info("Checking email existence for: {}", email);
        CheckEmailResponse response = userService.checkEmailExists(email);
        return ResponseEntity.ok(response);
    }
}
