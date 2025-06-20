package com.skishop.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * OAuth Callback Request DTO
 * 
 * OAuth プロバイダーからのコールバック処理用データ転送オブジェクト
 */
@Data
public class OAuthCallbackRequest {
    
    @NotBlank(message = "Authorization code is required")
    private String code;
    
    private String state;  // CSRF防御用
    
    private String redirectUri;
    
    private String scope;
}
