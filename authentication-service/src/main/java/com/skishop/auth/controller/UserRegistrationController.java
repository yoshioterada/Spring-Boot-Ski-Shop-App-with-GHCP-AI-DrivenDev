package com.skishop.auth.controller;

import com.skishop.auth.dto.request.UserCreateRequest;
import com.skishop.auth.dto.response.UserResponse;
import com.skishop.auth.service.UserRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * ユーザー登録 API コントローラー
 */
@RestController
@RequestMapping("/api/auth/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Registration API", description = "ユーザー登録・削除API")
public class UserRegistrationController {

    private final UserRegistrationService userRegistrationService;

    /**
     * 新規ユーザー登録
     */
    @PostMapping
    @Operation(summary = "新規ユーザー登録", description = "新しいユーザーアカウントを作成し、イベントを発行します")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "ユーザー登録成功"),
        @ApiResponse(responseCode = "400", description = "リクエスト内容が不正です"),
        @ApiResponse(responseCode = "409", description = "既に登録済みのメールアドレスまたはユーザー名です")
    })
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("User registration request received for username: {}", request.getUsername());
        
        try {
            UserResponse response = userRegistrationService.registerUser(request);
            log.info("User registration completed for user: {}", response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("User registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Unexpected error during user registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * ユーザー削除（論理削除）
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "ユーザー削除", description = "ユーザーを論理削除し、イベントを発行します")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "ユーザー削除成功"),
        @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません"),
        @ApiResponse(responseCode = "403", description = "権限がありません")
    })
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID userId,
            @RequestParam(required = false, defaultValue = "Admin deletion") String reason) {
        log.info("User deletion request received for user: {} with reason: {}", userId, reason);
        
        try {
            userRegistrationService.deleteUser(userId, reason);
            log.info("User deletion completed for user: {}", userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("User deletion failed: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error during user deletion: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * ユーザー物理削除（管理者専用）
     */
    @DeleteMapping("/{userId}/hard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "ユーザー物理削除", description = "ユーザーを物理削除し、イベントを発行します")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "ユーザー物理削除成功"),
        @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません"),
        @ApiResponse(responseCode = "403", description = "権限がありません")
    })
    public ResponseEntity<Void> hardDeleteUser(
            @PathVariable UUID userId,
            @RequestParam(required = false, defaultValue = "Admin hard deletion") String reason) {
        log.info("User hard deletion request received for user: {} with reason: {}", userId, reason);
        
        try {
            userRegistrationService.hardDeleteUser(userId, reason);
            log.info("User hard deletion completed for user: {}", userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("User hard deletion failed: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error during user hard deletion: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
