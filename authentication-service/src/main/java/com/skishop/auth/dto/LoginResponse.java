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
    
    // Default constructor for manual builder
    public LoginResponse() {}
    
    // All-args constructor for Lombok compatibility
    public LoginResponse(boolean success, String message, UserDto user, 
                        TokenInfo tokens, SessionInfo session, boolean mfaRequired, 
                        String mfaSessionId, boolean isNewUser, String tempToken) {
        this.success = success;
        this.message = message;
        this.user = user;
        this.tokens = tokens;
        this.session = session;
        this.mfaRequired = mfaRequired;
        this.mfaSessionId = mfaSessionId;
        this.isNewUser = isNewUser;
        this.tempToken = tempToken;
    }
    
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
    
    // Manual builder method since Lombok may not be working properly
    public static LoginResponseBuilder builder() {
        return new LoginResponseBuilder();
    }
    
    // Inner Builder class
    public static class LoginResponseBuilder {
        private boolean success;
        private String message;
        private UserDto user;
        private TokenInfo tokens;
        private SessionInfo session;
        private boolean mfaRequired;
        private String mfaSessionId;
        private boolean isNewUser;
        private String tempToken;
        
        public LoginResponseBuilder success(boolean success) { this.success = success; return this; }
        public LoginResponseBuilder message(String message) { this.message = message; return this; }
        public LoginResponseBuilder user(UserDto user) { this.user = user; return this; }
        public LoginResponseBuilder tokens(TokenInfo tokens) { this.tokens = tokens; return this; }
        public LoginResponseBuilder session(SessionInfo session) { this.session = session; return this; }
        public LoginResponseBuilder mfaRequired(boolean mfaRequired) { this.mfaRequired = mfaRequired; return this; }
        public LoginResponseBuilder mfaSessionId(String mfaSessionId) { this.mfaSessionId = mfaSessionId; return this; }
        public LoginResponseBuilder isNewUser(boolean isNewUser) { this.isNewUser = isNewUser; return this; }
        public LoginResponseBuilder tempToken(String tempToken) { this.tempToken = tempToken; return this; }
        
        // For backwards compatibility - adding these methods that are used in the code
        public LoginResponseBuilder requiresMfa(boolean requiresMfa) { this.mfaRequired = requiresMfa; return this; }
        public LoginResponseBuilder token(String token) { this.tempToken = token; return this; }
        public LoginResponseBuilder sessionId(String sessionId) { this.mfaSessionId = sessionId; return this; }
        
        public LoginResponse build() {
            LoginResponse loginResponse = new LoginResponse();
            loginResponse.success = this.success;
            loginResponse.message = this.message;
            loginResponse.user = this.user;
            loginResponse.tokens = this.tokens;
            loginResponse.session = this.session;
            loginResponse.mfaRequired = this.mfaRequired;
            loginResponse.mfaSessionId = this.mfaSessionId;
            loginResponse.isNewUser = this.isNewUser;
            loginResponse.tempToken = this.tempToken;
            return loginResponse;
        }
    }
}
