package com.skishop.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * メール認証リクエストDTO
 */
public record VerifyEmailRequest(
    
    @NotBlank(message = "Verification token is required")
    String token
) {
}
