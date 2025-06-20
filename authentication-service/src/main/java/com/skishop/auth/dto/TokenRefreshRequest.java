package com.skishop.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Token Refresh Request DTO
 * 
 * トークン更新要求のデータ転送オブジェクト
 */
@Data
public class TokenRefreshRequest {
    
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
