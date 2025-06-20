package com.skishop.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Authentication Response DTO
 * 
 * 認証成功時のレスポンス
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticationResponse {
    
    private boolean success;
    private String message;
    private UserDto user;
    private TokenInfo tokens;
    private SessionInfo session;
    private boolean mfaRequired;
    private String mfaSessionId;
    private boolean isNewUser;
    
    // 新しいフィールド
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
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
    }
    
    public static AuthenticationResponse success(UserDto user, TokenInfo tokens, SessionInfo session) {
        return AuthenticationResponse.builder()
                .success(true)
                .user(user)
                .tokens(tokens)
                .session(session)
                .mfaRequired(false)
                .build();
    }
    
    public static AuthenticationResponse mfaRequired(String mfaSessionId) {
        return AuthenticationResponse.builder()
                .success(false)
                .mfaRequired(true)
                .mfaSessionId(mfaSessionId)
                .message("Multi-factor authentication required")
                .build();
    }
    
    public static AuthenticationResponse error(String message) {
        return AuthenticationResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
