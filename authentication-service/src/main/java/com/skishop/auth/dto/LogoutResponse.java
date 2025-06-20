package com.skishop.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * Logout Response DTO
 * 
 * ログアウト成功時のレスポンス
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogoutResponse {
    
    private boolean success;
    private String message;
    private String logoutUrl;  // Azure Entra IDのログアウトURL
    
    /**
     * 成功レスポンスを作成
     */
    public static LogoutResponse success() {
        return LogoutResponse.builder()
                .success(true)
                .message("Successfully logged out")
                .build();
    }
    
    /**
     * Azure ADログアウト用成功レスポンスを作成
     */
    public static LogoutResponse success(String logoutUrl) {
        return LogoutResponse.builder()
                .success(true)
                .message("Successfully logged out")
                .logoutUrl(logoutUrl)
                .build();
    }
    
    /**
     * エラーレスポンスを作成
     */
    public static LogoutResponse error(String message) {
        return LogoutResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
