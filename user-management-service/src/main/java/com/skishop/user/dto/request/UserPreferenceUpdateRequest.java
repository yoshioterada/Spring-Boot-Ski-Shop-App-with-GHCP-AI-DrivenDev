package com.skishop.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * ユーザー設定更新リクエストDTO
 */
public record UserPreferenceUpdateRequest(
    
    @NotBlank(message = "Key is required")
    String key,
    
    @NotBlank(message = "Value is required")
    String value
) {
}
