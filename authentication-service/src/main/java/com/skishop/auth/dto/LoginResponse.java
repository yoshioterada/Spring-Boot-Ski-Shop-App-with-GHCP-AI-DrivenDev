package com.skishop.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Login Response DTO
 * 
 * ログイン成功時のレスポンス
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {
    
    private boolean success;
    private String message;
    private UserDto user;
    private TokenInfo tokens;
    private SessionInfo session;
    private boolean mfaRequired;
    private String mfaSessionId;
    private boolean isNewUser;
    private String tempToken;  // MFA待機時の一時トークン
    
    @Data
    @Builder
    public static class TokenInfo {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Long expiresIn;
        private Instant expiresAt;
    }
    
    @Data
    @Builder
    public static class SessionInfo {
        private UUID sessionId;
        private Instant expiresAt;
        private String deviceInfo;
    }
    
    /**
     * 成功レスポンスを作成
     */
    public static LoginResponse success(UserDto user, TokenInfo tokens, SessionInfo session) {
        return LoginResponse.builder()
                .success(true)
                .user(user)
                .tokens(tokens)
                .session(session)
                .mfaRequired(false)
                .build();
    }
    
    /**
     * MFA要求レスポンスを作成
     */
    public static LoginResponse mfaRequired(String mfaSessionId, String tempToken) {
        return LoginResponse.builder()
                .success(true)
                .mfaRequired(true)
                .mfaSessionId(mfaSessionId)
                .tempToken(tempToken)
                .message("MFA verification required")
                .build();
    }
    
    /**
     * エラーレスポンスを作成
     */
    public static LoginResponse error(String message) {
        return LoginResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
