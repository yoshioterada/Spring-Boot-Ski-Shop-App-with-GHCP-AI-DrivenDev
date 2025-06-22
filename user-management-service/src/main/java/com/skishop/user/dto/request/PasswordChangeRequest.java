package com.skishop.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * パスワード変更リクエスト
 */
public record PasswordChangeRequest(
    @NotBlank(message = "Current password is required")
    String currentPassword,
    
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "New password must be between 8 and 100 characters")
    String newPassword,
    
    @NotBlank(message = "Password confirmation is required")
    String confirmPassword
) {
    public PasswordChangeRequest {
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }
    }
}
