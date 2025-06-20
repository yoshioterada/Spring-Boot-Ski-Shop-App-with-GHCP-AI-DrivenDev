package com.skishop.auth.service;

import com.skishop.auth.dto.*;
import com.skishop.auth.entity.User;
import com.skishop.auth.entity.UserSession;
import com.skishop.auth.entity.UserMFA;
import com.skishop.auth.entity.SecurityLog;
import com.skishop.auth.exception.AuthenticationException;
import com.skishop.auth.exception.InvalidTokenException;
import com.skishop.auth.repository.UserRepository;
import com.skishop.auth.repository.UserSessionRepository;
import com.skishop.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * 認証サービス
 * ログイン、ログアウト、トークンリフレッシュ、MFA検証を担当
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthenticationService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SecurityLogService securityLogService;
    private final MfaService mfaService;
    private final EventPublishingService eventPublishingService;

    /**
     * ユーザーログイン
     */
    public AuthenticationResponse authenticate(LoginRequest request, String ipAddress, String userAgent) {
        log.info("Authentication attempt for email: {}", request.getEmail());
        
        // ユーザー検索
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

        // アカウント状態チェック
        if (!user.isActive()) {
            securityLogService.logAuthenticationAttempt(user.getId(), "LOGIN_FAILED", 
                ipAddress, userAgent, "Account inactive");
            throw new AuthenticationException("Account is inactive");
        }

        if (user.isLocked()) {
            securityLogService.logAuthenticationAttempt(user.getId(), "LOGIN_FAILED", 
                ipAddress, userAgent, "Account locked");
            throw new AuthenticationException("Account is locked");
        }

        // パスワード検証
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user, ipAddress, userAgent);
            throw new AuthenticationException("Invalid credentials");
        }

        // MFA必須チェック
        if (user.isMfaEnabled()) {
            if (request.getMfaCode() == null || request.getMfaCode().isEmpty()) {
                // MFA待機状態のトークンを生成
                String tempToken = jwtUtil.generateTempToken(user.getId(), "MFA_REQUIRED");
                return AuthenticationResponse.builder()
                    .tempToken(tempToken)
                    .mfaRequired(true)
                    .build();
            }

            // MFA検証
            if (!mfaService.verifyMfaCode(user.getId(), request.getMfaCode())) {
                securityLogService.logAuthenticationAttempt(user.getId(), "MFA_FAILED", 
                    ipAddress, userAgent, "Invalid MFA code");
                throw new AuthenticationException("Invalid MFA code");
            }
        }

        // ログイン成功処理
        return handleSuccessfulLogin(user, ipAddress, userAgent);
    }

    /**
     * MFA検証
     */
    public AuthenticationResponse verifyMfa(MfaVerificationRequest request, String ipAddress, String userAgent) {
        log.info("MFA verification attempt");

        // 一時トークンの検証
        if (!jwtUtil.validateTempToken(request.getTempToken())) {
            throw new InvalidTokenException("Invalid temporary token");
        }

        UUID userId = UUID.fromString(jwtUtil.getUserIdFromToken(request.getTempToken()));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthenticationException("User not found"));

        // MFA検証
        if (!mfaService.verifyMfaCode(userId, request.getMfaCode())) {
            securityLogService.logAuthenticationAttempt(userId, "MFA_FAILED", 
                ipAddress, userAgent, "Invalid MFA code");
            throw new AuthenticationException("Invalid MFA code");
        }

        // ログイン成功処理
        return handleSuccessfulLogin(user, ipAddress, userAgent);
    }

    /**
     * トークンリフレッシュ
     */
    public AuthenticationResponse refreshToken(TokenRefreshRequest request) {
        log.info("Token refresh attempt");

        String refreshToken = request.getRefreshToken();
        
        // リフレッシュトークンの検証
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        UUID userId = UUID.fromString(jwtUtil.getUserIdFromToken(refreshToken));
        
        // セッション確認
        UserSession session = userSessionRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new InvalidTokenException("Session not found"));

        if (session.getExpiresAt().isBefore(Instant.now())) {
            userSessionRepository.delete(session);
            throw new InvalidTokenException("Session expired");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthenticationException("User not found"));

        // 新しいトークン生成
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().name());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId().toString());

        // セッション更新
        session.setRefreshToken(newRefreshToken);
        session.setExpiresAt(Instant.now().plusSeconds(7L * 24 * 3600)); // 7日
        userSessionRepository.save(session);

        return AuthenticationResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .tokenType("Bearer")
            .expiresIn(3600L)
            .user(convertToUserDto(user))
            .build();
    }

    /**
     * トークンリフレッシュ（API用）
     */
    public TokenRefreshResponse refreshTokenApi(TokenRefreshRequest request) {
        AuthenticationResponse authResponse = refreshToken(request);
        
        return TokenRefreshResponse.success(
                authResponse.getAccessToken(),
                authResponse.getRefreshToken(),
                authResponse.getTokenType(),
                authResponse.getExpiresIn(),
                Instant.now().plusSeconds(authResponse.getExpiresIn())
        );
    }

    /**
     * ログアウト
     */
    public void logout(String refreshToken, String ipAddress, String userAgent) {
        log.info("Logout attempt");

        try {
            UUID userId = UUID.fromString(jwtUtil.getUserIdFromToken(refreshToken));
            
            // セッション削除
            userSessionRepository.findByRefreshToken(refreshToken)
                .ifPresent(userSessionRepository::delete);

            // セキュリティログ記録
            securityLogService.logAuthenticationAttempt(userId, "LOGOUT", 
                ipAddress, userAgent, "User logout");

            // ログアウトイベント発行
            eventPublishingService.publishUserEvent("USER_LOGOUT", userId, "User logged out");

        } catch (Exception e) {
            log.warn("Error during logout: {}", e.getMessage());
        }
    }

    /**
     * 全セッション無効化
     */
    public void logoutAllSessions(UUID userId) {
        log.info("Logout all sessions for user: {}", userId);
        
        userSessionRepository.deleteByUserId(userId);
        
        // イベント発行
        eventPublishingService.publishUserEvent("USER_LOGOUT_ALL", userId, "All sessions invalidated");
    }

    /**
     * ログイン成功処理
     */
    private AuthenticationResponse handleSuccessfulLogin(User user, String ipAddress, String userAgent) {
        // ログイン失敗回数リセット
        user.setFailedLoginAttempts(0);
        user.setLastLogin(Instant.now());
        userRepository.save(user);

        // トークン生成
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId().toString());

        // セッション作成
        UserSession session = UserSession.builder()
            .id(UUID.randomUUID())
            .userId(user.getId())
            .refreshToken(refreshToken)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(7L * 24 * 3600)) // 7日
            .build();
        userSessionRepository.save(session);

        // セキュリティログ記録
        securityLogService.logAuthenticationAttempt(user.getId(), "LOGIN_SUCCESS", 
            ipAddress, userAgent, "Successful login");

        // ログインイベント発行
        eventPublishingService.publishUserEvent("USER_LOGIN", user.getId(), "User logged in");

        return AuthenticationResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(3600L)
            .user(convertToUserDto(user))
            .build();
    }

    /**
     * ログイン失敗処理
     */
    private void handleFailedLogin(User user, String ipAddress, String userAgent) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        
        // アカウントロック判定（5回失敗でロック）
        if (user.getFailedLoginAttempts() >= 5) {
            user.setLocked(true);
            user.setLockedAt(Instant.now());
            securityLogService.logAuthenticationAttempt(user.getId(), "ACCOUNT_LOCKED", 
                ipAddress, userAgent, "Account locked due to failed attempts");
        }
        
        userRepository.save(user);
        
        securityLogService.logAuthenticationAttempt(user.getId(), "LOGIN_FAILED", 
            ipAddress, userAgent, "Invalid password");
    }

    /**
     * UserをUserDtoに変換
     */
    private UserDto convertToUserDto(User user) {
        return UserDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .role(user.getRole().name())
            .mfaEnabled(user.isMfaEnabled())
            .createdAt(user.getCreatedAt())
            .lastLogin(user.getLastLogin())
            .build();
    }

    /**
     * Spring SecurityのUserDetailsServiceインターフェース実装
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPasswordHash())
            .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
            .accountExpired(false)
            .accountLocked(user.isLocked())
            .credentialsExpired(false)
            .disabled(!user.isActive())
            .build();
    }

    /**
     * ユーザーログイン（新しいAPIエンドポイント用）
     */
    public LoginResponse authenticateUser(LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        AuthenticationResponse authResponse = authenticate(request, ipAddress, userAgent);
        
        // AuthenticationResponse から LoginResponse に変換
        if (authResponse.isMfaRequired()) {
            return LoginResponse.mfaRequired(UUID.randomUUID().toString(), authResponse.getTempToken());
        }
        
        LoginResponse.TokenInfo tokenInfo = LoginResponse.TokenInfo.builder()
                .accessToken(authResponse.getAccessToken())
                .refreshToken(authResponse.getRefreshToken())
                .tokenType(authResponse.getTokenType())
                .expiresIn(authResponse.getExpiresIn())
                .expiresAt(Instant.now().plusSeconds(authResponse.getExpiresIn()))
                .build();
                
        LoginResponse.SessionInfo sessionInfo = LoginResponse.SessionInfo.builder()
                .sessionId(UUID.randomUUID()) // セッションIDを適切に取得
                .expiresAt(Instant.now().plusSeconds(7L * 24 * 3600))
                .deviceInfo(request.getDeviceInfo() != null ? request.getDeviceInfo().toString() : null)
                .build();
        
        return LoginResponse.success(authResponse.getUser(), tokenInfo, sessionInfo);
    }



    /**
     * ログアウト（新しいAPIエンドポイント用）
     */
    public LogoutResponse logout(String authHeader, HttpServletRequest request) {
        String token = extractTokenFromHeader(authHeader);
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        logout(token, ipAddress, userAgent);
        return LogoutResponse.success();
    }

    /**
     * OAuth フロー開始
     */
    public String initiateOAuthFlow(String provider, String redirectUri) {
        // プロバイダー別のOAuth URLを生成
        switch (provider.toLowerCase()) {
            case "azure":
            case "microsoft":
                return "/oauth2/authorization/azure";
            case "google":
                return "/oauth2/authorization/google";
            default:
                throw new IllegalArgumentException("Unsupported OAuth provider: " + provider);
        }
    }

    /**
     * OAuth コールバック処理
     */
    public LoginResponse handleOAuthCallback(String provider, OAuthCallbackRequest request, HttpServletRequest httpRequest) {
        // TODO: OAuth プロバイダー別の処理を実装
        // 現在はプレースホルダー実装
        throw new UnsupportedOperationException("OAuth callback processing not implemented yet");
    }

    /**
     * パスワードリセット要求
     */
    public void requestPasswordReset(String email) {
        // TODO: パスワードリセット機能の実装
        log.info("Password reset requested for email: {}", email);
        // メール送信などの処理を実装
    }

    /**
     * パスワードリセット実行
     */
    public void resetPassword(String token, String newPassword) {
        // TODO: パスワードリセット機能の実装
        log.info("Password reset execution");
        // トークン検証とパスワード更新処理を実装
    }

    /**
     * トークン検証
     */
    public java.util.Map<String, Object> validateToken(String authHeader) {
        String token = extractTokenFromHeader(authHeader);
        
        if (!jwtUtil.validateToken(token)) {
            throw new InvalidTokenException("Invalid token");
        }
        
        String userId = jwtUtil.getUserIdFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);
        
        return java.util.Map.of(
                "valid", true,
                "userId", userId,
                "role", role,
                "expiresAt", jwtUtil.getExpirationFromToken(token)
        );
    }

    /**
     * 現在のユーザー情報取得
     */
    public java.util.Map<String, Object> getCurrentUserInfo(String authHeader) {
        String token = extractTokenFromHeader(authHeader);
        String userId = jwtUtil.getUserIdFromToken(token);
        
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AuthenticationException("User not found"));
        
        UserDto userDto = convertToUserDto(user);
        
        return java.util.Map.of(
                "user", userDto,
                "permissions", Collections.singletonList("ROLE_" + user.getRole().name())
        );
    }

    /**
     * Authorizationヘッダーからトークンを抽出
     */
    private String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new InvalidTokenException("Invalid authorization header");
        }
        return authHeader.substring(7);
    }

    /**
     * クライアントIPアドレスを取得
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
