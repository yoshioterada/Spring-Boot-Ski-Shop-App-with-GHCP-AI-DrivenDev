package com.skishop.auth.controller;

import com.skishop.auth.dto.LoginRequest;
import com.skishop.auth.dto.LoginResponse;
import com.skishop.auth.dto.MfaVerificationRequest;
import com.skishop.auth.dto.TokenRefreshRequest;
import com.skishop.auth.dto.TokenRefreshResponse;
import com.skishop.auth.dto.PasswordResetRequest;
import com.skishop.auth.dto.PasswordResetConfirmRequest;
import com.skishop.auth.dto.LogoutResponse;
import com.skishop.auth.dto.OAuthCallbackRequest;
import com.skishop.auth.service.AuthenticationService;
import com.skishop.auth.service.MfaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;

/**
 * 認証API REST エンドポイントコントローラー
 * ユーザー認証、トークン管理、パスワードリセット等のAPIを提供
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication API", description = "認証・認可関連のAPI")
public class AuthApiController {

    private final AuthenticationService authenticationService;
    private final MfaService mfaService;

    /**
     * ユーザーログイン
     */
    @PostMapping("/login")
    @Operation(summary = "ユーザーログイン", description = "メールアドレスとパスワードによるユーザー認証")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Login attempt for user: {}", request.getEmail());
        
        try {
            LoginResponse response = authenticationService.authenticateUser(request, httpRequest);
            log.info("Login successful for user: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login failed for user: {}, error: {}", request.getEmail(), e.getMessage());
            throw e;
        }
    }

    /**
     * MFA検証
     */
    @PostMapping("/mfa/verify")
    @Operation(summary = "MFA検証", description = "多要素認証コードの検証")
    public ResponseEntity<LoginResponse> verifyMfa(
            @Valid @RequestBody MfaVerificationRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("MFA verification attempt for session: {}", request.getSessionId());
        
        try {
            LoginResponse response = mfaService.verifyMfaCode(request, httpRequest);
            log.info("MFA verification successful for session: {}", request.getSessionId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("MFA verification failed for session: {}, error: {}", request.getSessionId(), e.getMessage());
            throw e;
        }
    }

    /**
     * トークンリフレッシュ
     */
    @PostMapping("/refresh")
    @Operation(summary = "トークンリフレッシュ", description = "リフレッシュトークンを使用してアクセストークンを更新")
    public ResponseEntity<TokenRefreshResponse> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        
        log.info("Token refresh attempt");
        
        try {
            TokenRefreshResponse response = authenticationService.refreshTokenApi(request);
            log.info("Token refresh successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * ログアウト
     */
    @PostMapping("/logout")
    @Operation(summary = "ログアウト", description = "ユーザーログアウトとセッション無効化")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LogoutResponse> logout(
            HttpServletRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        log.info("Logout attempt");
        
        try {
            LogoutResponse response = authenticationService.logout(authHeader, request);
            log.info("Logout successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * OAuth リダイレクト開始
     */
    @GetMapping("/oauth/{provider}/redirect")
    @Operation(summary = "OAuth認証開始", description = "指定されたプロバイダーでのOAuth認証を開始")
    public ResponseEntity<Map<String, String>> oauthRedirect(
            @PathVariable String provider,
            @RequestParam(required = false) String redirectUri) {
        
        log.info("OAuth redirect initiated for provider: {}", provider);
        
        try {
            String authUrl = authenticationService.initiateOAuthFlow(provider, redirectUri);
            return ResponseEntity.ok(Map.of("authUrl", authUrl));
        } catch (Exception e) {
            log.error("OAuth redirect failed for provider: {}, error: {}", provider, e.getMessage());
            throw e;
        }
    }

    /**
     * OAuth コールバック
     */
    @PostMapping("/oauth/{provider}/callback")
    @Operation(summary = "OAuth認証コールバック", description = "OAuth プロバイダーからのコールバック処理")
    public ResponseEntity<LoginResponse> oauthCallback(
            @PathVariable String provider,
            @Valid @RequestBody OAuthCallbackRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("OAuth callback received for provider: {}", provider);
        
        try {
            LoginResponse response = authenticationService.handleOAuthCallback(provider, request, httpRequest);
            log.info("OAuth authentication successful for provider: {}", provider);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("OAuth callback failed for provider: {}, error: {}", provider, e.getMessage());
            throw e;
        }
    }

    /**
     * パスワードリセット要求
     */
    @PostMapping("/password/reset-request")
    @Operation(summary = "パスワードリセット要求", description = "パスワードリセット用のメールを送信")
    public ResponseEntity<Map<String, String>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        
        log.info("Password reset request for email: {}", request.getEmail());
        
        try {
            authenticationService.requestPasswordReset(request.getEmail());
            return ResponseEntity.ok(Map.of("message", "Password reset instructions sent to your email"));
        } catch (Exception e) {
            log.error("Password reset request failed for email: {}, error: {}", request.getEmail(), e.getMessage());
            // セキュリティ上、エラーがあっても成功レスポンスを返す
            return ResponseEntity.ok(Map.of("message", "Password reset instructions sent to your email"));
        }
    }

    /**
     * パスワードリセット実行
     */
    @PostMapping("/password/reset")
    @Operation(summary = "パスワードリセット実行", description = "トークンを使用してパスワードをリセット")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        
        log.info("Password reset execution attempt");
        
        try {
            authenticationService.resetPassword(request.getToken(), request.getNewPassword());
            log.info("Password reset successful");
            return ResponseEntity.ok(Map.of("message", "Password successfully reset"));
        } catch (Exception e) {
            log.error("Password reset failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * トークン検証
     */
    @PostMapping("/validate")
    @Operation(summary = "トークン検証", description = "アクセストークンの有効性を検証")
    public ResponseEntity<Map<String, Object>> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            Map<String, Object> tokenInfo = authenticationService.validateToken(authHeader);
            return ResponseEntity.ok(tokenInfo);
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 現在のユーザー情報取得
     */
    @GetMapping("/me")
    @Operation(summary = "現在のユーザー情報", description = "認証されたユーザーの情報を取得")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            Map<String, Object> userInfo = authenticationService.getCurrentUserInfo(authHeader);
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            log.error("Get current user failed: {}", e.getMessage());
            throw e;
        }
    }
}
